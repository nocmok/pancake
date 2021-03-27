package com.nocmok.pancake;

import java.nio.ByteBuffer;

import org.gdal.gdal.Band;

/** Interface for gdal raster band wrappers */
public interface PancakeBand {

        public int readRasterDirect(int xoff, int yoff, int xsize, int ysize, int buf_xsize, int buf_ysize,
                        int buf_type, ByteBuffer nioBuffer);

        public int readRasterDirect(int xoff, int yoff, int xsize, int ysize, int buf_xsize, int buf_ysize,
                        int buf_type, ByteBuffer nioBuffer, int nPixelSpace, int nLineSpace);

        public int readRasterDirect(int xoff, int yoff, int xsize, int ysize, int buf_xsize, int buf_ysize,
                        ByteBuffer nioBuffer);

        public int readRasterDirect(int xoff, int yoff, int xsize, int ysize, ByteBuffer nioBuffer);

        public int writeRasterDirect(int xoff, int yoff, int xsize, int ysize, int buf_xsize, int buf_ysize,
                        int buf_type, ByteBuffer nioBuffer);

        public int writeRasterDirect(int xoff, int yoff, int xsize, int ysize, int buf_xsize, int buf_ysize,
                        int buf_type, ByteBuffer nioBuffer, int nPixelSpace, int nLineSpace);

        public int writeRasterDirect(int xoff, int yoff, int xsize, int ysize, int buf_xsize, int buf_ysize,
                        ByteBuffer nioBuffer);

        public int writeRasterDirect(int xoff, int yoff, int xsize, int ysize, ByteBuffer nioBuffer);

        public int getXSize();

        public int getYSize();

        public int getBlockXSize();

        public int getBlockYSize();

        public int getRasterDatatype();

        public double getNoData();

        public Band getUnderlyingBand();
}
