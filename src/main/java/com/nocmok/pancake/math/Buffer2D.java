package com.nocmok.pancake.math;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.nocmok.pancake.OpenCvHelper;
import com.nocmok.pancake.Pancake;

import org.opencv.core.Mat;

public interface Buffer2D {

    /** Datatype of underlying buffer */
    public int datatype();

    public int xsize();

    public int ysize();

    public byte[] getBufferArray();

    public ByteBuffer getNioBuffer();

    Mat mat();

    public static Buffer2D wrap(ByteBuffer buf, int xsize, int ysize, int dtype) {
        return new MatBuffer2D(buf, xsize, ysize, dtype);
    }

    public static Buffer2D arrange(int xsize, int ysize, int dtype) {
        ByteBuffer buf = ByteBuffer.allocateDirect(xsize * ysize * Pancake.dtBytes(dtype));
        buf.order(ByteOrder.nativeOrder());
        return new MatBuffer2D(buf, xsize, ysize, dtype);
    }
}

class MatBuffer2D implements Buffer2D {

    private Mat mat;

    private ByteBuffer nioBuffer;

    MatBuffer2D(ByteBuffer buf, int xsize, int ysize, int dtype) {
        this.nioBuffer = buf;
        this.mat = new Mat(ysize, xsize, OpenCvHelper.toOpencvDatatype(dtype), nioBuffer);
    }

    @Override
    public int datatype() {
        return OpenCvHelper.toPancakeDatatype(mat.depth());
    }

    @Override
    public int xsize() {
        return mat.width();
    }

    @Override
    public int ysize() {
        return mat.height();
    }

    @Override
    public byte[] getBufferArray() {
        if (nioBuffer.isDirect()) {
            throw new UnsupportedOperationException(
                    "cannot get byte array from " + ByteBuffer.class + ", which is direct");
        }
        return nioBuffer.array();
    }

    @Override
    public ByteBuffer getNioBuffer() {
        return nioBuffer;
    }

    @Override
    public Mat mat() {
        return this.mat;
    }
}
