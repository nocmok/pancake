package com.nocmok.pancake.math;

public class PaddedComplexBuffer implements ComplexBuffer {

    private ComplexBuffer _buf;

    private int _padding;

    private Complex _placeholder;

    public static PaddedComplexBuffer pad(ComplexBuffer buffer, int padding, Complex placeholder) {
        return new PaddedComplexBuffer(buffer, padding, placeholder);
    }

    public static PaddedComplexBuffer pad(ComplexBuffer buffer, int padding) {
        return new PaddedComplexBuffer(buffer, padding);
    }

    public PaddedComplexBuffer(ComplexBuffer buffer, int padding) {
        this(buffer, padding, Complex._0);
    }

    public PaddedComplexBuffer(ComplexBuffer buffer, int padding, Complex placeholder) {
        if (buffer instanceof PaddedComplexBuffer) {
            _buf = ((PaddedComplexBuffer) buffer).getUnderlyingBuffer();
        } else {
            _buf = buffer;
        }
        _padding = padding;
        _placeholder = placeholder;
    }

    @Override
    public double re(int i) {
        return (i < _buf.size()) ? _buf.re(i) : _placeholder.re();
    }

    @Override
    public void re(int i, double value) {
        if (i < _buf.size()) {
            _buf.re(i, value);
        }
    }

    @Override
    public double im(int i) {
        return (i < _buf.size()) ? _buf.im(i) : _placeholder.im();
    }

    @Override
    public void im(int i, double value) {
        if (i < _buf.size()) {
            _buf.im(i, value);
        }

    }

    @Override
    public Complex get(int i) {
        return (i < _buf.size()) ? _buf.get(i) : _placeholder;
    }

    @Override
    public void set(int i, double re, double im) {
        if (i < _buf.size()) {
            _buf.set(i, re, im);
        }
    }

    @Override
    public void set(int i, Complex complex) {
        if (i < _buf.size()) {
            _buf.set(i, complex);
        }
    }

    @Override
    public int size() {
        return _buf.size() + _padding;
    }

    public ComplexBuffer getUnderlyingBuffer() {
        return _buf;
    }
}
