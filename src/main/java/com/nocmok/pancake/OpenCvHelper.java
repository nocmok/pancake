package com.nocmok.pancake;

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

    /** TODO */
    public static int toPancakeDatatype(int opencvDt) {
        switch (opencvDt) {
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
