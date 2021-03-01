package com.nocmok.pancake.math;

public class Complex {

    private final double re;

    private final double im;

    public static final Complex _0 = new Complex(0.0, 0.0);

    public static final Complex _1 = new Complex(1.0, 0.0);

    public static final Complex _i = new Complex(0.0, 1.0);

    private Complex(double re, double im) {
        this.re = re;
        this.im = im;
    }

    public static Complex fromPolar(double arg, double abs) {
        double re = abs * Math.cos(arg);
        double im = abs * Math.sin(arg);
        return new Complex(re, im);
    }

    public static Complex fromReIm(double re, double im) {
        return new Complex(re, im);
    }

    public double re() {
        return re;
    }

    public double im() {
        return im;
    }

    public Complex sum(Complex b) {
        return new Complex(re + b.re, im + b.im);
    }

    public Complex sub(Complex b) {
        return new Complex(re - b.re, im - b.im);
    }

    public Complex mul(Complex b) {
        return new Complex(re * b.re - im * b.im, im * b.re + re * b.im);
    }

    /**
     * 
     * @param b real number
     * @return
     */
    public Complex mul(double b) {
        return new Complex(re * b, im * b);
    }

    public boolean isZero() {
        return re == 0 && im == 0;
    }

    public Complex conj() {
        return new Complex(re, -im);
    }

    public Complex inv() {
        if (isZero()) {
            throw new ArithmeticException("divide by zero");
        }
        return new Complex(re / (re * re + im * im), -im / (re * re + im * im));
    }

    public Complex div(Complex b) {
        if (b.isZero()) {
            throw new ArithmeticException("divide by zero");
        }
        // return mul(b.inv());
        double dn = b.abs2();
        return new Complex((re * b.re + im * b.im) / dn, (im * b.re - re * b.im) / dn);
    }

    /**
     * 
     * @param b real number
     * @return
     */
    public Complex div(double b) {
        if (b == 0.0) {
            throw new ArithmeticException("divide by zero");
        }
        return new Complex(re / b, im / b);
    }

    public double arg() {
        if (isZero()) {
            return Double.NaN;
        }
        return Math.atan2(im, re);
    }

    public double argOrElse(double value) {
        if (isZero()) {
            return value;
        }
        return Math.atan2(im, re);
    }

    public double abs2() {
        return re * re + im * im;
    }

    public double abs() {
        return Math.sqrt(abs2());
    }

    public double tan() {
        return im / re;
    }

    public double ctan() {
        return re / im;
    }

    public double cos() {
        return re / abs();
    }

    public Complex pow(double n) {
        if (n == 0) {
            return Complex._1;
        }
        double abs = Math.pow(abs(), n);
        double arg = arg();
        return new Complex(abs * Math.cos(n * arg), abs * Math.sin(n * arg));
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Complex)) {
            return false;
        }
        Complex b = (Complex) other;
        return (Math.abs(re - b.re()) < 1e-10) && (Math.abs(im - b.im()) < 1e-10);
    }

    public String toPlaneString() {
        return String.format("%.3f + %.3fi", re, im);
    }

    public String toEulerString() {
        return String.format("%.3f * e^(i * %.3f)", abs(), arg());
    }

    @Override
    public String toString() {
        return toPlaneString();
    }
}
