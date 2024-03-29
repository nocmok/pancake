package com.nocmok.pancake.math;

public class FFT {

    /**
     * 1d fft
     * 
     * @param in
     */
    private static void _fft1(ComplexBuffer in, ComplexBuffer out) {
        if (in.size() > out.size()) {
            throw new IndexOutOfBoundsException("out buffer size < in buffer size");
        }
        if (!PancakeMath.isPow2(in.size())) {
            throw new UnsupportedOperationException("buffer size must be power of 2");
        }

        int n = in.size();
        int logLen = 0;
        while ((n >>> logLen) > 1) {
            ++logLen;
        }

        /** bit-reversal permutation */
        for (int i = 0; i < n; ++i) {
            int revI = PancakeMath.reverseNLeast(i, logLen);
            if (i < revI + 1) {
                Complex a = in.get(i);
                Complex b = in.get(revI);
                out.set(i, b);
                out.set(revI, a);
            }
        }

        if (n < 2) {
            return;
        }

        for (int len = 2; len <= n; len <<= 1) {
            Complex wlen = Complex.fromPolar(2 * Math.PI / len, 1);

            for (int i = 0; i < n; i += len) {
                Complex w = Complex._1;

                for (int j = i; j < i + len / 2; ++j) {
                    Complex a = out.get(j);
                    Complex b = out.get(j + len / 2).mul(w);

                    out.set(j, a.sum(b));
                    out.set(j + len / 2, a.sub(b));

                    w = w.mul(wlen);
                }
            }
        }
    }

    /**
     * 1d inverse fft
     * 
     * @param in
     * @param out
     */
    private static void _ifft1(ComplexBuffer in, ComplexBuffer out) {
        if (in.size() > out.size()) {
            throw new IndexOutOfBoundsException("out buffer size < in buffer size");
        }
        if (!PancakeMath.isPow2(in.size())) {
            throw new UnsupportedOperationException("buffer size must be power of 2");
        }

        int n = in.size();
        int logLen = 0;
        while ((n >>> logLen) > 1) {
            ++logLen;
        }

        /** bit-reversal permutation */
        for (int i = 0; i < n; ++i) {
            int revI = PancakeMath.reverseNLeast(i, logLen);
            if (i < revI + 1) {
                Complex a = in.get(i);
                Complex b = in.get(revI);
                out.set(i, b);
                out.set(revI, a);
            }
        }

        if (n < 2) {
            return;
        }

        for (int len = 2; len <= n; len <<= 1) {
            Complex wlen = Complex.fromPolar(-2 * Math.PI / len, 1);

            for (int i = 0; i < n; i += len) {
                Complex w = Complex._1;

                for (int j = i; j < i + len / 2; ++j) {
                    Complex a = out.get(j);
                    Complex b = out.get(j + len / 2).mul(w);

                    out.set(j, a.sum(b));
                    out.set(j + len / 2, a.sub(b));

                    w = w.mul(wlen);
                }
            }
        }

        for (int i = 0; i < n; ++i) {
            out.set(i, out.get(i).div(n));
        }
    }

    public static void fft1(ComplexBuffer in, ComplexBuffer out) {
        if (out.size() < in.size()) {
            throw new IndexOutOfBoundsException("out buffer size < in buffer size");
        }
        if (!PancakeMath.isPow2(in.size())) {
            int newLen = 1;
            while (newLen < in.size()) {
                newLen <<= 1;
            }
            in = PaddedComplexBuffer.pad(in, newLen - in.size());
            if (out.size() < in.size()) {
                out = PaddedComplexBuffer.pad(out, newLen);
            }
        }
        _fft1(in, out);
    }

    public static void ifft1(ComplexBuffer in, ComplexBuffer out) {
        if (out.size() < in.size()) {
            throw new IndexOutOfBoundsException("out buffer size < in buffer size");
        }
        if (!PancakeMath.isPow2(in.size())) {
            int newLen = 1;
            while (newLen < in.size()) {
                newLen <<= 1;
            }
            in = PaddedComplexBuffer.pad(in, newLen - in.size());
            if (out.size() < in.size()) {
                out = PaddedComplexBuffer.pad(out, newLen);
            }
        }
        _ifft1(in, out);
    }

    /**
     * 2d fft
     * 
     * @param in
     * @param xSize
     * @param ySize
     */
    public static void fft2(ComplexBuffer in, ComplexBuffer out, int xSize, int ySize) {

    }
}
