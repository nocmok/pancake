package com.nocmok.pancake.math;

import java.util.Arrays;

public class BoxHP implements Filter2D {

    private int size;

    private double[][] kernel;

    public BoxHP(int size) {
        size = Integer.max(3, size);
        if (size % 2 == 0) {
            size += 1;
        }
        this.size = size;
        this.kernel = _getKernel();
    }

    public static BoxHP ofSize(int size){
        size = Integer.max(3, size);
        if(size % 2 == 0){
            size += 1;
        }
        return new BoxHP(size);   
    }

    private double[][] _getKernel() {
        double[][] kernel = new double[size][size];
        for (int y = 0; y < size; ++y) {
            Arrays.fill(kernel[y], -1f / (size * size));
        }
        kernel[size / 2][size / 2] += 1f;
        return kernel;
    }

    @Override
    public double[][] getKernel() {
        return this.kernel;
    }

}
