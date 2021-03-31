package com.nocmok.pancake.math;

/** two dimensional linear filter */
public interface Filter2D {
    
    /**
     * 
     * @return 2d array representing filter kernel
     */
    public double[][] getKernel();
}
