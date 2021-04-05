package com.nocmok.pancake;

import java.nio.ByteBuffer;
import java.util.Optional;

import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;

public class GdalBandMirror implements PancakeBand {

    private Band _band;

    private Double[] minVal;

    private Double[] maxVal;

    private PancakeDataset dataset;

    public GdalBandMirror(Band band) {
        this._band = band;
        this.minVal = new Double[1];
        this.maxVal = new Double[1];
        band.GetMaximum(maxVal);
        band.GetMinimum(minVal);
        maxVal[0] = Optional.ofNullable(maxVal[0]).orElse(Pancake.dtMax(band.GetRasterDataType()));
        minVal[0] = Optional.ofNullable(minVal[0]).orElse(Pancake.dtMin(band.GetRasterDataType()));
        this.dataset = new GdalDatasetMirror(_band.GetDataset());
    }

    @Override
    public int readRasterDirect(int xOff, int yOff, int xSize, int ySize, int bufXSize, int bufYSize, int bufType,
            ByteBuffer buffer) {
        int code = _band.ReadRaster_Direct(xOff, yOff, xSize, ySize, bufXSize, bufYSize, bufType, buffer);
        if (code == gdalconst.CE_Failure) {
            throw new RuntimeException("failed to read raster: " + gdal.GetLastErrorMsg());
        }
        return code;
    }

    @Override
    public int readRasterDirect(int xOff, int yOff, int xSize, int ySize, int bufXSize, int bufYSize, int bufType,
            ByteBuffer buffer, int nPixelSpace, int nLineSpace) {
        int code = _band.ReadRaster_Direct(xOff, yOff, xSize, ySize, bufXSize, bufYSize, bufType, buffer, nPixelSpace,
                nLineSpace);
        if (code == gdalconst.CE_Failure) {
            throw new RuntimeException("failed to read raster: " + gdal.GetLastErrorMsg());
        }
        return code;
    }

    @Override
    public int readRasterDirect(int xOff, int yOff, int xSize, int ySize, int bufXSize, int bufYSize,
            ByteBuffer buffer) {
        int code = _band.ReadRaster_Direct(xOff, yOff, xSize, ySize, bufXSize, bufYSize, buffer);
        if (code == gdalconst.CE_Failure) {
            throw new RuntimeException("failed to read raster: " + gdal.GetLastErrorMsg());
        }
        return code;
    }

    @Override
    public int readRasterDirect(int xOff, int yOff, int xSize, int ySize, ByteBuffer buffer) {
        int code = _band.ReadRaster_Direct(xOff, yOff, xSize, ySize, buffer);
        if (code == gdalconst.CE_Failure) {
            throw new RuntimeException("failed to read raster: " + gdal.GetLastErrorMsg());
        }
        return code;
    }

    @Override
    public int writeRasterDirect(int xOff, int yOff, int xSize, int ySize, int bufXSize, int bufYSize, int bufType,
            ByteBuffer buffer) {
        int code = _band.WriteRaster_Direct(xOff, yOff, xSize, ySize, bufXSize, bufYSize, bufType, buffer);
        if (code == gdalconst.CE_Failure) {
            throw new RuntimeException("failed to read raster: " + gdal.GetLastErrorMsg());
        }
        return code;
    }

    @Override
    public int writeRasterDirect(int xOff, int yOff, int xSize, int ySize, int bufXSize, int bufYSize, int bufType,
            ByteBuffer buffer, int nPixelSpace, int nLineSpace) {
        int code = _band.WriteRaster_Direct(xOff, yOff, xSize, ySize, bufXSize, bufYSize, bufType, buffer, nPixelSpace,
                nLineSpace);
        if (code == gdalconst.CE_Failure) {
            throw new RuntimeException("failed to read raster: " + gdal.GetLastErrorMsg());
        }
        return code;
    }

    @Override
    public int writeRasterDirect(int xOff, int yOff, int xSize, int ySize, int bufXsize, int bufYsize,
            ByteBuffer buffer) {
        int code = _band.WriteRaster_Direct(xOff, yOff, xSize, ySize, bufXsize, bufYsize, buffer);
        if (code == gdalconst.CE_Failure) {
            throw new RuntimeException("failed to read raster: " + gdal.GetLastErrorMsg());
        }
        return code;
    }

    @Override
    public int writeRasterDirect(int xOff, int yOff, int xSize, int ySize, ByteBuffer buffer) {
        int code = _band.WriteRaster_Direct(xOff, yOff, xSize, ySize, buffer);
        if (code == gdalconst.CE_Failure) {
            throw new RuntimeException("failed to read raster: " + gdal.GetLastErrorMsg());
        }
        return code;
    }

    @Override
    public int getXSize() {
        return _band.getXSize();
    }

    @Override
    public int getYSize() {
        return _band.getYSize();
    }

    @Override
    public int getBlockXSize() {
        return _band.GetBlockXSize();
    }

    @Override
    public int getBlockYSize() {
        return _band.GetBlockYSize();
    }

    @Override
    public int getRasterDatatype() {
        return _band.GetRasterDataType();
    }

    @Override
    public double getNoData() {
        Double[] noData = new Double[1];
        _band.GetNoDataValue(noData);
        return Optional.ofNullable(noData[0]).orElse(0.0);
    }

    @Override
    public double maxValue() {
        return maxVal[0];
    }

    @Override
    public double minValue() {
        return minVal[0];
    }

    public Band getUnderlyingBand() {
        return _band;
    }

    Dataset dataset() {
        return _band.GetDataset();
    }
}
