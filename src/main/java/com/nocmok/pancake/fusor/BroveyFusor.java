package com.nocmok.pancake.fusor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.nocmok.pancake.Spectrum;
import com.nocmok.pancake.utils.Rectangle;

import org.gdal.gdal.Band;

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

    private void validateDst(Map<Spectrum, ? extends Band> dst) {
        List<Spectrum> dstRequiredBands = List.of(Spectrum.R, Spectrum.G, Spectrum.B);
        for (Spectrum spect : dstRequiredBands) {
            if (!dst.containsKey(spect)) {
                throw new RuntimeException("missed " + spect.toString() + " band");
            }
        }
    }

    private void validateSrc(Map<Spectrum, ? extends Band> src) {
        List<Spectrum> srcRequiredBands = List.of(Spectrum.R, Spectrum.G, Spectrum.B, Spectrum.PA);
        for (Spectrum spect : srcRequiredBands) {
            if (!src.containsKey(spect)) {
                throw new RuntimeException("missed " + spect.toString() + " band");
            }
        }
    }

    private void validateArgs(Map<Spectrum, ? extends Band> dst, Map<Spectrum, ? extends Band> src) {
        validateDst(dst);
        validateSrc(src);
        int paXSize = src.get(Spectrum.PA).getXSize();
        int paYSize = src.get(Spectrum.PA).getYSize();
        List<Band> allBands = new ArrayList<>();
        allBands.addAll(dst.values());
        allBands.addAll(src.values());
        for (Band band : allBands) {
            if ((band.getXSize() != paXSize) || (band.getYSize() != paYSize)) {
                throw new RuntimeException("one of provided band mismatch panchromatic band resolution");
            }
        }
    }

    @Override
    public void fuse(Map<Spectrum, ? extends Band> dst, Map<Spectrum, ? extends Band> src, Rectangle region) {
        validateArgs(dst, src);
        _fuse(dst, src, region);
    }

    private void _fuse(Map<Spectrum, ? extends Band> dst, Map<Spectrum, ? extends Band> src, Rectangle region) {
        NormalizedBand r = new NormalizedBand(dst.get(Spectrum.R));
        NormalizedBand g = new NormalizedBand(dst.get(Spectrum.G));
        NormalizedBand b = new NormalizedBand(dst.get(Spectrum.B));

        NormalizedBand r0 = new NormalizedBand(src.get(Spectrum.R));
        NormalizedBand g0 = new NormalizedBand(src.get(Spectrum.G));
        NormalizedBand b0 = new NormalizedBand(src.get(Spectrum.B));
        NormalizedBand pa = new NormalizedBand(src.get(Spectrum.PA));

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
    public void fuse(Map<Spectrum, ? extends Band> dst, Map<Spectrum, ? extends Band> src) {
        validateArgs(dst, src);
        _fuse(dst, src, new Rectangle(0, 0, src.get(Spectrum.PA).getXSize(), src.get(Spectrum.PA).getYSize()));
    }
}
