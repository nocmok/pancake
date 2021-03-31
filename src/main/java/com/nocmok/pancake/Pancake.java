package com.nocmok.pancake;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import com.nocmok.pancake.utils.PancakeIOException;

import org.gdal.gdal.gdal;
import org.opencv.core.Core;

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

    public static final int TYPE_SBYTE = 100;

    private Pancake() {

    }

    public static void load() {
        gdal.AllRegister();
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
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
            return 0;
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

    public static String getDatatypeName(int dtype) {
        return gdal.GetDataTypeName(dtype);
    }
}
