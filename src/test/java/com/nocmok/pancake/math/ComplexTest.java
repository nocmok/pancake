package com.nocmok.pancake.math;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ComplexTest {

    @Test
    public void testPolarCreation() {
        double arg = Math.PI;
        double abs = 3;

        Complex a = Complex.fromPolar(arg, abs);

        assertEquals(arg, a.arg());
        assertEquals(abs, a.abs());
    }

    @Test
    public void testSum() {
        Complex a = Complex.fromReIm(2, 3);

        assertEquals(a, a.sum(Complex._0));

        assertEquals(Complex.fromReIm(4, 6), a.sum(a));

        Complex b = Complex.fromReIm(4, 5);

        assertEquals(b.sum(a), a.sum(b));
    }

    @Test
    public void testMul() {
        Complex a = Complex.fromReIm(3, 4);

        assertEquals(a, a.mul(Complex._1));

        assertEquals(Complex._0, a.mul(Complex._0));

        assertEquals(Complex.fromReIm(-1, 0), Complex._i.mul(Complex._i));

        Complex b = Complex.fromReIm(5, 2);

        assertEquals(Complex.fromReIm(7, 26), a.mul(b));

        assertEquals(a.mul(b), b.mul(a));
    }
}