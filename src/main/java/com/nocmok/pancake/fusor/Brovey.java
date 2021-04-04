package com.nocmok.pancake.fusor;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.nocmok.pancake.Pancake;
import com.nocmok.pancake.Spectrum;
import com.nocmok.pancake.math.Buffer2D;
import com.nocmok.pancake.math.Math2D;
import com.nocmok.pancake.utils.Rectangle;

import com.nocmok.pancake.PancakeBand;

public class Brovey implements Fusor {

    /** weight of red band */
    double rWeight = 1.0;

    /** weight of green band */
    double gWeight = 1.0;

    /** weight of blue band */
    double bWeight = 1.0;

    public Brovey(double rWeight, double gWeight, double bWeight) {
        double max = Collections.max(List.of(rWeight, gWeight, bWeight));
        this.rWeight = rWeight / max;
        this.gWeight = gWeight / max;
        this.bWeight = gWeight / max;
    }

    public Brovey() {
        this(1.0, 1.0, 1.0);
    }

    private void validateDst(Map<Spectrum, ? extends PancakeBand> dst) {
        List<Spectrum> dstRequiredBands = List.of(Spectrum.R, Spectrum.G, Spectrum.B);
        for (Spectrum spect : dstRequiredBands) {
            if (!dst.containsKey(spect)) {
                throw new RuntimeException("missed " + spect.toString() + " band");
            }
        }
    }

    private void validateSrc(Map<Spectrum, ? extends PancakeBand> src) {
        List<Spectrum> srcRequiredBands = List.of(Spectrum.R, Spectrum.G, Spectrum.B, Spectrum.PA);
        for (Spectrum spect : srcRequiredBands) {
            if (!src.containsKey(spect)) {
                throw new RuntimeException("missed " + spect.toString() + " band");
            }
        }
    }

    private void validateArgs(Map<Spectrum, ? extends PancakeBand> dst, Map<Spectrum, ? extends PancakeBand> src) {
        validateDst(dst);
        validateSrc(src);
        int paXSize = src.get(Spectrum.PA).getXSize();
        int paYSize = src.get(Spectrum.PA).getYSize();
        List<PancakeBand> allBands = new ArrayList<>();
        allBands.addAll(dst.values());
        allBands.addAll(src.values());
        for (var band : allBands) {
            if ((band.getXSize() != paXSize) || (band.getYSize() != paYSize)) {
                throw new RuntimeException("one of provided band mismatch panchromatic band resolution");
            }
        }
    }

    private void cacheBlock(PancakeBand band, int xsize, int ysize, int blockX, int blockY, ByteBuffer buf) {
        int blockXSize = Integer.min(xsize, band.getXSize() - (blockX * xsize));
        int blockYSize = Integer.min(ysize, band.getYSize() - (blockY * ysize));
        try {
            band.readRasterDirect(blockX * xsize, blockY * ysize, blockXSize, blockYSize, blockXSize, blockYSize,
                    band.getRasterDatatype(), buf);
        } catch (RuntimeException e) {
            throw new RuntimeException("failed to cache block", e);
        }
    }

    private void flushBlock(PancakeBand band, int xsize, int ysize, int blockX, int blockY, ByteBuffer buf) {
        int blockXSize = Integer.min(xsize, band.getXSize() - (blockX * xsize));
        int blockYSize = Integer.min(ysize, band.getYSize() - (blockY * ysize));
        try {
            band.writeRasterDirect(blockX * xsize, blockY * ysize, blockXSize, blockYSize, blockXSize, blockYSize,
                    band.getRasterDatatype(), buf);
        } catch (RuntimeException e) {
            throw new RuntimeException("failed to flush block cache", e);
        }
    }

    private void _fuseInt(Map<Spectrum, ? extends PancakeBand> dst, Map<Spectrum, ? extends PancakeBand> src,
            Rectangle region) {
        List<PancakeBand> allBands = new ArrayList<>();
        allBands.addAll(dst.values());
        allBands.addAll(src.values());

        List<PancakeBand> srcMs = new ArrayList<>();
        List<PancakeBand> dstMs = new ArrayList<>();
        PancakeBand pa = src.get(Spectrum.PA);

        for (Spectrum spect : Spectrum.RGB()) {
            srcMs.add(src.get(spect));
            dstMs.add(dst.get(spect));
        }

        int blockXSize = 0;
        int blockYSize = 0;
        for (PancakeBand band : allBands) {
            blockXSize = Integer.max(blockXSize, band.getBlockXSize());
            blockYSize = Integer.max(blockYSize, band.getBlockYSize());
        }
        int blocksize = blockXSize * blockYSize;

        int xsize = pa.getXSize();
        int ysize = pa.getYSize();
        int blockY0 = 0;
        int blockX0 = 0;
        int blockY1 = (ysize + blockYSize - 1) / blockYSize;
        int blockX1 = (xsize + blockXSize - 1) / blockXSize;

        ByteBuffer panCache = ByteBuffer.allocateDirect(blocksize * Pancake.dtBytes(pa.getRasterDatatype()))
                .order(ByteOrder.nativeOrder());

        int largerDstDtBytes = dstMs.stream().mapToInt(b -> Pancake.dtBytes(b.getRasterDatatype())).max().getAsInt();
        int dstMsCacheSize = blocksize * largerDstDtBytes;
        ByteBuffer dstMsCache = ByteBuffer.allocateDirect(dstMsCacheSize).order(ByteOrder.nativeOrder());

        int largetSrcMsBytes = srcMs.stream().mapToInt(b -> Pancake.dtBytes(b.getRasterDatatype())).max().getAsInt();
        int srcMsCacheSize = blocksize * largetSrcMsBytes;
        ByteBuffer srcMsCache = ByteBuffer.allocateDirect(srcMsCacheSize).order(ByteOrder.nativeOrder());

        Map<Spectrum, Buffer2D> srcMsBufs = new EnumMap<>(Spectrum.class);
        for (Spectrum spect : Spectrum.RGB()) {
            srcMsBufs.put(spect, Buffer2D.arrange(blockXSize, blockYSize, pa.getRasterDatatype()));
        }
        Buffer2D dstMsBuf = Buffer2D.arrange(blockXSize, blockYSize, Pancake.TYPE_FLOAT_64);
        Buffer2D ratio = Buffer2D.arrange(blockXSize, blockYSize, Pancake.TYPE_FLOAT_64);

        Math2D math2d = new Math2D();

        for (int blockY = blockY0; blockY < blockY1; ++blockY) {
            for (int blockX = blockX0; blockX < blockX1; ++blockX) {
                int currBlockXSize = Integer.min(blockXSize, xsize - blockX * blockXSize);
                int currBlockYSize = Integer.min(blockYSize, ysize - blockY * blockYSize);

                cacheBlock(pa, blockXSize, blockYSize, blockX, blockY, panCache);
                Buffer2D paBuf = Buffer2D.wrap(panCache, currBlockXSize, currBlockYSize, pa.getRasterDatatype());

                for (Spectrum spect : Spectrum.RGB()) {
                    cacheBlock(src.get(spect), blockXSize, blockYSize, blockX, blockY, srcMsCache);
                    Buffer2D tmpBuf = Buffer2D.wrap(srcMsCache, currBlockXSize, currBlockYSize,
                            src.get(spect).getRasterDatatype());
                    math2d.convert(tmpBuf, srcMsBufs.get(spect));
                }

                math2d.fill(ratio, 0f);
                for (Buffer2D msBuf : srcMsBufs.values()) {
                    math2d.sum(msBuf, ratio, ratio);
                }
                Buffer2D zeros = math2d.compareEquals(ratio, 0f);
                math2d.div(paBuf, ratio, ratio);
                math2d.fill(ratio, 0f, zeros);

                for (Spectrum spect : Spectrum.RGB()) {
                    math2d.mul(ratio, srcMsBufs.get(spect), dstMsBuf);
                    math2d.convertAndScale(dstMsBuf, dst.get(spect).getRasterDatatype(),
                            Buffer2D.wrap(dstMsCache, currBlockXSize, currBlockYSize,
                                    dst.get(spect).getRasterDatatype()),
                            0, Pancake.dtMax(paBuf.datatype()), 0, Pancake.dtMax(dst.get(spect).getRasterDatatype()));
                    flushBlock(dst.get(spect), blockXSize, blockYSize, blockX, blockY, dstMsCache);
                }
            }
        }
    }

    private void _fuse(Map<Spectrum, ? extends PancakeBand> dst, Map<Spectrum, ? extends PancakeBand> src,
            Rectangle region) {
        List<PancakeBand> allBands = new ArrayList<>();
        allBands.addAll(dst.values());
        allBands.addAll(src.values());

        boolean useIntegerImplementaion = true;
        for (var band : allBands) {
            if (!Pancake.isInt(band.getRasterDatatype())) {
                useIntegerImplementaion = false;
                break;
            }
        }

        if (useIntegerImplementaion) {
            _fuseInt(dst, src, region);
        } else {
            throw new UnsupportedOperationException("images with floating point data types not supported");
        }
    }

    @Override
    public void fuse(Map<Spectrum, ? extends PancakeBand> dst, Map<Spectrum, ? extends PancakeBand> src,
            Rectangle region) {
        validateArgs(dst, src);
        _fuse(dst, src, region);
    }

    @Override
    public void fuse(Map<Spectrum, ? extends PancakeBand> dst, Map<Spectrum, ? extends PancakeBand> src) {
        validateArgs(dst, src);
        _fuse(dst, src, new Rectangle(0, 0, src.get(Spectrum.PA).getXSize(), src.get(Spectrum.PA).getYSize()));
    }
}
