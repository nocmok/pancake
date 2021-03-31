package com.nocmok.pancake;

import java.io.File;
import java.util.Map;

import com.nocmok.pancake.fusor.Fusor;
import com.nocmok.pancake.resampler.Resampler;
import com.nocmok.pancake.utils.PancakeOptions;

import org.gdal.gdal.Band;

public class PansharpJobBuilder {

    private PancakeOptions options;

    private Resampler resampler;

    private Fusor fusor;

    private Map<Spectrum, Band> mapping;

    public PansharpJobBuilder() {
        options = new PancakeOptions();
    }

    public PansharpJobBuilder withOutputFormat(Formats format) {
        options.put(PansharpJob.JOB_TARGET_FORMAT, format.driverName());
        return this;
    }

    public PansharpJobBuilder withOutputDatatype(int dtype) {
        options.put(PansharpJob.JOB_TARGET_DATATYPE, dtype);
        return this;
    }

    public PansharpJobBuilder withResampler(Resampler resampler) {
        this.resampler = resampler;
        return this;
    }

    public PansharpJobBuilder withFusor(Fusor fusor) {
        this.fusor = fusor;
        return this;
    }

    public PansharpJobBuilder withThreads(int numThreads) {
        options.put(PansharpJob.JOB_NUM_THREADS, numThreads);
        return this;
    }

    public PansharpJobBuilder withCompression(Compression compression) {
        options.put(PansharpJob.JOB_COMPRESSION, compression.gdalIdentifier());
        return this;
    }

    public PansharpJobBuilder withCompressionQuality(int quality) {
        options.put(PansharpJob.JOB_COMPRESSION_QUALITY, quality);
        return this;
    }

    public PansharpJobBuilder withOutputFile(File outputFile) {
        options.put(PansharpJob.JOB_TARGET_PATH, outputFile.getAbsolutePath());
        return this;
    }

    public PansharpJobBuilder withSource(Map<Spectrum, Band> source) {
        this.mapping = source;
        return this;
    }

    public PansharpJobBuilder useHistMatching(boolean useHistMathcing) {
        options.put(PansharpJob.JOB_USE_HIST_MATCHING, useHistMathcing);
        return this;
    }

    public PansharpJobBuilder with(String key, Object value) {
        options.put(key, value);
        return this;
    }

    public PansharpJob build() {
        PansharpJob job = new PansharpJob(resampler, fusor, mapping, options);
        return job;
    }
}
