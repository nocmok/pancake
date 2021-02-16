package com.nocmok.pancake.fusor;

import java.nio.ByteBuffer;

import org.gdal.gdal.Band;

public class GdalBandMirror implements PancakeBand {

    private Band _band;

    public GdalBandMirror(Band band) {
        this._band = band;
    }

    @Override
    public int readRasterDirect(int xoff, int yoff, int xsize, int ysize, int buf_xsize, int buf_ysize, int buf_type,
            ByteBuffer nioBuffer) {
        return _band.ReadRaster_Direct(xoff, yoff, xsize, ysize, nioBuffer);
    }

    @Override
    public int readRasterDirect​(int xoff, int yoff, int xsize, int ysize, int buf_xsize, int buf_ysize, int buf_type,
            ByteBuffer nioBuffer, int nPixelSpace, int nLineSpace) {
        return _band.ReadRaster_Direct(xoff, yoff, xsize, ysize, buf_xsize, buf_ysize, buf_type, nioBuffer, nPixelSpace,
                nLineSpace);
    }

    @Override
    public int readRasterDirect​(int xoff, int yoff, int xsize, int ysize, int buf_xsize, int buf_ysize,
            ByteBuffer nioBuffer) {
        return _band.ReadRaster_Direct(xoff, yoff, xsize, ysize, buf_xsize, buf_ysize, nioBuffer);
    }

    @Override
    public int readRasterDirect​(int xoff, int yoff, int xsize, int ysize, ByteBuffer nioBuffer) {
        return _band.ReadRaster_Direct(xoff, yoff, xsize, ysize, nioBuffer);
    }

    @Override
    public int writeRasterDirect​(int xoff, int yoff, int xsize, int ysize, int buf_xsize, int buf_ysize, int buf_type,
            ByteBuffer nioBuffer) {
        return _band.WriteRaster_Direct(xoff, yoff, xsize, ysize, buf_type, nioBuffer);
    }

    @Override
    public int writeRasterDirect​(int xoff, int yoff, int xsize, int ysize, int buf_xsize, int buf_ysize, int buf_type,
            ByteBuffer nioBuffer, int nPixelSpace, int nLineSpace) {
        return _band.WriteRaster_Direct(xoff, yoff, xsize, ysize, buf_xsize, buf_ysize, buf_type, nioBuffer,
                nPixelSpace);
    }

    @Override
    public int writeRasterDirect​(int xoff, int yoff, int xsize, int ysize, int buf_xsize, int buf_ysize,
            ByteBuffer nioBuffer) {
        return _band.WriteRaster_Direct(xoff, yoff, xsize, ysize, buf_xsize, buf_ysize, nioBuffer);
    }

    @Override
    public int writeRasterDirect​(int xoff, int yoff, int xsize, int ysize, int buf_type, ByteBuffer nioBuffer) {
        return _band.WriteRaster_Direct(xoff, yoff, xsize, ysize, buf_type, nioBuffer);
    }

    @Override
    public int writeRasterDirect​(int xoff, int yoff, int xsize, int ysize, ByteBuffer nioBuffer) {
        return _band.WriteRaster_Direct(xoff, yoff, xsize, ysize, nioBuffer);
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
}
