package com.nocmok.pancake.math;

/** Gaussian high pass filter */
public class GaussianHP implements Filter2D {
    
    private double sigma;

    public GaussianHP(double sigma){
        this.sigma = sigma;      
    }

    @Override
    public double[][] getKernel() {
        int n = (int) Math.ceil(6 * sigma);
        if (n % 2 == 0) {
            n += 1;
        }
        n = Integer.max(3, n);
        double[][] kernel = new double[n][n];

        double sigma2 = Math.pow(sigma, 2);
        int x0 = n / 2;
        int y0 = n / 2;

        for (int y = 0; y < n; ++y) {
            for (int x = 0; x < n; ++x) {
                int dist = (x - x0) * (x - x0) + (y - y0) * (y - y0);
                double scale = 1.0 / (2 * Math.PI * sigma2) * Math.pow(Math.E, -dist / (2.0 * sigma2));
                kernel[y][x] = -scale;
            }
        }
        kernel[y0][x0] += 1;
        
        return kernel;
    }
    
}
