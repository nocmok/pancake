package com.nocmok.pancake;

import org.gdal.gdal.ProgressCallback;

public class Nearest extends ResamplerBase {

    public Nearest(ProgressCallback callback) {
        super(callback, "near");
    }

    public Nearest() {
        super(new ProgressCallback(), "near");
    }
}
