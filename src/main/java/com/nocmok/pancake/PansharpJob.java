package com.nocmok.pancake;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.nocmok.pancake.fusor.Fusor;
import com.nocmok.pancake.fusor.NormalizedBand;
import com.nocmok.pancake.fusor.PancakeBand;
import com.nocmok.pancake.upsampler.Upsampler;
import com.nocmok.pancake.utils.PancakeIOException;
import com.nocmok.pancake.utils.Rectangle;
import com.nocmok.pancake.utils.PancakeOptions;

import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.Driver;
import org.gdal.gdal.gdal;

/**
 * Objects of this class collect source data and pansharpening options in order
 * to produce pansharpened artefact.
 */
public class PansharpJob {

    Upsampler _resampler;

    Fusor _fusor;

    Map<Spectrum, Band> _mapping;

    /**  */
    List<Dataset> _datasets;

    /** may contains duplicates */
    List<Band> _bands;

    List<Band> _multispectral;

    /**
     * Order in which multispectral bands will be packed in dataset in order to be
     * resampled.
     */
    List<Spectrum> _multispecBandsPackingOrder;

    File _targetFile;

    ExecutorService _worker;

    int _targetXSize;

    int _targetYSize;

    int _targetDataType;

    Compression _targetCompression;

    Formats _targetFormat;

    Driver _targetDriver;

    PancakeOptions _options;

    /** creation options for target artifact */
    PancakeOptions _targetOptions;

    public static final String JOB_TARGET_FORMAT = "job_out_format";

    public static final String JOB_COMPRESSION = "job_compress";

    public static final String JOB_TARGET_PATH = "job_out_path";

    public static final String JOB_TARGET_DATATYPE = "job_out_dt";

    public static final String JOB_BLOCKXSIZE = "job_blockxsize";

    public static final String JOB_BLOCKYSIZE = "job_blockysize";

    public static final String JOB_NUM_THREADS = "job_nthreads";

    public static final String JOB_COMPRESSION_QUALITY = "job_compress_qual";

    public static final String JOB_TILED = "job_tiled";

    PansharpJob(Upsampler resampler, Fusor fusor, Map<Spectrum, Band> mapping, PancakeOptions options) {
        this._resampler = resampler;
        this._fusor = fusor;
        this._mapping = mapping;

        this._targetCompression = Compression
                .valueOf(options.getStringOr(JOB_COMPRESSION, Compression.NONE.gdalIdentifier()));
        this._targetFormat = Formats.byName(options.getString(JOB_TARGET_FORMAT));
        if (_targetFormat == null) {
            throw new UnsupportedOperationException("unknown format driver " + options.getString("driver"));
        }
        this._targetDriver = _targetFormat.getDriver();
        this._targetDriver.Register();
        this._targetFile = new File(options.getStringOr(JOB_TARGET_PATH, "."));
        this._targetDataType = options.getIntOr(JOB_TARGET_DATATYPE, Pancake.TYPE_INT_16);

        _targetXSize = _mapping.get(Spectrum.PA).getXSize();
        _targetYSize = _mapping.get(Spectrum.PA).getYSize();
        _worker = Executors.newFixedThreadPool(numThreads());

        _bands = new ArrayList<>(_mapping.values());
        _datasets = new ArrayList<>();
        Set<String> datasetSet = new HashSet<>();
        for (var band : _bands) {
            if (!datasetSet.contains(Pancake.pathTo(band.GetDataset()))) {
                _datasets.add(band.GetDataset());
                datasetSet.add(Pancake.pathTo(band.GetDataset()));
            }
        }
        _multispecBandsPackingOrder = new ArrayList<>();
        _multispecBandsPackingOrder.add(Spectrum.R);
        _multispecBandsPackingOrder.add(Spectrum.G);
        _multispecBandsPackingOrder.add(Spectrum.B);
        _multispecBandsPackingOrder.add(Spectrum.NI);
        for (Spectrum spec : _multispecBandsPackingOrder) {
            Band band = _mapping.get(spec);
            if (band != null) {
                _multispectral.add(band);
            }
        }
        _targetOptions = populateTargetOptions();
    }

    private PancakeOptions populateTargetOptions() {
        PancakeOptions options = new PancakeOptions();
        options.put(PancakeConstants.KEY_INTERLEAVE, "band");
        options.put(PancakeConstants.KEY_TILED, (isTiled() ? "yes" : "no"));
        options.put(PancakeConstants.KEY_BLOCKXSIZE, blockXSize());
        options.put(PancakeConstants.KEY_BLOCKYSIZE, blockYSize());
        options.put(PancakeConstants.KEY_PHOTOMETRIC, "rgb");
        options.put(PancakeConstants.KEY_BIGTIFF, "if_safer");
        options.put(PancakeConstants.KEY_COMPRESSION, _targetCompression.gdalIdentifier());
        options.put(PancakeConstants.KEY_COMPRESSION_NUM_THREADS, numThreads());
        options.put(PancakeConstants.KEY_COMPRESSION_QUALITY, compressionQuality());
        return options;
    }

    public Dataset pansharp() {
        validateSpatialReferences();

        Dataset multispectral;
        Dataset artifact;

        /** reuse interpolated dataset if possible */
        if (canReuseResampledDataset()) {
            multispectral = resample(populateTargetOptions(), _targetFile);
            artifact = multispectral;
        } else {
            multispectral = resample(null, Pancake.createTempFile());
            artifact = createDstDataset();
        }

        Map<Spectrum, Band> srcMapping = new HashMap<>();
        var keyIt = _multispecBandsPackingOrder.iterator();
        var valIt = Pancake.getBands(multispectral).iterator();
        while (valIt.hasNext() && keyIt.hasNext()) {
            srcMapping.put(keyIt.next(), valIt.next());
        }

        Map<Spectrum, Band> dstMapping = new HashMap<>();
        dstMapping.put(Spectrum.R, artifact.GetRasterBand(1));
        dstMapping.put(Spectrum.G, artifact.GetRasterBand(2));
        dstMapping.put(Spectrum.B, artifact.GetRasterBand(3));

        if (numThreads() > 1) {
            throw new UnsupportedOperationException("parallel fusion not implemented");
        } else {
            fuse(dstMapping, srcMapping);
        }

        return artifact;
    }

    /** TODO */
    private void validateSpatialReferences() {

    }

    /** TODO */
    private boolean canReuseResampledDataset() {
        // if upsampler returns non vrt dataset
        // if fusor on each iteration doesn't use values of any pixel except the current
        // if multispectral bands contains only r,g,b
        return false;
    }

    private Dataset resample(PancakeOptions options, File dst) {
        options = Optional.ofNullable(options).orElse(new PancakeOptions());
        Dataset vrt = Pancake.bundleBandsToVRT(_multispectral, Pancake.createTempFile(), _targetDataType,
                options.getAsGdalOptions());
        Dataset scaled = _resampler.upsample(vrt, _targetXSize, _targetYSize, dst, options);
        return scaled;
    }

    private Dataset createDstDataset() {
        PancakeOptions driverOptions = _targetFormat.toDriverOptions(_targetOptions);
        Dataset dstDataset = _targetDriver.Create(_targetFile.getAbsolutePath(), _targetXSize, _targetYSize, 3,
                _targetDataType, new Vector<>(driverOptions.getAsGdalOptions()));
        if (dstDataset == null) {
            throw new PancakeIOException("failed to create destination dataset: " + gdal.GetLastErrorMsg());
        }
        return dstDataset;
    }

    /**
     * TODO
     * 
     * @return [0] - x size of block, [1] - y size of block
     */
    private int[] computeOptimalCacheBlockSize() {
        return new int[] { blockXSize(), blockYSize() };
    }

    private Map<Spectrum, PancakeBand> remap(Map<Spectrum, Band> mapping) {
        int[] cacheBlockSize = computeOptimalCacheBlockSize();
        Map<Spectrum, PancakeBand> pancakeMapping = new HashMap<>();
        for (var entry : mapping.entrySet()) {
            PancakeBand pancakeBand = new NormalizedBand(entry.getValue(), cacheBlockSize[0], cacheBlockSize[1]);
            pancakeMapping.put(entry.getKey(), pancakeBand);
        }
        return pancakeMapping;
    }

    private void fuse(Map<Spectrum, Band> dstMapping, Map<Spectrum, Band> srcMapping) {
        Map<Spectrum, PancakeBand> pdstMapping = remap(dstMapping);
        Map<Spectrum, PancakeBand> psrcMapping = remap(srcMapping);
        _fusor.fuse(pdstMapping, psrcMapping);
    }

    private List<Rectangle> sliceRegions() {
        int blocksPerRow = (_targetXSize + blockXSize() - 1) / blockXSize();
        int blocksPerCol = (_targetYSize + blockYSize() - 1) / blockYSize();
        int totalBlocks = blocksPerRow * blocksPerCol;
        int avgBlocksPerCPU = totalBlocks / numThreads();
        List<Rectangle> regions = new ArrayList<>();
        if (avgBlocksPerCPU >= blocksPerRow) {
            int areaBlockYSize = blocksPerCol / numThreads();
            int linesCovered = 0;
            for (int i = 0; i < numThreads(); ++i) {
                regions.add(new Rectangle(0, linesCovered, _targetXSize,
                        Integer.min(_targetYSize - linesCovered, areaBlockYSize * blockYSize())));
                linesCovered += areaBlockYSize * blockYSize();
            }
        } else {
            throw new UnsupportedOperationException("not implemented");
        }
        return regions;
    }

    private void fuseParallel(Map<Spectrum, Band> dstMapping, Map<Spectrum, Band> srcMapping) {
        throw new UnsupportedOperationException("parrallel fusion not implemented");
    }

    public int blockXSize() {
        return _options.getIntOr(JOB_BLOCKXSIZE, _mapping.get(Spectrum.PA).GetBlockXSize());
    }

    public int blockYSize() {
        return _options.getIntOr(JOB_BLOCKYSIZE, _mapping.get(Spectrum.PA).GetBlockYSize());
    }

    public int numThreads() {
        return _options.getIntOr(JOB_NUM_THREADS, 1);
    }

    /**
     * @return value in range [0 .. 100], where 0 - lack of compression (fastest),
     *         100 - best compression (slowest)
     */
    public int compressionQuality() {
        return _options.getIntOr(JOB_COMPRESSION_QUALITY, 0);
    }

    public boolean isTiled() {
        return _options.getBoolOr(JOB_TILED, true);
    }
}