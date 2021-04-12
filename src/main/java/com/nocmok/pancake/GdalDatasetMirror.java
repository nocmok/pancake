package com.nocmok.pancake;

import java.io.File;
import java.io.IOException;
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
            GdalBandMirror band = new GdalBandMirror(ds.GetRasterBand(i + 1));
            band.setDataset(this);
            bands.add(band);
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

    @Override
    public void close() throws IOException {
        ds.delete();        
    }

    @Override
    public int xSize() {
        return ds.getRasterXSize();
    }

    @Override
    public int ySize() {
        return ds.getRasterYSize();
    }
}
