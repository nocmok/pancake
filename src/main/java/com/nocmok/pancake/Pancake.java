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

    /**
     * 
     * @param datatype
     * @return is data type integer 
     */
    public static boolean isInt(int datatype) {
        switch (datatype) {
        case TYPE_SBYTE:
        case TYPE_BYTE:
        case TYPE_UNKNOWN:
        case TYPE_INT_16:
        case TYPE_UINT_16:
        case TYPE_INT_32:
        case TYPE_UINT_32:
            return true;
        case TYPE_FLOAT_32:
        case TYPE_FLOAT_64:
            return false;
        default:
            throw new UnsupportedOperationException("unsupported data type " + datatype);
        }
    }

    /**
     * 
     * @param dtype
     * @return is datatype unsigned integer
     */
    public static boolean isUint(int dtype) {
        switch (dtype) {
        case TYPE_BYTE:
        case TYPE_UNKNOWN:
        case TYPE_UINT_16:
        case TYPE_UINT_32:
            return true;
        case TYPE_SBYTE:
        case TYPE_INT_32:
        case TYPE_INT_16:
        case TYPE_FLOAT_32:
        case TYPE_FLOAT_64:
            return false;
        default:
            throw new UnsupportedOperationException("unsupported data type " + dtype);
        }
    }

    /**
     * 
     * @param dtype
     * @return How much bytes in data type
     */
    public static int dtBytes(int dtype) {
        switch (dtype) {
        case TYPE_BYTE:
        case TYPE_SBYTE:
        case TYPE_UNKNOWN:
            return 1;
        case TYPE_UINT_16:
        case TYPE_INT_16:
            return 2;
        case TYPE_UINT_32:
        case TYPE_INT_32:
        case TYPE_FLOAT_32:
            return 4;
        case TYPE_FLOAT_64:
            return 8;
        default:
            throw new UnsupportedOperationException("unsupported data type " + dtype);
        }
    }

    /**
     * 
     * @param dtype
     * @return data type maximum value
     */
    public static double dtMax(int dtype) {
        switch (dtype) {
        case TYPE_SBYTE:
            return Byte.MAX_VALUE;
        case TYPE_BYTE:
        case TYPE_UNKNOWN:
            return 0xff;
        case TYPE_INT_16:
            return 0x7fff;
        case TYPE_UINT_16:
            return 0xffff;
        case TYPE_INT_32:
            return 0x7fffffff;
        case TYPE_UINT_32:
            return 0xffffffffL;
        case TYPE_FLOAT_32:
            return Float.MAX_VALUE;
        case TYPE_FLOAT_64:
            return Double.MAX_VALUE;
        default:
            throw new UnsupportedOperationException("unsupported data type " + dtype);
        }
    }

    /**
     * 
     * @param dtype
     * @return data type minimum value
     */
    public static double dtMin(int dtype) {
        switch (dtype) {
        case TYPE_SBYTE:
            return Byte.MIN_VALUE;
        case TYPE_BYTE:
        case TYPE_UNKNOWN:
            return 0;
        case TYPE_INT_16:
            return Short.MIN_VALUE;
        case TYPE_UINT_16:
            return 0;
        case TYPE_INT_32:
            return Integer.MIN_VALUE;
        case TYPE_UINT_32:
            return 0;
        case TYPE_FLOAT_32:
            return -Float.MAX_VALUE;
        case TYPE_FLOAT_64:
            return -Double.MAX_VALUE;
        default:
            throw new UnsupportedOperationException("unsupported data type " + dtype);
        }
    }

    public static double normalize(double value, int valDtype) {
        return (value - dtMin(valDtype)) / (dtMax(valDtype) - dtMin(valDtype));
    }

    public static double denormalize(double value, int valDtype) {
        return value * (dtMax(valDtype) - dtMin(valDtype)) + dtMin(valDtype);
    }

    public static double convert(double value, int valDtype, int destDtype) {
        if (valDtype == destDtype) {
            return value;
        }
        return denormalize(normalize(value, valDtype), destDtype);
    }

    /**
     * 
     * @return smallest signed datatype able to fit specified datatype
     */
    public static int signedDatatypeFit(int dtype) {
        switch (dtype) {
        case TYPE_SBYTE:
            return TYPE_SBYTE;
        case TYPE_INT_16:
        case TYPE_BYTE:
        case TYPE_UNKNOWN:
            return TYPE_INT_16;
        case TYPE_INT_32:
        case TYPE_UINT_16:
            return TYPE_INT_32;
        case TYPE_UINT_32:
        case TYPE_FLOAT_32:
            return TYPE_FLOAT_32;
        case TYPE_FLOAT_64:
            return TYPE_FLOAT_64;
        default:
            throw new UnsupportedOperationException("unsupported data type: " + dtype);
        }
    }

    public static int largerDt(Collection<Integer> dtypes) {
        int largerDt = dtypes.iterator().next();
        for (int dt : dtypes) {
            if (dtBytes(dt) > dtBytes(largerDt)) {
                largerDt = dt;
            } else if (dtBytes(dt) == dtBytes(largerDt)) {
                if (isUint(largerDt)) {
                    largerDt = dt;
                }
            }
        }
        return largerDt;
    }

    public static String dtName(int dtype) {
        return gdal.GetDataTypeName(dtype);
    }
}
