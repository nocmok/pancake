package com.nocmok.pancake;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Vector;

import com.nocmok.pancake.utils.PancakeIOException;

import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.Driver;
import org.gdal.gdal.gdal;

public class Pancake {

    /** wrapper for gdalconst.GDT_Unknown */
    public static final int TYPE_UNKNOWN = 0;

    /** wrapper for gdalconst.GDT_Byte */
    public static final int TYPE_BYTE = 1;

    /** wrapper for gdalconst.GDT_Int16 */
    public static final int TYPE_INT_16 = 3;

    /** wrapper for gdalconst.GDT_UInt16 */
    public static final int TYPE_UINT_16 = 2;

    /** wrapper for gdalconst.GDT_Int32 */
    public static final int TYPE_INT_32 = 5;

    /** wrapper for gdalconst.GDT_UInt32 */
    public static final int TYPE_UINT_32 = 4;

    /** wrapper for gdalconst.GDT_Float32 */
    public static final int TYPE_FLOAT_32 = 6;

    /** wrapper for gdalconst.GDT_Float64 */
    public static final int TYPE_FLOAT_64 = 7;

    private Pancake() {

    }

    public static File createTempFile() {
        try {
            File file = File.createTempFile(".pnk", "tmp");
            file.deleteOnExit();
            return file;
        } catch (IOException e) {
            throw new PancakeIOException("failed to create temporary file due to i/o error", e);
        }
    }

    public static String pathTo(Dataset dataset) {
        Vector<?> fileList = dataset.GetFileList();
        return fileList != null ? fileList.firstElement().toString() : null;
    }

    public static List<Band> getBands(Dataset dataset) {
        List<Band> bands = new ArrayList<>();
        for (int nBand = 1; nBand <= dataset.getRasterCount(); ++nBand) {
            bands.add(dataset.GetRasterBand(nBand));
        }
        return bands;
    }

    public static boolean isIntegerDatatype(int datatype) {
        switch (datatype) {
            case Pancake.TYPE_BYTE:
            case Pancake.TYPE_UNKNOWN:
            case Pancake.TYPE_INT_16:
            case Pancake.TYPE_UINT_16:
            case Pancake.TYPE_INT_32:
            case Pancake.TYPE_UINT_32:
                return true;
            case Pancake.TYPE_FLOAT_32:
            case Pancake.TYPE_FLOAT_64:
                return false;
            default:
                throw new UnsupportedOperationException("unsupported data type " + datatype);
        }
    }

    public static boolean isUnsignedInt(int datatype) {
        switch (datatype) {
            case Pancake.TYPE_BYTE:
            case Pancake.TYPE_UNKNOWN:
            case Pancake.TYPE_UINT_16:
            case Pancake.TYPE_UINT_32:
                return true;
            case Pancake.TYPE_INT_32:
            case Pancake.TYPE_INT_16:
            case Pancake.TYPE_FLOAT_32:
            case Pancake.TYPE_FLOAT_64:
                return false;
            default:
                throw new UnsupportedOperationException("unsupported data type " + datatype);
        }
    }

    public static int getDatatypeSizeBytes(int datatype) {
        return gdal.GetDataTypeSize(datatype) / 8;
    }

    public static double getDatatypeMax(int datatype) {
        switch (datatype) {
            case Pancake.TYPE_BYTE:
            case Pancake.TYPE_UNKNOWN:
                return 0xff;
            case Pancake.TYPE_INT_16:
                return 0x7fff;
            case Pancake.TYPE_UINT_16:
                return 0xffff;
            case Pancake.TYPE_INT_32:
                return 0x7fffffff;
            case Pancake.TYPE_UINT_32:
                return 0xffffffffL;
            case Pancake.TYPE_FLOAT_32:
                return Float.MAX_VALUE;
            case Pancake.TYPE_FLOAT_64:
                return Double.MAX_VALUE;
            default:
                throw new UnsupportedOperationException("unknown data type " + datatype);
        }
    }

    public static double getDatatypeMin(int datatype) {
        switch (datatype) {
            case Pancake.TYPE_BYTE:
            case Pancake.TYPE_UNKNOWN:
                return Byte.MIN_VALUE;
            case Pancake.TYPE_INT_16:
                return Short.MIN_VALUE;
            case Pancake.TYPE_UINT_16:
                return 0;
            case Pancake.TYPE_INT_32:
                return Integer.MIN_VALUE;
            case Pancake.TYPE_UINT_32:
                return 0;
            case Pancake.TYPE_FLOAT_32:
                return -Float.MAX_VALUE;
            case Pancake.TYPE_FLOAT_64:
                return -Double.MAX_VALUE;
            default:
                throw new UnsupportedOperationException("unknown data type " + datatype);
        }
    }

    public static int getBiggestDatatype(Collection<Integer> datatypes) {
        int biggestDt = datatypes.iterator().next();
        for (int dt : datatypes) {
            if (getDatatypeSizeBytes(dt) > getDatatypeSizeBytes(biggestDt)) {
                biggestDt = dt;
            } else if (getDatatypeSizeBytes(dt) == getDatatypeSizeBytes(biggestDt)) {
                if (isUnsignedInt(biggestDt)) {
                    biggestDt = dt;
                }
            }
        }
        return biggestDt;
    }

    public static String getDatatypeName(int dtype){
        return gdal.GetDataTypeName(dtype);
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
            String sourceSpecXml = String.format(sourceXmlPattern, ((String) band.GetDataset().GetFileList().get(0)),
                    band.GetBand(), band.getXSize(), band.getYSize(), gdal.GetDataTypeName(band.GetRasterDataType()),
                    band.GetBlockXSize(), band.GetBlockYSize());
            vrtBand.SetMetadataItem("source_" + (vrtBand.GetBand() - 1), sourceSpecXml, "new_vrt_sources");
        }
        return dataset;
    }

    public static Dataset copyDataset(File dstFile, Dataset srcDataset) {
        Driver driver = srcDataset.GetDriver();
        return driver.CreateCopy(dstFile.getAbsolutePath(), srcDataset, (String[]) null);
    }
}
