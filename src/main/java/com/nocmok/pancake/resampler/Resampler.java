package com.nocmok.pancake.resampler;

import java.io.File;

import com.nocmok.pancake.utils.PancakeOptions;

import org.gdal.gdal.Dataset;

/**
 * Object of this interface aimed to create upsampled dataset from source
 * dataset.
 */
public interface Resampler {

    public static final String OUT_FORMAT = "rsmp_out_format";

    /**
     * 
     * @param src       source dataset
     * @param outWidth  target width after source dataset will be upsampled
     * @param outHeight target height after source dataset will be upsampled
     * @param dest      file which has to back destination dataset
     * @return dataset that contains all bands from source dataset, but upsampled
     */
    public Dataset upsample(Dataset src, int outWidth, int outHeight, File dest, PancakeOptions options);
}
