package com.nocmok.pancake.math;

import com.nocmok.pancake.OpenCvHelper;

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

    public void mul(Buffer2D src, double scalar, Buffer2D dst) {
        Mat srcMat = matFromBuffer2D(src);
        Mat dstMat = matFromBuffer2D(dst);
        Core.multiply(srcMat, new Scalar(scalar), dstMat);
    }
}
