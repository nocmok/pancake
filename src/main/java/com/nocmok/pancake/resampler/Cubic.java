package com.nocmok.pancake.resampler;

import org.gdal.gdal.ProgressCallback;

public class Cubic extends ResamplerBase {

    public Cubic(ProgressCallback callback) {
        super(callback, "cubic");
    }
    
    public Cubic(){
        super(new ProgressCallback(), "cubic");
    }
}
