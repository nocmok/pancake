package com.nocmok.pancake;

import org.gdal.gdal.ProgressCallback;

public class Bilinear extends ResamplerBase {

    public Bilinear(ProgressCallback callback) {
        super(callback, "bilinear");
    }

    public Bilinear() {
        super(new ProgressCallback(), "bilinear");
    }
}
