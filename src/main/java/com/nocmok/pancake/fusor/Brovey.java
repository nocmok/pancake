package com.nocmok.pancake.fusor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.nocmok.pancake.Pancake;
import com.nocmok.pancake.Spectrum;
import com.nocmok.pancake.utils.Rectangle;
import com.nocmok.pancake.utils.IntBufferedBand;
import com.nocmok.pancake.utils.NormalizedBufferedBand;
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

    private void _fuseFloat(Map<Spectrum, NormalizedBufferedBand> dst, Map<Spectrum, NormalizedBufferedBand> src,
            Rectangle region) {
        List<NormalizedBufferedBand> allBands = new ArrayList<>();
        allBands.addAll(dst.values());
        allBands.addAll(src.values());

        var r = dst.get(Spectrum.R);
        var g = dst.get(Spectrum.G);
        var b = dst.get(Spectrum.B);

        var r0 = src.get(Spectrum.R);
        var g0 = src.get(Spectrum.G);
        var b0 = src.get(Spectrum.B);
        var pa = src.get(Spectrum.PA);

        for (int yBlock = r0.toBlockY(region.y0()); yBlock < r0.toBlockY(region.y1()); ++yBlock) {
            for (int xBlock = r0.toBlockX(region.x0()); xBlock < r0.toBlockX(region.x1()); ++xBlock) {

                int blockX = xBlock;
                int blockY = yBlock;

                allBands.forEach(band -> band.cacheBlock(blockX, blockY));
                int blockSize = pa.blockXSize(xBlock) * pa.blockYSize(yBlock);

                for (int i = 0; i < blockSize; ++i) {
                    double pseudoPanchro = r0.get(i) + g0.get(i) + b0.get(i);
                    if (pseudoPanchro == 0.0) {
                        continue;
                    } else {
                        double correction = pa.get(i) / pseudoPanchro;
                        r.set(i, r0.get(i) * rWeight * correction);
                        g.set(i, g0.get(i) * gWeight * correction);
                        b.set(i, b0.get(i) * bWeight * correction);
                    }
                }
            }
        }

        r.flushCache();
        g.flushCache();
        b.flushCache();
    }

    private void _fuseInt(Map<Spectrum, IntBufferedBand> dst, Map<Spectrum, IntBufferedBand> src, Rectangle region) {
        List<IntBufferedBand> allBands = new ArrayList<>();
        allBands.addAll(dst.values());
        allBands.addAll(src.values());

        var r = dst.get(Spectrum.R);
        var g = dst.get(Spectrum.G);
        var b = dst.get(Spectrum.B);

        var r0 = src.get(Spectrum.R);
        var g0 = src.get(Spectrum.G);
        var b0 = src.get(Spectrum.B);
        var pa = src.get(Spectrum.PA);

        long maxValue = pa.getAbsoluteMaxValue();
        long rFactor = (long) (maxValue * rWeight);
        long gFactor = (long) (maxValue * gWeight);
        long bFactor = (long) (maxValue * bWeight);

        for (int yBlock = r0.toBlockY(region.y0()); yBlock < r0.toBlockY(region.y1()); ++yBlock) {
            for (int xBlock = r0.toBlockX(region.x0()); xBlock < r0.toBlockX(region.x1()); ++xBlock) {

                int blockX = xBlock;
                int blockY = yBlock;

                allBands.forEach(band -> band.cacheBlock(blockX, blockY));
                int blockSize = pa.blockXSize(xBlock) * pa.blockYSize(yBlock);

                for (int i = 0; i < blockSize; ++i) {
                    long pseudoPanchro = r0.get(i) + g0.get(i) + b0.get(i);
                    if (pseudoPanchro == 0) {
                        continue;
                    }
                    r.set(i, r0.get(i) * pa.get(i) / pseudoPanchro * rFactor / maxValue);
                    g.set(i, g0.get(i) * pa.get(i) / pseudoPanchro * gFactor / maxValue);
                    b.set(i, b0.get(i) * pa.get(i) / pseudoPanchro * bFactor / maxValue);
                }
            }
        }

        r.flushCache();
        g.flushCache();
        b.flushCache();
    }

    private void _fuse(Map<Spectrum, ? extends PancakeBand> dst, Map<Spectrum, ? extends PancakeBand> src,
            Rectangle region) {

        List<PancakeBand> allBands = new ArrayList<>();
        allBands.addAll(dst.values());
        allBands.addAll(src.values());

        boolean useIntegerImplementaion = true;
        for (var band : allBands) {
            if (!Pancake.isIntegerDatatype(band.getRasterDatatype())) {
                useIntegerImplementaion = false;
                break;
            }
        }

        if (useIntegerImplementaion) {
            List<Integer> allDatatypes = new ArrayList<>();
            allBands.forEach(band -> allDatatypes.add(band.getRasterDatatype()));
            int commonDt = Pancake.getBiggestDatatype(allDatatypes);

            Map<Spectrum, IntBufferedBand> dstInt = new HashMap<>();
            Map<Spectrum, IntBufferedBand> srcInt = new HashMap<>();

            dst.forEach((spect, band) -> dstInt.put(spect, new IntBufferedBand(band, commonDt)));
            src.forEach((spect, band) -> srcInt.put(spect, new IntBufferedBand(band, commonDt)));

            _fuseInt(dstInt, srcInt, region);
        } else {
            Map<Spectrum, NormalizedBufferedBand> dstNorm = new HashMap<>();
            Map<Spectrum, NormalizedBufferedBand> srcNorm = new HashMap<>();

            dst.forEach((spect, band) -> dstNorm.put(spect, new NormalizedBufferedBand(band)));
            src.forEach((spect, band) -> srcNorm.put(spect, new NormalizedBufferedBand(band)));

            _fuseFloat(dstNorm, srcNorm, region);
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
