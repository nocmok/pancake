package com.nocmok.pancake.fusor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.nocmok.pancake.Pancake;
import com.nocmok.pancake.PancakeBand;
import com.nocmok.pancake.Spectrum;
import com.nocmok.pancake.math.Buffer2D;
import com.nocmok.pancake.math.Math2D;
import com.nocmok.pancake.math.Math2D.Stat;
import com.nocmok.pancake.math.Filter2D;
import com.nocmok.pancake.utils.BandIntTileReader;
import com.nocmok.pancake.utils.Rectangle;
import com.nocmok.pancake.utils.Shape;

public class HPFM implements Fusor {

    private Filter2D filter;

    /**
     * Determines which data type to use in order to store convolution result for
     * raster with specific data type
     */
    private static final Map<Integer, Integer> convDtMap = new HashMap<>();

    /**
     * Types conversion map. Opencv doesn't work with several types, so its
     * necessary to perform conversion in some cases
     */
    private static final Map<Integer, Integer> dtConversion = new HashMap<>();

    static {
        convDtMap.put(Pancake.TYPE_BYTE, Pancake.TYPE_INT_16);
        convDtMap.put(Pancake.TYPE_INT_16, Pancake.TYPE_FLOAT_32);
        convDtMap.put(Pancake.TYPE_UINT_16, Pancake.TYPE_FLOAT_32);
        convDtMap.put(Pancake.TYPE_INT_32, Pancake.TYPE_FLOAT_32);
        convDtMap.put(Pancake.TYPE_UINT_32, Pancake.TYPE_FLOAT_32);
        convDtMap.put(Pancake.TYPE_FLOAT_32, Pancake.TYPE_FLOAT_64);
        convDtMap.put(Pancake.TYPE_FLOAT_64, Pancake.TYPE_FLOAT_64);

        dtConversion.put(Pancake.TYPE_BYTE, Pancake.TYPE_BYTE);
        dtConversion.put(Pancake.TYPE_INT_16, Pancake.TYPE_INT_16);
        dtConversion.put(Pancake.TYPE_UINT_16, Pancake.TYPE_UINT_16);
        dtConversion.put(Pancake.TYPE_INT_32, Pancake.TYPE_INT_16);
        dtConversion.put(Pancake.TYPE_UINT_32, Pancake.TYPE_UINT_16);
        dtConversion.put(Pancake.TYPE_FLOAT_32, Pancake.TYPE_FLOAT_32);
        dtConversion.put(Pancake.TYPE_FLOAT_64, Pancake.TYPE_FLOAT_64);
    }

    public HPFM(Filter2D filter) {
        this.filter = filter;
    }

    @Override
    public void fuse(Map<Spectrum, ? extends PancakeBand> dst, Map<Spectrum, ? extends PancakeBand> src) {
        // some validation routine
        PancakeBand band = src.get(Spectrum.PA);
        int xsize = band.getXSize();
        int ysize = band.getYSize();
        _fuse(dst, src, new Rectangle(0, 0, xsize, ysize));
    }

    private Shape computeCacheBlockSize(Shape imgsize, Shape tilesize, Shape kernelsize) {
        int xsize = imgsize.xsize();
        /** block height >= kernel height - 1 */
        int tilesInCol = Integer.max(0, kernelsize.ysize() - 2) / tilesize.ysize() + 1;
        int ysize = tilesize.ysize();
        ysize *= tilesInCol;
        ysize = Integer.min(ysize, imgsize.ysize());
        return Shape.of(xsize, ysize);
    }

    /**
     * Creates 2d buffer from currently cached block
     * 
     * @param band
     * @return
     */
    private Buffer2D buffer2dFromBandCache(BandIntTileReader band) {
        if (!band.hasBlockInCache()) {
            throw new RuntimeException("attempt to access band data, that was not cached");
        }
        int[] block = band.getBlockInCache();
        return Buffer2D.wrap(band.getCache(), band.blockXSize(block[0]), band.blockYSize(block[1]),
                band.getNativeDatatype());
    }

    private void _fuse(Map<Spectrum, ? extends PancakeBand> dst, Map<Spectrum, ? extends PancakeBand> src,
            Rectangle region) {

        Shape imgsize = Shape.of(src.get(Spectrum.PA).getXSize(), src.get(Spectrum.PA).getYSize());
        Shape tilesize = Shape.of(src.get(Spectrum.PA).getBlockXSize(), src.get(Spectrum.PA).getBlockYSize());
        Shape kernelsize = Shape.of(filter.getKernel()[0].length, filter.getKernel().length);
        Shape blocksize = computeCacheBlockSize(imgsize, tilesize, kernelsize);

        List<BandIntTileReader> ms0 = new ArrayList<>();
        List<BandIntTileReader> ms = new ArrayList<>();
        List<BandIntTileReader> allbands = new ArrayList<>();
        BandIntTileReader pa = new BandIntTileReader(src.get(Spectrum.PA), blocksize.xsize(), blocksize.ysize());

        for (Spectrum s : Spectrum.RGB()) {
            ms0.add(new BandIntTileReader(src.get(s), blocksize.xsize(), blocksize.ysize()));
            ms.add(new BandIntTileReader(dst.get(s), blocksize.xsize(), blocksize.ysize()));
        }

        allbands.addAll(ms0);
        allbands.addAll(ms);
        allbands.add(pa);

        Buffer2D convBuf = Buffer2D.arrange(blocksize.xsize(), blocksize.ysize(),
                convDtMap.get(pa.getNativeDatatype()));
        Buffer2D fuseBuf = Buffer2D.arrange(blocksize.xsize(), blocksize.ysize(),
                convDtMap.get(pa.getNativeDatatype()));

        Math2D math2d = new Math2D();

        int blockY0 = pa.toBlockY(region.y0() - 1);
        int blockX0 = pa.toBlockX(region.x0() - 1);
        int blockY1 = pa.toBlockY(region.y1() - 1);
        int blockX1 = pa.toBlockX(region.x1() - 1);

        for (int blockY = blockY0; blockY <= blockY1; ++blockY) {
            for (int blockX = blockX0; blockX <= blockX1; ++blockX) {
                for (BandIntTileReader band : allbands) {
                    band.cacheBlock(blockX, blockY);
                }
                Buffer2D paBuf = buffer2dFromBandCache(pa);
                math2d.convertAndScale(paBuf, dtConversion.get(paBuf.datatype()), paBuf);
                math2d.convolve(paBuf, this.filter, convBuf);
                Iterator<BandIntTileReader> ms0It = ms0.iterator();
                Iterator<BandIntTileReader> msIt = ms.iterator();
                while (ms0It.hasNext() && msIt.hasNext()) {
                    Buffer2D ms0Buf = buffer2dFromBandCache(ms0It.next());
                    Buffer2D msBuf = buffer2dFromBandCache(msIt.next());
                    double scale = Pancake.getDatatypeMax(ms0Buf.datatype()) / Pancake.getDatatypeMax(paBuf.datatype());
                    math2d.convert(convBuf, fuseBuf, scale, 0.0);
                    math2d.sum(fuseBuf, ms0Buf, fuseBuf);
                    Stat stat = math2d.stat(fuseBuf);
                    if ((stat.max() - stat.min()) == 0) {
                        math2d.fill(msBuf, Pancake.getDatatypeMax(msBuf.datatype()));
                    } else {
                        double alpha = Pancake.getDatatypeMax(msBuf.datatype()) / (stat.max() - stat.min());
                        double beta = -stat.min() * Pancake.getDatatypeMax(msBuf.datatype())
                                / (stat.max() - stat.min());
                        math2d.convert(fuseBuf, msBuf, alpha, beta);
                    }
                }
                for (BandIntTileReader band : ms) {
                    band.flushCacheAnyway();
                }
            }
        }

    }

    @Override
    public void fuse(Map<Spectrum, ? extends PancakeBand> dst, Map<Spectrum, ? extends PancakeBand> src,
            Rectangle region) {
        // some validation routine
        _fuse(dst, src, region);
    }

}
