package com.nocmok.pancake.resampler;

import org.gdal.gdal.ProgressCallback;

public class Bilinear extends ResamplerBase {

    public Bilinear(ProgressCallback callback) {
        super(callback, "bilinear");
    }

    public Bilinear() {
        super(new ProgressCallback(), "bilinear");
    }
}
