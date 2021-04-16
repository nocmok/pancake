package com.nocmok.pancake.math;

import java.util.ArrayList;
import java.util.List;

import com.nocmok.pancake.Pancake;
import com.nocmok.pancake.utils.Rectangle;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.core.Core.MinMaxLocResult;
import org.opencv.imgproc.Imgproc;

/** Mostly opencv wrapper, containing 2d math routines */
public class Math2D {

    public Math2D() {

    }

    public static class Stat {

        private double min;

        private double max;

        private Stat() {

        }

        private void min(double min) {
            this.min = min;
        }

        private void max(double max) {
            this.max = max;
        }

        public double min() {
            return min;
        }

        public double max() {
            return max;
        }
    }

    private Mat matFromDoubleArray(double[][] array) {
        Mat mat = new Mat(new Size(array[0].length, array.length), CvType.CV_64F);
        for (int y = 0; y < mat.height(); ++y) {
            for (int x = 0; x < mat.width(); ++x) {
                mat.put(y, x, array[y][x]);
            }
        }
        return mat;
    }

    private Mat matFromBuffer2D(Buffer2D buf) {
        return buf.mat();
    }

    public void convolve(Buffer2D src, Filter2D filter, Buffer2D dst) {
        Mat srcMat = matFromBuffer2D(src);
        Mat dstMat = matFromBuffer2D(dst);
        Mat kernel = matFromDoubleArray(filter.getKernel());
        Core.flip(kernel, kernel, -1);
        Imgproc.filter2D(srcMat, dstMat, dstMat.depth(), kernel);
    }

    public void sum(Buffer2D a, Buffer2D b, Buffer2D sum) {
        Mat aMat = matFromBuffer2D(a);
        Mat bMat = matFromBuffer2D(b);
        Mat sumMat = matFromBuffer2D(sum);
        Core.add(aMat, bMat, sumMat, Mat.ones(sumMat.size(), CvType.CV_8U), sumMat.depth());
    }

    public void sum(Buffer2D a, Buffer2D b, Buffer2D sum, Rectangle region) {
        Mat aMat = matFromBuffer2D(a).submat(region.y0(), region.y1(), region.x0(), region.x1());
        Mat bMat = matFromBuffer2D(b).submat(region.y0(), region.y1(), region.x0(), region.x1());
        Mat sumMat = matFromBuffer2D(sum).submat(region.y0(), region.y1(), region.x0(), region.x1());
        Core.add(aMat, bMat, sumMat, Mat.ones(sumMat.size(), CvType.CV_8U), sumMat.depth());
    }

    /**
     * sum = a + b * scale
     * 
     * @param a
     * @param b
     * @param scale
     * @param sum
     */
    public void scaleSum(Buffer2D a, Buffer2D b, double scale, Buffer2D sum) {
        Mat aMat = matFromBuffer2D(a);
        Mat bMat = matFromBuffer2D(b);
        Mat sumMat = matFromBuffer2D(sum);
        Core.addWeighted(aMat, 1d, bMat, scale, 0d, sumMat, sumMat.depth());
    }

    public void scaleSum(Buffer2D a, double scale, Buffer2D b, Buffer2D sum) {
        Mat aMat = matFromBuffer2D(a);
        Mat bMat = matFromBuffer2D(b);
        Mat sumMat = matFromBuffer2D(sum);
        Core.scaleAdd(aMat, scale, bMat, sumMat);
    }

    public void convert(Buffer2D src, Buffer2D dst) {
        Mat srcMat = matFromBuffer2D(src);
        Mat dstMat = matFromBuffer2D(dst);
        srcMat.convertTo(dstMat, dstMat.depth());
    }

    public void convert(Buffer2D src, int dtype, Buffer2D dst) {
        Mat srcMat = matFromBuffer2D(src);
        Mat dstMat = matFromBuffer2D(dst);
        srcMat.convertTo(dstMat, OpenCvHelper.toOpencvDatatype(dtype));
    }

    public void convert(Buffer2D src, Buffer2D dst, double alpha, double beta) {
        Mat srcMat = matFromBuffer2D(src);
        Mat dstMat = matFromBuffer2D(dst);
        srcMat.convertTo(dstMat, dstMat.depth(), alpha, beta);
    }

    public void convertAndScale(Buffer2D src, Buffer2D dst) {
        convertAndScale(src, dst.datatype(), dst);
    }

    /**
     * Converts source buffer to destination buffer and scales values from source
     * buffer in order to fit in destination buffer datatype and preserve ratio
     * 
     * @param src
     * @param dtype
     * @param dst
     */
    public void convertAndScale(Buffer2D src, int dtype, Buffer2D dst) {
        if ((src == dst) && (dtype == dst.datatype())) {
            return;
        }
        Mat srcMat = matFromBuffer2D(src);
        Mat dstMat = matFromBuffer2D(dst);
        if (src.datatype() == dtype) {
            srcMat.convertTo(dstMat, dstMat.depth());
        } else if (Pancake.dtMax(dtype) > Pancake.dtMax(src.datatype())) {
            double scale = Pancake.dtMax(dtype) / Pancake.dtMax(src.datatype());
            Core.multiply(srcMat, new Scalar(scale), dstMat, 1d, dtype);
            // dstMat.convertTo(dstMat, OpenCvHelper.toOpencvDatatype(dtype));
        } else {
            double scale = Pancake.dtMax(src.datatype()) / Pancake.dtMax(src.datatype());
            Core.divide(srcMat, new Scalar(scale), dstMat, 1d, dtype);
            // dstMat.convertTo(dstMat, OpenCvHelper.toOpencvDatatype(dtype));
        }
    }

    /**
     * 
     * @param src
     * @param dtype
     * @param dst
     * @param oldMin
     * @param oldMax
     * @param newMin
     * @param newMax
     */
    public void convertAndScale(Buffer2D src, int dtype, Buffer2D dst, double oldMin, double oldMax, double newMin,
            double newMax) {
        double alpha = (newMax - newMin) / (oldMax - oldMin);
        double beta = newMin - oldMin * alpha;
        Mat srcMat = matFromBuffer2D(src);
        Mat dstMat = matFromBuffer2D(dst);
        srcMat.convertTo(dstMat, OpenCvHelper.toOpencvDatatype(dtype), alpha, beta);
    }

    public Stat stat(Buffer2D buf) {
        Stat stat = new Stat();
        Mat mat = matFromBuffer2D(buf);
        MinMaxLocResult minMax = Core.minMaxLoc(mat);
        stat.max(minMax.maxVal);
        stat.min(minMax.minVal);
        return stat;
    }

    public void sum(Buffer2D src, int scalar, Buffer2D dst) {
        Mat srcMat = matFromBuffer2D(src);
        Mat dstMat = matFromBuffer2D(dst);
        Core.add(srcMat, new Scalar(scalar), dstMat);
    }

    public void sub(Buffer2D src, int scalar, Buffer2D dst) {
        Mat srcMat = matFromBuffer2D(src);
        Mat dstMat = matFromBuffer2D(dst);
        Core.subtract(srcMat, new Scalar(scalar), dstMat);
    }

    public void sub(Buffer2D a, Buffer2D b, Buffer2D dest) {
        Mat aMat = matFromBuffer2D(a);
        Mat bMat = matFromBuffer2D(b);
        Mat dstMat = matFromBuffer2D(dest);
        Core.subtract(aMat, bMat, dstMat, Mat.ones(dstMat.size(), CvType.CV_8U), dstMat.depth());
    }

    public void mul(Buffer2D src, double scalar, Buffer2D dst) {
        Mat srcMat = matFromBuffer2D(src);
        Mat dstMat = matFromBuffer2D(dst);
        Core.multiply(srcMat, new Scalar(scalar), dstMat);
    }

    public void mul(Buffer2D a, Buffer2D b, Buffer2D dst) {
        Mat aMat = matFromBuffer2D(a);
        Mat bMat = matFromBuffer2D(b);
        Mat dstMat = matFromBuffer2D(dst);
        Core.multiply(aMat, bMat, dstMat, 1f, dstMat.depth());
    }

    public void mul(Buffer2D a, Buffer2D b, Buffer2D dst, Rectangle region) {
        Mat aMat = matFromBuffer2D(a).submat(region.y0(), region.y1(), region.x0(), region.x1());
        Mat bMat = matFromBuffer2D(b).submat(region.y0(), region.y1(), region.x0(), region.x1());
        Mat dstMat = matFromBuffer2D(dst).submat(region.y0(), region.y1(), region.x0(), region.x1());
        Core.multiply(aMat, bMat, dstMat, 1f, dstMat.depth());
    }

    /**
     * 
     * @param a
     * @param dtype  dst dtype
     * @param b
     * @param dst
     * @param oldMin
     * @param oldMax
     * @param newMin
     * @param newMax
     */
    public void mul(Buffer2D a, Buffer2D b, Buffer2D dst, double scale, int dtype) {
        Mat aMat = matFromBuffer2D(a);
        Mat bMat = matFromBuffer2D(b);
        Mat dstMat = matFromBuffer2D(dst);
        Core.multiply(aMat, bMat, dstMat, scale, dtype);
    }

    public void div(Buffer2D a, Buffer2D b, Buffer2D dst) {
        Mat aMat = matFromBuffer2D(a);
        Mat bMat = matFromBuffer2D(b);
        Mat dstMat = matFromBuffer2D(dst);
        Core.divide(aMat, bMat, dstMat, 1f, dstMat.depth());
    }

    public void div(Buffer2D a, Buffer2D b, Buffer2D dst, Rectangle region) {
        Mat aMat = matFromBuffer2D(a).submat(region.y0(), region.y1(), region.x0(), region.x1());
        Mat bMat = matFromBuffer2D(b).submat(region.y0(), region.y1(), region.x0(), region.x1());
        Mat dstMat = matFromBuffer2D(dst).submat(region.y0(), region.y1(), region.x0(), region.x1());
        Core.divide(aMat, bMat, dstMat, 1f, dstMat.depth());
    }

    public void fill(Buffer2D buf, double scalar) {
        buf.mat().setTo(new Scalar(scalar));
    }

    /** */
    public void normalize(Buffer2D buf, double min, double max) {
        Mat mat = matFromBuffer2D(buf);
        Core.normalize(mat, mat, min, max, Core.NORM_MINMAX);
    }

    /** */
    public void normalize(Buffer2D src, Buffer2D dst, double min, double max) {
        Mat srcMat = matFromBuffer2D(src);
        Mat dstMat = matFromBuffer2D(dst);
        Core.normalize(srcMat, dstMat, min, max, Core.NORM_MINMAX);
    }

    public void vconcat(List<Buffer2D> src, Buffer2D dst) {
        List<Mat> mats = new ArrayList<>();
        for (Buffer2D buf : src) {
            mats.add(matFromBuffer2D(buf));
        }
        Core.vconcat(mats, matFromBuffer2D(dst));
    }

    public Buffer2D subBuffer(Buffer2D buf, int x0, int y0, int xsize, int ysize) {
        Mat mat = matFromBuffer2D(buf);
        Mat submat = mat.submat(y0, y0 + ysize, x0, x0 + xsize);
        return Buffer2D.wrapMat(submat);
    }

    public Buffer2D subBuffer(Buffer2D buf, Rectangle roi) {
        return subBuffer(buf, roi.x0(), roi.y0(), roi.xSize(), roi.ySize());
    }

    public Buffer2D compareEquals(Buffer2D a, Buffer2D b) {
        Mat aMat = matFromBuffer2D(a);
        Mat bMat = matFromBuffer2D(b);
        Mat mask = new Mat();
        Core.compare(aMat, bMat, mask, Core.CMP_EQ);
        return Buffer2D.wrapMat(mask);
    }

    public Buffer2D compareEquals(Buffer2D a, double scalar) {
        Mat aMat = matFromBuffer2D(a);
        Mat mask = new Mat();
        Core.compare(aMat, new Scalar(scalar), mask, Core.CMP_EQ);
        return Buffer2D.wrapMat(mask);
    }

    public void fill(Buffer2D buf, double scalar, Buffer2D mask) {
        Mat mat = matFromBuffer2D(buf);
        mat.setTo(new Scalar(scalar), matFromBuffer2D(mask));
    }

    public void replace(Buffer2D buf, double oldVal, double newVal) {
        Mat mat = matFromBuffer2D(buf);
        Mat mask = new Mat();
        Core.compare(mat, new Scalar(oldVal), mask, Core.CMP_EQ);
        mat.setTo(new Scalar(newVal), mask);
    }
}
