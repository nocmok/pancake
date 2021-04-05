package com.nocmok.pancake;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.gdal.gdal.Dataset;

class GdalDatasetMirror implements PancakeDataset {

    private Dataset ds;

    public GdalDatasetMirror(Dataset ds) {
        this.ds = ds;
    }

    private List<PancakeBand> split(Dataset ds) {
        List<PancakeBand> bands = new ArrayList<>();
        for (int i = 0; i < ds.getRasterCount(); ++i) {
            bands.add(new GdalBandMirror(ds.GetRasterBand(i + 1)));
        }
        return bands;
    }

    @Override
    public List<PancakeBand> bands() {
        return split(ds);
    }

    @Override
    public File path() {
        return new File(GdalHelper.pathTo(ds));
    }

    @Override
    public void flushCache() {
        ds.FlushCache();
    }

    public Dataset getUnderlyingDataset(){
        return ds;
    }
}
