package com.nocmok.pancake;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.nocmok.pancake.fusor.Fusor;
import com.nocmok.pancake.utils.HistogramMatching;
import com.nocmok.pancake.utils.Pair;
import com.nocmok.pancake.utils.PancakeIOException;

import org.gdal.gdal.Driver;
import org.gdal.gdal.gdal;

/**
 * Objects of this class collect source data and pansharpening options in order
 * to produce pansharpened artefact.
 */
public class PansharpJob {

    Resampler _resampler;

    Fusor _fusor;

    Map<Spectrum, PancakeBand> _mapping;

    /** may contains duplicates */
    List<PancakeBand> _bands;

    List<PancakeBand> _multispectral;

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

    boolean useHistMatching;

    boolean memoryPolite;

    PancakeOptions _options;

    /** creation options for target artifact */
    PancakeOptions _targetOptions;

    PancakeOptions _resamplingOptions;

    public static final String JOB_TARGET_FORMAT = "job_out_format";

    public static final String JOB_COMPRESSION = "job_compress";

    public static final String JOB_TARGET_PATH = "job_out_path";

    public static final String JOB_TARGET_DATATYPE = "job_out_dt";

    public static final String JOB_BLOCKXSIZE = "job_blockxsize";

    public static final String JOB_BLOCKYSIZE = "job_blockysize";

    public static final String JOB_NUM_THREADS = "job_nthreads";

    public static final String JOB_COMPRESSION_QUALITY = "job_compress_qual";

    public static final String JOB_TILED = "job_tiled";

    public static final String JOB_USE_HIST_MATCHING = "job_use_hist_matching";

    public static final String JOB_MEMORY_POLITE = "job_memory_polite";

    PansharpJob(Resampler resampler, Fusor fusor, Map<Spectrum, PancakeBand> mapping, PancakeOptions options) {
        this._options = new PancakeOptions(options);
        this._resampler = resampler;
        this._fusor = fusor;
        this._mapping = mapping;

        this._targetCompression = Compression
                .byName(options.getStringOr(JOB_COMPRESSION, Compression.NONE.gdalIdentifier()));
        this._targetFormat = Formats.byName(options.getString(JOB_TARGET_FORMAT));
        if (_targetFormat == null) {
            throw new UnsupportedOperationException("unknown format driver " + options.getString(JOB_TARGET_FORMAT));
        }
        this._targetDriver = _targetFormat.getDriver();
        this._targetDriver.Register();
        this._targetFile = new File(options.getStringOr(JOB_TARGET_PATH, "."));
        this._targetDataType = options.getIntOr(JOB_TARGET_DATATYPE, Pancake.TYPE_BYTE);
        this.useHistMatching = options.getBoolOr(JOB_USE_HIST_MATCHING, true);
        this.memoryPolite = options.getBoolOr(JOB_MEMORY_POLITE, false);

        _targetXSize = _mapping.get(Spectrum.PA).getXSize();
        _targetYSize = _mapping.get(Spectrum.PA).getYSize();
        _worker = Executors.newFixedThreadPool(numThreads());

        _bands = new ArrayList<>(_mapping.values());
        _multispectral = new ArrayList<>();
        _multispecBandsPackingOrder = new ArrayList<>();
        _multispecBandsPackingOrder.add(Spectrum.R);
        _multispecBandsPackingOrder.add(Spectrum.G);
        _multispecBandsPackingOrder.add(Spectrum.B);
        _multispecBandsPackingOrder.add(Spectrum.NI);
        for (Spectrum spec : _multispecBandsPackingOrder) {
            PancakeBand band = _mapping.get(spec);
            if (band != null) {
                _multispectral.add(band);
            }
        }
        validateMultispectralBands(_multispectral);
        _targetOptions = populateTargetOptions();
        _resamplingOptions = populateResamplingOptions();
    }

    private void validateMultispectralBands(List<PancakeBand> ms) {
        PancakeBand first = ms.get(0);
        for (PancakeBand band : ms) {
            if (band.getXSize() != first.getXSize() || band.getYSize() != first.getYSize()) {
                throw new RuntimeException("multispectral bands has different resolutions");
            }
        }
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
        options.put(PancakeConstants.KEY_DATATYPE, _targetDataType);
        return options;
    }

    private PancakeOptions populateResamplingOptions() {
        PancakeOptions options = new PancakeOptions();
        options.update(Optional.of(_targetOptions).orElse(populateTargetOptions()));
        if (!(_resampler instanceof OnTheFlyResampler)) {
            options.put(Resampler.OUT_FORMAT, _targetFormat.driverName());
        }
        return options;
    }

    public PancakeDataset pansharp() {
        validateSpatialReferences();

        int msXSize = _multispectral.get(0).getXSize();
        int msYSize = _multispectral.get(0).getYSize();

        Map<Spectrum, PancakeBand> srcMapping = new EnumMap<>(Spectrum.class);
        srcMapping.put(Spectrum.PA, _mapping.get(Spectrum.PA));

        if (msXSize != _targetXSize || msYSize != _targetYSize) {
            PancakeDataset multispectral = resample(_resamplingOptions, Pancake.createTempFile());
            Iterator<PancakeBand> bandsIt = multispectral.bands().iterator();
            for (Spectrum spect : _multispecBandsPackingOrder) {
                if (_mapping.containsKey(spect)) {
                    srcMapping.put(spect, bandsIt.next());
                }
            }
        } else {
            srcMapping.putAll(_mapping);
        }

        PancakeDataset artifact = createTargetDataset(_targetOptions, _targetFile);

        Map<Spectrum, PancakeBand> dstMapping = new EnumMap<>(Spectrum.class);
        dstMapping.put(Spectrum.R, artifact.bands().get(0));
        dstMapping.put(Spectrum.G, artifact.bands().get(1));
        dstMapping.put(Spectrum.B, artifact.bands().get(2));

        if (numThreads() > 1) {
            throw new UnsupportedOperationException("parallel fusion not implemented");
        } else {
            fuse(dstMapping, srcMapping);
        }

        if (useHistMatching) {
            List<Pair<PancakeBand, PancakeBand>> histMapping = new ArrayList<>();
            for (Spectrum spec : _multispecBandsPackingOrder) {
                PancakeBand fused = dstMapping.get(spec);
                PancakeBand source = _mapping.get(spec);
                if (fused != null && source != null) {
                    histMapping.add(Pair.of(fused, source));
                }
            }
            matchHistograms(histMapping);
        }

        postProcessArtifact(artifact);
        return artifact;
    }

    private void matchHistograms(List<Pair<PancakeBand, PancakeBand>> mapping) {
        HistogramMatching hm = new HistogramMatching();
        for (Pair<PancakeBand, PancakeBand> pair : mapping) {
            hm.matchHistogram(pair.first(), pair.second());
        }
    }

    /** TODO */
    private void validateSpatialReferences() {

    }

    /** TODO */
    private void postProcessArtifact(PancakeDataset artifact) {

    }

    private PancakeDataset resample(PancakeOptions options, File dst) {
        PancakeOptions resamplingOptions = options;
        try (PancakeDataset vrt = Pancake.bundle(_multispectral, Pancake.createTempFile(), null)) {
            PancakeDataset scaled = _resampler.resample(vrt, _targetXSize, _targetYSize, dst, resamplingOptions);
            if (scaled == null) {
                throw new RuntimeException(
                        "failed to create resampled dataset due to error: " + gdal.GetLastErrorMsg());
            }
            return scaled;
        } catch(IOException e){
            throw new RuntimeException("failed to free dataset due to i/o error: ", e);
        }
    }

    private PancakeDataset createTargetDataset(PancakeOptions options, File dst) {
        PancakeOptions driverOptions = _targetFormat.toDriverOptions(options);
        PancakeDataset targetDataset = Pancake.create(_targetFormat, _targetFile, _targetXSize, _targetYSize, 3,
                _targetDataType, driverOptions);
        if (targetDataset == null) {
            throw new PancakeIOException("failed to create destination dataset: " + gdal.GetLastErrorMsg());
        }
        return targetDataset;
    }

    private void fuse(Map<Spectrum, PancakeBand> dstMapping, Map<Spectrum, PancakeBand> srcMapping) {
        _fusor.fuse(dstMapping, srcMapping);
    }

    public int blockXSize() {
        return _options.getIntOr(JOB_BLOCKXSIZE, _mapping.get(Spectrum.PA).getBlockXSize());
    }

    public int blockYSize() {
        return _options.getIntOr(JOB_BLOCKYSIZE, _mapping.get(Spectrum.PA).getBlockYSize());
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
