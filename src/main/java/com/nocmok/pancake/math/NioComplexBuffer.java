package com.nocmok.pancake.math;

import java.nio.DoubleBuffer;

public class NioComplexBuffer implements ComplexBuffer {

    private DoubleBuffer _buff;

    public NioComplexBuffer(int size) {
        this(DoubleBuffer.allocate(2 * size));
    }

    /**
     * 
     * @param buffer
     * @param start  start position of specified buffer
     * @param size   the number of complex numbers, this buffer should store
     */
    public NioComplexBuffer(DoubleBuffer buffer) {
        _buff = buffer;
    }

    @Override
    public double re(int i) {
        return _buff.get(i << 1);
    }

    @Override
    public void re(int i, double value) {
        _buff.put(i << 1, value);

    }

    @Override
    public double im(int i) {
        return _buff.get((i << 1) + 1);
    }

    @Override
    public void im(int i, double value) {
        _buff.put((i << 1) + 1, value);
    }

    @Override
    public Complex get(int i) {
        return Complex.fromReIm(re(i), im(i));
    }

    @Override
    public void set(int i, double re, double im) {
        re(i, re);
        im(i, im);
    }

    @Override
    public void set(int i, Complex complex) {
        re(i, complex.re());
        im(i, complex.im());
    }

    @Override
    public int size() {
        return _buff.capacity() >> 1;
    }

}