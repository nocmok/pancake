package com.nocmok.pancake.resampler;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Vector;

import com.nocmok.pancake.Formats;
import com.nocmok.pancake.utils.PancakeOptions;

import org.gdal.gdal.Dataset;
import org.gdal.gdal.ProgressCallback;
import org.gdal.gdal.TranslateOptions;
import org.gdal.gdal.gdal;

public abstract class ResamplerBase implements Resampler {

    private ProgressCallback callback;

    private final String resamplingMethod;

    public ResamplerBase(String resamplingMethod) {
        this(new ProgressCallback(), resamplingMethod);
    }

    public ResamplerBase(ProgressCallback callback, String resamplingMethod) {
        this.callback = callback;
        this.resamplingMethod = resamplingMethod;
    }

    private List<String> getAsArgList(List<String> creationOptions) {
        List<String> options = new ArrayList<>();
        for (String option : creationOptions) {
            options.add("-co");
            options.add(option);
        }
        return options;
    }

    @Override
    public Dataset resample(Dataset src, int outWidth, int outHeight, File dest, PancakeOptions options) {
        options = Optional.ofNullable(options).orElse(new PancakeOptions());

        String outFormat = Optional.ofNullable(options.getString(Resampler.OUT_FORMAT))
                .orElse(src.GetDriver().getShortName());

        List<String> toList = new ArrayList<>();
        toList.add("-of");
        toList.add(outFormat);
        toList.add("-r");
        toList.add(resamplingMethod);
        toList.add("-outsize");
        toList.add(Integer.toString(outWidth));
        toList.add(Integer.toString(outHeight));

        List<String> creationOptions = Formats.byName(outFormat).toDriverOptions(options).getAsGdalOptions();
        toList.addAll(getAsArgList(creationOptions));

        return gdal.Translate(dest.getAbsolutePath(), src, new TranslateOptions(new Vector<>(toList)), callback);
    }
}
