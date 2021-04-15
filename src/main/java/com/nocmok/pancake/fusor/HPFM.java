package com.nocmok.pancake.fusor;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.nocmok.pancake.Pancake;
import com.nocmok.pancake.PancakeBand;
import com.nocmok.pancake.PancakeConstants;
import com.nocmok.pancake.PancakeProgressListener;
import com.nocmok.pancake.Spectrum;
import com.nocmok.pancake.math.Buffer2D;
import com.nocmok.pancake.math.Math2D;
import com.nocmok.pancake.math.Math2D.Stat;
import com.nocmok.pancake.math.Filter2D;
import com.nocmok.pancake.utils.Rectangle;
import com.nocmok.pancake.utils.Shape;

public class HPFM implements Fusor {

    private Filter2D filter;

    private PancakeProgressListener listener = PancakeProgressListener.empty;

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

    /** TODO */
    private void validateFilter(Filter2D filter) {
    }

    public HPFM(Filter2D filter) {
        validateFilter(filter);
        this.filter = filter;
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
     * 
     * @param band
     * @param xsize block width
     * @param ysize block height
     * @param block block number
     * @param buf
     */
    private void cacheBlock(PancakeBand band, int xsize, int ysize, int block, ByteBuffer buf) {
        int blockXSize = xsize;
        int blockYSize = Integer.min(ysize, band.getYSize() - (block * ysize));
        try {
            band.readRasterDirect(0, block * ysize, blockXSize, blockYSize, blockXSize, blockYSize,
                    band.getRasterDatatype(), buf);
        } catch (RuntimeException e) {
            throw new RuntimeException("failed to cache block", e);
        }
    }

    /**
     * 
     * @param band
     * @param xsize block width
     * @param ysize block height
     * @param block block number
     * @param buf
     */
    private void flushBlock(PancakeBand band, int xsize, int ysize, int block, ByteBuffer buf) {
        int blockXSize = xsize;
        int blockYSize = Integer.min(ysize, band.getYSize() - (block * ysize));
        try {
            band.writeRasterDirect(0, block * ysize, blockXSize, blockYSize, blockXSize, blockYSize,
                    band.getRasterDatatype(), buf);
        } catch (RuntimeException e) {
            throw new RuntimeException("failed to flush block cache", e);
        }
    }

    private void _fuse(Map<Spectrum, ? extends PancakeBand> dst, Map<Spectrum, ? extends PancakeBand> src,
            Rectangle region) {

        Shape imgsize = Shape.of(src.get(Spectrum.PA).getXSize(), src.get(Spectrum.PA).getYSize());
        Shape tilesize = Shape.of(src.get(Spectrum.PA).getBlockXSize(), src.get(Spectrum.PA).getBlockYSize());
        Shape kernelsize = Shape.of(filter.getKernel()[0].length, filter.getKernel().length);
        Shape blocksize = computeCacheBlockSize(imgsize, tilesize, kernelsize);

        List<PancakeBand> srcMs = new ArrayList<>();
        List<PancakeBand> dstMs = new ArrayList<>();
        PancakeBand pa = src.get(Spectrum.PA);

        for (Spectrum s : Spectrum.RGB()) {
            srcMs.add(src.get(s));
            dstMs.add(dst.get(s));
        }

        List<ByteBuffer> paCachePool = new ArrayList<>(3);
        List<ByteBuffer> paCache = new ArrayList<>();
        for (int i = 0; i < 3; ++i) {
            paCachePool.add(ByteBuffer.allocateDirect(blocksize.size() * Pancake.dtBytes(pa.getRasterDatatype()))
                    .order(ByteOrder.nativeOrder()));
        }

        int srcMsCacheSize = blocksize.size()
                * srcMs.stream().mapToInt(b -> Pancake.dtBytes(b.getRasterDatatype())).max().getAsInt();
        int dstMsCacheSize = blocksize.size()
                * dstMs.stream().mapToInt(b -> Pancake.dtBytes(b.getRasterDatatype())).max().getAsInt();

        ByteBuffer srcMsCache = ByteBuffer.allocateDirect(srcMsCacheSize).order(ByteOrder.nativeOrder());
        ByteBuffer dstMsCache = ByteBuffer.allocateDirect(dstMsCacheSize).order(ByteOrder.nativeOrder());

        Buffer2D convBuf = Buffer2D.arrange(blocksize.xsize(), 3 * blocksize.ysize(),
                convDtMap.get(pa.getRasterDatatype()));

        Buffer2D fuseBuf = Buffer2D.arrange(blocksize.xsize(), blocksize.ysize(),
                convDtMap.get(pa.getRasterDatatype()));

        Math2D math2d = new Math2D();

        int blocks = (pa.getYSize() + blocksize.ysize() - 1) / blocksize.ysize();

        int nBlock = 0;
        int stepSize = (blocks + Pancake.logsFrequency() - 1) / Pancake.logsFrequency();
        int stepsTotal = (blocks + stepSize - 1) / stepSize;
        listener.listen(PancakeConstants.PROGRESS_FUSION, 0D, "[HPFM] performing fusion");

        for (int block = 0; block < blocks; ++block) {
            int ysize = pa.getYSize();
            int blockXSize = blocksize.xsize();
            int blockYSize = Integer.min(blocksize.ysize(), ysize - block * blocksize.ysize());

            List<Buffer2D> paBufs = new ArrayList<>();
            Rectangle roi;

            if (block == 0) {
                paCache.add(paCachePool.get(0));
                paCache.add(paCachePool.get(1));
                cacheBlock(pa, blocksize.xsize(), blocksize.ysize(), block, paCache.get(0));
                cacheBlock(pa, blocksize.xsize(), blocksize.ysize(), block + 1, paCache.get(1));
                paBufs.add(Buffer2D.wrap(paCache.get(0), blockXSize, blockYSize, pa.getRasterDatatype()));
                paBufs.add(Buffer2D.wrap(paCache.get(1), blockXSize, blockYSize, pa.getRasterDatatype()));
                roi = new Rectangle(0, 0, blockXSize, blockYSize);
            } else if (block < blocks - 1) {
                if (paCache.size() < 3) {
                    paCache.add(paCachePool.get(2));
                } else {
                    ByteBuffer buf = paCache.get(0);
                    paCache.remove(0);
                    paCache.add(buf);
                }
                cacheBlock(pa, blocksize.xsize(), blocksize.ysize(), block + 1, paCache.get(2));
                paBufs.add(Buffer2D.wrap(paCache.get(0), blockXSize, blockYSize, pa.getRasterDatatype()));
                paBufs.add(Buffer2D.wrap(paCache.get(1), blockXSize, blockYSize, pa.getRasterDatatype()));
                paBufs.add(Buffer2D.wrap(paCache.get(2), blockXSize, blockYSize, pa.getRasterDatatype()));
                roi = new Rectangle(0, blocksize.ysize(), blockXSize, blockYSize);
            } else {
                paBufs.add(Buffer2D.wrap(paCache.get(1), blockXSize, blocksize.ysize(), pa.getRasterDatatype()));
                paBufs.add(Buffer2D.wrap(paCache.get(2), blockXSize, blockYSize, pa.getRasterDatatype()));
                roi = new Rectangle(0, blocksize.ysize(), blockXSize, blockYSize);
            }

            math2d.vconcat(paBufs, convBuf);
            math2d.convert(convBuf, convDtMap.get(pa.getRasterDatatype()), convBuf);
            math2d.convolve(convBuf, this.filter, convBuf);
            Buffer2D convRoi = math2d.subBuffer(convBuf, roi);

            Iterator<PancakeBand> srcMsIt = srcMs.iterator();
            Iterator<PancakeBand> dstMsIt = dstMs.iterator();

            while (srcMsIt.hasNext() && dstMsIt.hasNext()) {
                PancakeBand srcMsBand = srcMsIt.next();
                PancakeBand dstMsBand = dstMsIt.next();

                cacheBlock(srcMsBand, blocksize.xsize(), blocksize.ysize(), block, srcMsCache);
                cacheBlock(dstMsBand, blocksize.xsize(), blocksize.ysize(), block, dstMsCache);

                Buffer2D srcBuf = Buffer2D.wrap(srcMsCache, blockXSize, blockYSize, srcMsBand.getRasterDatatype());
                Buffer2D dstBuf = Buffer2D.wrap(dstMsCache, blockXSize, blockYSize, dstMsBand.getRasterDatatype());

                math2d.convertAndScale(srcBuf, fuseBuf.datatype(), fuseBuf, Pancake.dtMin(srcBuf.datatype()), Pancake.dtMax(srcBuf.datatype()), 0,
                        Pancake.dtMax(pa.getRasterDatatype()));
                math2d.sum(fuseBuf, convRoi, fuseBuf);

                Stat stat = math2d.stat(fuseBuf);
                if ((stat.max() - stat.min()) == 0) {
                    double placeholder = Pancake.convert(stat.max(), pa.getRasterDatatype(), dstBuf.datatype());
                    math2d.fill(dstBuf, placeholder);
                } else {
                    math2d.convertAndScale(fuseBuf, dstBuf.datatype(), dstBuf, -Pancake.dtMax(pa.getRasterDatatype()),
                            2 * Pancake.dtMax(pa.getRasterDatatype()), 0, Pancake.dtMax(dstBuf.datatype()));
                }

                flushBlock(dstMsBand, blocksize.xsize(), blocksize.ysize(), block, dstMsCache);
            }

            if (((nBlock + 1) % stepSize == 0) || (nBlock + 1 >= blocks)) {
                double progress = (nBlock / stepSize + 1) / (double) stepsTotal;
                listener.listen(PancakeConstants.PROGRESS_FUSION, progress, "[HPFM] performing fusion");
            }

            ++nBlock;
        }

    }

    private void _fuseMemoryPolite(Map<Spectrum, ? extends PancakeBand> dst, Map<Spectrum, ? extends PancakeBand> src,
            Rectangle region) {

        Shape imgsize = Shape.of(src.get(Spectrum.PA).getXSize(), src.get(Spectrum.PA).getYSize());
        Shape tilesize = Shape.of(src.get(Spectrum.PA).getBlockXSize(), src.get(Spectrum.PA).getBlockYSize());
        Shape kernelsize = Shape.of(filter.getKernel()[0].length, filter.getKernel().length);
        Shape blocksize = computeCacheBlockSize(imgsize, tilesize, kernelsize);

        List<PancakeBand> srcMs = new ArrayList<>();
        List<PancakeBand> dstMs = new ArrayList<>();
        PancakeBand pa = src.get(Spectrum.PA);

        for (Spectrum s : Spectrum.RGB()) {
            srcMs.add(src.get(s));
            dstMs.add(dst.get(s));
        }

        ByteBuffer paCache = ByteBuffer.allocateDirect(blocksize.size() * Pancake.dtBytes(pa.getRasterDatatype()))
                .order(ByteOrder.nativeOrder());

        int srcMsCacheSize = blocksize.size()
                * srcMs.stream().mapToInt(b -> Pancake.dtBytes(b.getRasterDatatype())).max().getAsInt();
        int dstMsCacheSize = blocksize.size()
                * dstMs.stream().mapToInt(b -> Pancake.dtBytes(b.getRasterDatatype())).max().getAsInt();

        ByteBuffer srcMsCache = ByteBuffer.allocateDirect(srcMsCacheSize).order(ByteOrder.nativeOrder());
        ByteBuffer dstMsCache = ByteBuffer.allocateDirect(dstMsCacheSize).order(ByteOrder.nativeOrder());

        Buffer2D convBuf = Buffer2D.arrange(blocksize.xsize(), blocksize.ysize(),
                convDtMap.get(pa.getRasterDatatype()));

        Buffer2D fuseBuf = Buffer2D.arrange(blocksize.xsize(), blocksize.ysize(),
                convDtMap.get(pa.getRasterDatatype()));

        Math2D math2d = new Math2D();

        int blocks = (pa.getYSize() + blocksize.ysize() - 1) / blocksize.ysize();

        int nBlock = 0;
        int stepSize = (blocks + Pancake.logsFrequency() - 1) / Pancake.logsFrequency();
        int stepsTotal = (blocks + stepSize - 1) / stepSize;
        listener.listen(PancakeConstants.PROGRESS_FUSION, 0D, "[HPFM] performing fusion");

        for (int block = 0; block < blocks; ++block) {
            int ysize = pa.getYSize();
            int blockXSize = blocksize.xsize();
            int blockYSize = Integer.min(blocksize.ysize(), ysize - block * blocksize.ysize());

            cacheBlock(pa, blockXSize, blockYSize, block, paCache);
            Buffer2D paBuf = Buffer2D.wrap(paCache, blockXSize, blockYSize, pa.getRasterDatatype());
            math2d.convert(paBuf, convBuf);
            math2d.convolve(convBuf, this.filter, convBuf);

            Iterator<PancakeBand> srcMsIt = srcMs.iterator();
            Iterator<PancakeBand> dstMsIt = dstMs.iterator();

            while (srcMsIt.hasNext() && dstMsIt.hasNext()) {
                PancakeBand srcMsBand = srcMsIt.next();
                PancakeBand dstMsBand = dstMsIt.next();

                cacheBlock(srcMsBand, blocksize.xsize(), blocksize.ysize(), block, srcMsCache);
                cacheBlock(dstMsBand, blocksize.xsize(), blocksize.ysize(), block, dstMsCache);

                Buffer2D srcBuf = Buffer2D.wrap(srcMsCache, blockXSize, blockYSize, srcMsBand.getRasterDatatype());
                Buffer2D dstBuf = Buffer2D.wrap(dstMsCache, blockXSize, blockYSize, dstMsBand.getRasterDatatype());

                math2d.convertAndScale(srcBuf, fuseBuf.datatype(), fuseBuf, 0, Pancake.dtMax(srcBuf.datatype()), 0,
                        Pancake.dtMax(pa.getRasterDatatype()));
                math2d.sum(fuseBuf, convBuf, fuseBuf);

                Stat stat = math2d.stat(fuseBuf);
                if ((stat.max() - stat.min()) == 0) {
                    double placeholder = Pancake.convert(stat.max(), pa.getRasterDatatype(), dstBuf.datatype());
                    math2d.fill(dstBuf, placeholder);
                } else {
                    math2d.convertAndScale(fuseBuf, dstBuf.datatype(), dstBuf, -Pancake.dtMax(pa.getRasterDatatype()),
                            2 * Pancake.dtMax(pa.getRasterDatatype()), 0, Pancake.dtMax(dstBuf.datatype()));
                }

                flushBlock(dstMsBand, blocksize.xsize(), blocksize.ysize(), block, dstMsCache);
            }

            if (((nBlock + 1) % stepSize == 0) || (nBlock + 1 >= blocks)) {
                double progress = (nBlock / stepSize + 1) / (double) stepsTotal;
                listener.listen(PancakeConstants.PROGRESS_FUSION, progress, "[HPFM] performing fusion");
            }

            ++nBlock;
        }
    }

    private boolean useMemoryPolite(int imgSize) {
        /** 150 mb */
        int memThreshold = 150 * 1024 * 1024;
        return imgSize > memThreshold;
    }

    @Override
    public void fuse(Map<Spectrum, ? extends PancakeBand> dst, Map<Spectrum, ? extends PancakeBand> src) {
        // some validation routine
        PancakeBand band = src.get(Spectrum.PA);
        Rectangle region = new Rectangle(0, 0, band.getXSize(), band.getYSize());
        if (useMemoryPolite(band.getXSize() * band.getYSize() * Pancake.dtBytes(band.getRasterDatatype()))) {
            _fuseMemoryPolite(dst, src, region);
        } else {
            _fuse(dst, src, region);
        }
    }

    @Override
    public void fuse(Map<Spectrum, ? extends PancakeBand> dst, Map<Spectrum, ? extends PancakeBand> src,
            Rectangle region) {
        // some validation routine
        PancakeBand band = src.get(Spectrum.PA);
        if (useMemoryPolite(band.getXSize() * band.getYSize() * Pancake.dtBytes(band.getRasterDatatype()))) {
            _fuseMemoryPolite(dst, src, region);
        } else {
            _fuse(dst, src, region);
        }
    }

    @Override
    public void setProgressListener(PancakeProgressListener listener) {
        this.listener = Optional.ofNullable(listener).orElse(PancakeProgressListener.empty);
    }

}
