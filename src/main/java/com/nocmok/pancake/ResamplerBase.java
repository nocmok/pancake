package com.nocmok.pancake;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Vector;

import org.gdal.gdal.Dataset;
import org.gdal.gdal.ProgressCallback;
import org.gdal.gdal.TranslateOptions;
import org.gdal.gdal.gdal;

public abstract class ResamplerBase implements Resampler {

    private ProgressCallback callback;

    private PancakeProgressListener listener = PancakeProgressListener.empty;

    private final String resamplingMethod;

    public ResamplerBase(String resamplingMethod) {
        this.resamplingMethod = resamplingMethod;
        this.callback = new ProgressCallback(){
            @Override
            public int run(double dfComplete, String pszMessage) {
                getListener().listen(PancakeConstants.PROGRESS_RESAMPLING, dfComplete, "[Gdal] performing resampling");
                return 1;
            }
        };
    }

    private List<String> getAsArgList(List<String> creationOptions) {
        List<String> options = new ArrayList<>();
        for (String option : creationOptions) {
            options.add("-co");
            options.add(option);
        }
        return options;
    }

    private PancakeProgressListener getListener(){
        return listener;
    }

    @Override
    public void setProgressListener(PancakeProgressListener listener){
        this.listener = Optional.ofNullable(listener).orElse(PancakeProgressListener.empty);
    }

    @Override
    public PancakeDataset resample(PancakeDataset src, int outWidth, int outHeight, File dest, PancakeOptions options) {
        options = Optional.ofNullable(options).orElse(new PancakeOptions());

        Dataset gdalSrc = GdalHelper.convert(src);

        String outFormat = Optional.ofNullable(options.getString(Resampler.OUT_FORMAT))
                .orElse(gdalSrc.GetDriver().getShortName());

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

        double[] minMax = GdalHelper.computeMinMax(gdalSrc);

        to.add("-scale");
        to.add(Integer.toString((int) minMax[0]));
        to.add(Integer.toString((int) minMax[1]));
        to.add(Integer.toString((int) Pancake.dtMin(dtype)));
        to.add(Integer.toString((int) Pancake.dtMax(dtype)));

        List<String> co = Formats.byName(outFormat).toDriverOptions(options).getAsGdalOptions();
        to.addAll(getAsArgList(co));

        Dataset gdalDst = gdal.Translate(dest.getAbsolutePath(), gdalSrc, new TranslateOptions(new Vector<>(to)),
                callback);
        return new GdalDatasetMirror(gdalDst);
    }
}
