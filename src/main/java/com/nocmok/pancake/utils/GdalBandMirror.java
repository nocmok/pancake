package com.nocmok.pancake.utils;

import java.nio.ByteBuffer;
import java.util.Optional;

import org.gdal.gdal.Band;

import com.nocmok.pancake.Pancake;
import com.nocmok.pancake.PancakeBand;

public class GdalBandMirror implements PancakeBand {

    private Band _band;

    private Double[] minVal;

    private Double[] maxVal;

    public GdalBandMirror(Band band) {
        this._band = band;
        this.minVal = new Double[1];
        this.maxVal = new Double[1];
        band.GetMaximum(maxVal);
        band.GetMinimum(minVal);
        maxVal[0] = Optional.ofNullable(maxVal[0]).orElse(Pancake.getDatatypeMax(band.GetRasterDataType()));    
        minVal[0] = Optional.ofNullable(minVal[0]).orElse(Pancake.getDatatypeMin(band.GetRasterDataType()));
    }

    @Override
    public int readRasterDirect(int xOff, int yOff, int xSize, int ySize, int bufXSize, int bufYSize, int bufType,
            ByteBuffer buffer) {
        return _band.ReadRaster_Direct(xOff, yOff, xSize, ySize, bufXSize, bufYSize, bufType, buffer);
    }

    @Override
    public int readRasterDirect(int xOff, int yOff, int xSize, int ySize, int bufXSize, int bufYSize, int bufType,
            ByteBuffer buffer, int nPixelSpace, int nLineSpace) {
        return _band.ReadRaster_Direct(xOff, yOff, xSize, ySize, bufXSize, bufYSize, bufType, buffer, nPixelSpace,
                nLineSpace);
    }

    @Override
    public int readRasterDirect(int xOff, int yOff, int xSize, int ySize, int bufXSize, int bufYSize,
            ByteBuffer buffer) {
        return _band.ReadRaster_Direct(xOff, yOff, xSize, ySize, bufXSize, bufYSize, buffer);
    }

    @Override
    public int readRasterDirect(int xOff, int yOff, int xSize, int ySize, ByteBuffer buffer) {
        return _band.ReadRaster_Direct(xOff, yOff, xSize, ySize, buffer);
    }

    @Override
    public int writeRasterDirect(int xOff, int yOff, int xSize, int ySize, int bufXSize, int bufYSize, int bufType,
            ByteBuffer buffer) {
        return _band.WriteRaster_Direct(xOff, yOff, xSize, ySize, bufXSize, bufYSize, bufType, buffer);
    }

    @Override
    public int writeRasterDirect(int xOff, int yOff, int xSize, int ySize, int bufXSize, int bufYSize, int bufType,
            ByteBuffer buffer, int nPixelSpace, int nLineSpace) {
        return _band.WriteRaster_Direct(xOff, yOff, xSize, ySize, bufXSize, bufYSize, bufType, buffer,
                nPixelSpace, nLineSpace);
    }

    @Override
    public int writeRasterDirect(int xOff, int yOff, int xSize, int ySize, int bufXsize, int bufYsize,
            ByteBuffer buffer) {
        return _band.WriteRaster_Direct(xOff, yOff, xSize, ySize, bufXsize, bufYsize, buffer);
    }

    @Override
    public int writeRasterDirect(int xOff, int yOff, int xSize, int ySize, ByteBuffer buffer) {
        return _band.WriteRaster_Direct(xOff, yOff, xSize, ySize, buffer);
    }

    @Override
    public Band getUnderlyingBand() {
        return _band;
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
}
