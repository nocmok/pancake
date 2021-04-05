package com.nocmok.pancake;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Vector;

import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.Driver;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;

class GdalHelper {

    public static String pathTo(Dataset dataset) {
        Vector<?> fileList = dataset.GetFileList();
        return fileList != null ? fileList.firstElement().toString() : null;
    }

    public static List<Band> split(Dataset dataset) {
        List<Band> bands = new ArrayList<>();
        for (int nBand = 1; nBand <= dataset.getRasterCount(); ++nBand) {
            bands.add(dataset.GetRasterBand(nBand));
        }
        return bands;
    }

    public static void copyBand(Band dst, Band src) {
        if (dst.GetXSize() != src.getXSize() || dst.getYSize() != src.getYSize()) {
            throw new UnsupportedOperationException("source band resolution mismatch destination band resolution");
        }
        if (dst.GetRasterDataType() == src.GetRasterDataType()) {
            copyBandBlockwise(dst, src);
        } else {
            copyBandWithTypeConversion(dst, src);
            throw new UnsupportedOperationException("copying bands with different sample data type not implemented");
        }
    }

    private static void copyBandBlockwise(Band dst, Band src) {
        int xBlocks = (src.getXSize() + src.GetBlockXSize() - 1) / src.GetBlockXSize();
        int yBlocks = (src.getYSize() + src.GetBlockYSize() - 1) / src.GetBlockYSize();

        int blockByteSize = src.GetBlockXSize() * src.GetBlockYSize() * gdal.GetDataTypeSize(src.GetRasterDataType())
                / 8;
        ByteBuffer buf = ByteBuffer.allocateDirect(blockByteSize);

        for (int y = 0; y < yBlocks; ++y) {
            for (int x = 0; x < xBlocks; ++x) {
                src.ReadBlock_Direct(x, y, buf);
                dst.WriteBlock_Direct(x, y, buf);
            }
        }
    }

    private static void copyBandWithTypeConversion(Band dst, Band src) {

    }

    public static Dataset bundleRgbBands(Band r, Band g, Band b, Driver driver, File dstFile, int bandsDataType,
            List<String> options) {
        List<Band> bands = List.of(r, g, b);
        int xSize = r.getXSize();
        int ySize = r.getYSize();
        for (Band band : bands) {
            if (band.getXSize() != xSize || band.getYSize() != ySize) {
                throw new UnsupportedOperationException("bands should have equal resolution");
            }
        }
        List<String> creationOptions = new ArrayList<>();
        creationOptions.add("PHOTOMETRIC=rgb");
        if (isTiled(r)) {
            creationOptions.add("TILED=YES");
        }
        creationOptions.add("BLOCKXSIZE=" + r.GetBlockXSize());
        creationOptions.add("BLOCKYSIZE=" + r.GetBlockYSize());
        // Vector<String> creationOptions = new Vector<>(List.of("PHOTOMETRIC=rgb"));
        if (options != null) {
            creationOptions.addAll(options);
        }
        Dataset outDataset = driver.Create(dstFile.getAbsolutePath(), xSize, ySize, 3, bandsDataType,
                new Vector<String>(creationOptions));

        copyBand(outDataset.GetRasterBand(1), r);
        copyBand(outDataset.GetRasterBand(2), g);
        copyBand(outDataset.GetRasterBand(3), b);

        return outDataset;
    }

    public static Dataset bundleRgbBands(Band r, Band g, Band b, Driver driver, File dstFile, int bandsDataType) {
        return bundleRgbBands(r, g, b, driver, dstFile, bandsDataType, null);
    }

    public static boolean isTiled(Band band) {
        return (band.GetBlockXSize() != band.getXSize());
    }

    public static Dataset bundleBandsToVRT(Collection<Band> bands, File dstFile, List<String> options) {
        Band first = bands.iterator().next();
        for (Band band : bands) {
            if (first.GetRasterDataType() != band.GetRasterDataType()) {
                throw new RuntimeException("all bands expected to have same datatype");
            }
        }
        return bundleBandsToVRT(bands, dstFile, first.GetRasterDataType(), options);
    }

    public static Dataset bundleBandsToVRT(Collection<Band> bands, File dstFile, int dataType, List<String> options) {
        int xSize = bands.iterator().next().getXSize();
        int ySize = bands.iterator().next().getYSize();
        String[] optionsArr = (options == null) ? (null) : (options.toArray(new String[0]));
        Driver driver = gdal.GetDriverByName("VRT");
        driver.Register();
        Dataset dataset = driver.Create(dstFile.getAbsolutePath(), xSize, ySize, 0, dataType, optionsArr);

        String sourceXmlPattern = "<SimpleSource>" + "<SourceFilename relativeToVRT=\"0\">%s</SourceFilename>"
                + "<SourceBand>%d</SourceBand>"
                + "<SourceProperties RasterXSize=\"%d\" RasterYSize=\"%d\" DataType=\"%s\" BlockXSize=\"%d\" BlockYSize=\"%d\"/>"
                + "</SimpleSource>";

        for (Band band : bands) {
            dataset.AddBand(dataType);
            Band vrtBand = dataset.GetRasterBand(dataset.getRasterCount());
            String sourceSpecXml = String.format(sourceXmlPattern, pathTo(band.GetDataset()), band.GetBand(),
                    band.getXSize(), band.getYSize(), gdal.GetDataTypeName(band.GetRasterDataType()),
                    band.GetBlockXSize(), band.GetBlockYSize());
            vrtBand.SetMetadataItem("source_" + (vrtBand.GetBand() - 1), sourceSpecXml, "new_vrt_sources");
        }

        return dataset;
    }

    public static Dataset copyDataset(File dstFile, Dataset srcDataset) {
        Driver driver = srcDataset.GetDriver();
        return driver.CreateCopy(dstFile.getAbsolutePath(), srcDataset, (String[]) null);
    }

    /**
     * 
     * @param ds
     * @return 0 - min, 1 - max
     */
    public static double[] computeMinMax(Dataset ds) {
        double[] minMax = new double[2];
        for (int i = 1; i <= ds.getRasterCount(); ++i) {
            double[] currMinMax = new double[2];
            ds.GetRasterBand(i).ComputeRasterMinMax(currMinMax);
            minMax[0] = Double.min(minMax[0], currMinMax[0]);
            minMax[1] = Double.max(minMax[1], currMinMax[1]);
        }
        return minMax;
    }

    public static int toGdalAccessMode(int pancakeAccess) {
        switch (pancakeAccess) {
        case Pancake.ACCESS_READONLY:
            return gdalconst.GA_ReadOnly;
        case Pancake.ACCESS_READWRITE:
            return gdalconst.GA_Update;
        default:
            throw new UnsupportedOperationException("unknown access mode " + pancakeAccess);
        }
    }

    public static Dataset convert(PancakeDataset ds) {
        if (ds instanceof GdalDatasetMirror) {
            return ((GdalDatasetMirror) ds).getUnderlyingDataset();
        }
        throw new UnsupportedOperationException("cannot convert dataset of class " + ds.getClass());
    }

    public static Band convert(PancakeBand band) {
        if (band instanceof GdalBandMirror) {
            return ((GdalBandMirror) band).getUnderlyingBand();
        } else {
            throw new UnsupportedOperationException("cannot convert band of class " + band.getClass());
        }
    }

    public static List<Band> convert(List<PancakeBand> bands) {
        List<Band> gdalBands = new ArrayList<>();
        for (PancakeBand band : bands) {
            gdalBands.add(convert(band));
        }
        return gdalBands;
    }
}
