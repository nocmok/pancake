package com.nocmok.pancake.fusor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.nocmok.pancake.Spectrum;
import com.nocmok.pancake.utils.Rectangle;

public class BroveyFusor implements Fusor {

    /** weight of red band */
    double rWeight = 1.0;

    /** weight of green band */
    double gWeight = 1.0;

    /** weight of blue band */
    double bWeight = 1.0;

    public BroveyFusor(double rWeight, double gWeight, double bWeight) {
        double max = Double.max(rWeight, Double.max(gWeight, bWeight));
        this.rWeight = rWeight / max;
        this.gWeight = gWeight / max;
        this.bWeight = gWeight / max;
    }

    public BroveyFusor() {
        this(1.0, 1.0, 1.0);
    }

    private void validateDst(Map<Spectrum, PancakeBand> dst) {
        List<Spectrum> dstRequiredBands = List.of(Spectrum.R, Spectrum.G, Spectrum.B);
        for (Spectrum spect : dstRequiredBands) {
            if (!dst.containsKey(spect)) {
                throw new RuntimeException("missed " + spect.toString() + " band");
            }
        }
    }

    private void validateSrc(Map<Spectrum, PancakeBand> src) {
        List<Spectrum> srcRequiredBands = List.of(Spectrum.R, Spectrum.G, Spectrum.B, Spectrum.PA);
        for (Spectrum spect : srcRequiredBands) {
            if (!src.containsKey(spect)) {
                throw new RuntimeException("missed " + spect.toString() + " band");
            }
        }
    }

    private void validateArgs(Map<Spectrum, PancakeBand> dst, Map<Spectrum, PancakeBand> src) {
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

    @Override
    public void fuse(Map<Spectrum, PancakeBand> dst, Map<Spectrum, PancakeBand> src, Rectangle region) {
        validateArgs(dst, src);
        _fuse(dst, src, region);
    }

    private void _fuse(Map<Spectrum, PancakeBand> dst, Map<Spectrum, PancakeBand> src, Rectangle region) {
        var r = dst.get(Spectrum.R);
        var g = dst.get(Spectrum.G);
        var b = dst.get(Spectrum.B);

        var r0 = src.get(Spectrum.R);
        var g0 = src.get(Spectrum.G);
        var b0 = src.get(Spectrum.B);
        var pa = src.get(Spectrum.PA);

        int blockXSize = r0.getBlockXSize();
        int blockYSize = r0.getBlockYSize();

        int x0 = region.x0();
        int y0 = region.y0();

        for (int yBlock = r0.toBlockY(region.y0()); yBlock <= r0.toBlockY(region.y1()); ++yBlock) {
            for (int xBlock = r0.toBlockX(region.x0()); xBlock <= r0.toBlockX(region.x1()); ++xBlock) {
                for (int y = y0; y < Integer.min(y0 + blockYSize, region.y1()); ++y) {
                    for (int x = x0; x < Integer.min(y0 + blockXSize, region.x1()); ++x) {
                        double pseudoPanchro = r0.get(x, y) + g0.get(x, y) + b0.get(x, y);
                        if (pseudoPanchro == 0.0) {
                            r.set(x, y, 0.0);
                            g.set(x, y, 0.0);
                            b.set(x, y, 0.0);
                        } else {
                            double correction = pa.get(x, y) / pseudoPanchro;
                            r.set(x, y, r0.get(x, y) * rWeight * correction);
                            g.set(x, y, g0.get(x, y) * gWeight * correction);
                            b.set(x, y, b0.get(x, y) * bWeight * correction);
                        }
                    }
                }
                x0 += blockXSize;
            }
            x0 = region.x0();
            y0 += blockYSize;
        }

        r.flushCache();
        g.flushCache();
        b.flushCache();
    }

    @Override
    public void fuse(Map<Spectrum, PancakeBand> dst, Map<Spectrum, PancakeBand> src) {
        validateArgs(dst, src);
        _fuse(dst, src, new Rectangle(0, 0, src.get(Spectrum.PA).getXSize(), src.get(Spectrum.PA).getYSize()));
    }
}
