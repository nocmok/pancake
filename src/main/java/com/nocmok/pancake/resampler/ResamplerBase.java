package com.nocmok.pancake.resampler;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Vector;

import com.nocmok.pancake.Formats;
import com.nocmok.pancake.GdalHelper;
import com.nocmok.pancake.Pancake;
import com.nocmok.pancake.PancakeConstants;
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

        List<String> to = new ArrayList<>();
        to.add("-of");
        to.add(outFormat);
        to.add("-r");
        to.add(resamplingMethod);
        to.add("-outsize");
        to.add(Integer.toString(outWidth));
        to.add(Integer.toString(outHeight));
        to.add("-ot");

        int dtype = options.getIntOr(PancakeConstants.KEY_DATATYPE, Pancake.TYPE_BYTE);

        to.add(Pancake.dtName(dtype));

        double[] minMax = GdalHelper.computeMinMax(src);

        to.add("-scale");
        to.add(Integer.toString((int) minMax[0]));
        to.add(Integer.toString((int) minMax[1]));
        to.add(Integer.toString((int) Pancake.dtMin(dtype)));
        to.add(Integer.toString((int) Pancake.dtMax(dtype)));

        List<String> co = Formats.byName(outFormat).toDriverOptions(options).getAsGdalOptions();
        to.addAll(getAsArgList(co));

        return gdal.Translate(dest.getAbsolutePath(), src, new TranslateOptions(new Vector<>(to)), callback);
    }
}
