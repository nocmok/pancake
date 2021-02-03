package com.nocmok.pancake.upsampler;

import java.io.File;
import java.util.List;

import org.gdal.gdal.Dataset;

/**
 * Object of this interface aimed to create upsampled dataset from source
 * dataset.
 */
public interface Upsampler {

    /**
     * 
     * @param src       source dataset
     * @param outWidth  target width after source dataset will be upsampled
     * @param outHeight target height after source dataset will be upsampled
     * @param dest      file which has to back destination dataset
     * @return dataset that contains all bands from source dataset, but upsampled
     */
    public Dataset upsample(Dataset src, int outWidth, int outHeight, File dest, List<String> options);
}
