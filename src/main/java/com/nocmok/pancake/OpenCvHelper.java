package com.nocmok.pancake;

import java.util.Set;

import org.opencv.core.CvType;

public class OpenCvHelper {

    public static int toOpencvDatatype(int pancakeDt) {
        switch (pancakeDt) {
        case Pancake.TYPE_BYTE:
        case Pancake.TYPE_UNKNOWN:
            return CvType.CV_8U;
        case Pancake.TYPE_SBYTE:
            return CvType.CV_8S;
        case Pancake.TYPE_INT_16:
            return CvType.CV_16S;
        case Pancake.TYPE_UINT_16:
            return CvType.CV_16U;
        case Pancake.TYPE_INT_32:
            return CvType.CV_32S;
        case Pancake.TYPE_UINT_32:
            throw new UnsupportedOperationException("cannot convert uint32 as its not supported in opencv");
        case Pancake.TYPE_FLOAT_32:
            return CvType.CV_32F;
        case Pancake.TYPE_FLOAT_64:
            return CvType.CV_64F;
        default:
            throw new UnsupportedOperationException("unknown datatype: " + pancakeDt);
        }
    }

    /**
     * 
     * @param dtype opencv data type
     * @return
     */
    public static int baseType(int dtype) {
        if (Set.of(CvType.CV_8S, CvType.CV_8SC1, CvType.CV_8SC2, CvType.CV_8SC3, CvType.CV_8SC4).contains(dtype)) {
            return CvType.CV_8S;
        } else if (Set.of(CvType.CV_8U, CvType.CV_8UC1, CvType.CV_8UC2, CvType.CV_8UC3, CvType.CV_8UC4)
                .contains(dtype)) {
            return CvType.CV_8U;
        } else if (Set.of(CvType.CV_16S, CvType.CV_16SC1, CvType.CV_16SC2, CvType.CV_16SC3, CvType.CV_16SC4)
                .contains(dtype)) {
            return CvType.CV_16S;
        } else if (Set.of(CvType.CV_16U, CvType.CV_16UC1, CvType.CV_16UC2, CvType.CV_16UC3, CvType.CV_16UC4)
                .contains(dtype)) {
            return CvType.CV_16U;
        } else if (Set.of(CvType.CV_32S, CvType.CV_32SC1, CvType.CV_32SC2, CvType.CV_32SC3, CvType.CV_32SC4)
                .contains(dtype)) {
            return CvType.CV_32S;
        } else if (Set.of(CvType.CV_16F, CvType.CV_16FC1, CvType.CV_16FC2, CvType.CV_16FC3, CvType.CV_16FC4)
                .contains(dtype)) {
            return CvType.CV_16F;
        } else if (Set.of(CvType.CV_32F, CvType.CV_32FC1, CvType.CV_32FC2, CvType.CV_32FC3, CvType.CV_32FC4)
                .contains(dtype)) {
            return CvType.CV_32F;
        } else if (Set.of(CvType.CV_64F, CvType.CV_64FC1, CvType.CV_64FC2, CvType.CV_64FC3, CvType.CV_64FC4)
                .contains(dtype)) {
            return CvType.CV_64F;
        }
        throw new UnsupportedOperationException("unknown opencv datat type: " + dtype);
    }

    /** TODO */
    public static int toPancakeDatatype(int opencvDt) {
        switch (baseType(opencvDt)) {
        case CvType.CV_8S:
            return Pancake.TYPE_SBYTE;
        case CvType.CV_8U:
            return Pancake.TYPE_BYTE;
        case CvType.CV_16S:
            return Pancake.TYPE_INT_16;
        case CvType.CV_16U:
            return Pancake.TYPE_UINT_16;
        case CvType.CV_32S:
            return Pancake.TYPE_INT_32;
        case CvType.CV_32F:
            return Pancake.TYPE_FLOAT_32;
        case CvType.CV_64F:
            return Pancake.TYPE_FLOAT_64;
        default:
            throw new UnsupportedOperationException("cannot convert opencv datatype: " + opencvDt);
        }
    }
}
