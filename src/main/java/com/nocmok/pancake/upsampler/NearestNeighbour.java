package com.nocmok.pancake.upsampler;

import java.io.File;
import java.util.List;
import java.util.Vector;

import org.gdal.gdal.Dataset;
import org.gdal.gdal.ProgressCallback;
import org.gdal.gdal.TranslateOptions;
import org.gdal.gdal.gdal;

public class NearestNeighbour implements Upsampler {

    ProgressCallback callback;

    public NearestNeighbour() {
        this(new ProgressCallback());
    }

    public NearestNeighbour(ProgressCallback callback) {
        this.callback = callback;
    }

    @Override
    public Dataset upsample(Dataset src, int outWidth, int outHeight, File dest, List<String> options) {
        Vector<String> toVec = new Vector<>();
        toVec.add("-r");
        toVec.add("near");
        toVec.add("-outsize");
        toVec.add(Integer.toString(outWidth));
        toVec.add(Integer.toString(outHeight));
        toVec.addAll(options);
        TranslateOptions to = new TranslateOptions(toVec);
        return gdal.Translate(dest.getAbsolutePath(), src, to, callback);
    }
}
