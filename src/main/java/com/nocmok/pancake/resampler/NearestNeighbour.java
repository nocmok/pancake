package com.nocmok.pancake.resampler;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import com.nocmok.pancake.utils.PancakeOptions;

import org.gdal.gdal.Dataset;
import org.gdal.gdal.ProgressCallback;
import org.gdal.gdal.TranslateOptions;
import org.gdal.gdal.gdal;

public class NearestNeighbour implements Resampler {

    private ProgressCallback callback;

    public NearestNeighbour() {
        this(new ProgressCallback());
    }

    public NearestNeighbour(ProgressCallback callback) {
        this.callback = callback;
    }

    private void addAsCreationOptions(List<String> options, List<String> creationOptions) {
        for (String option : creationOptions) {
            options.add("-co");
            options.add(option);
        }
    }

    @Override
    public Dataset upsample(Dataset src, int outWidth, int outHeight, File dest, PancakeOptions options) {
        options = (options != null) ? options : new PancakeOptions();
        String outFormat = options.getString(Resampler.OUT_FORMAT);
        if (outFormat == null) {
            outFormat = src.GetDriver().getShortName();
        }
        List<String> toList = new ArrayList<>();
        toList.add("-of");
        toList.add(outFormat);
        toList.add("-r");
        toList.add("near");
        toList.add("-outsize");
        toList.add(Integer.toString(outWidth));
        toList.add(Integer.toString(outHeight));
        addAsCreationOptions(toList, options.getAsGdalOptions());
        TranslateOptions to = new TranslateOptions(new Vector<>(toList));
        return gdal.Translate(dest.getAbsolutePath(), src, to, callback);
    }
}
