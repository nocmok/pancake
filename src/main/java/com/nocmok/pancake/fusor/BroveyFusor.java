package com.nocmok.pancake.fusor;

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

    @Override
    public void fuse(Map<Spectrum, ? extends Band> dst, Map<Spectrum, ? extends Band> src, Rectangle area) {
        NormalizedBand r = new NormalizedBand(dst.get(Spectrum.R));
        NormalizedBand g = new NormalizedBand(dst.get(Spectrum.G));
        NormalizedBand b = new NormalizedBand(dst.get(Spectrum.B));

        NormalizedBand r0 = new NormalizedBand(src.get(Spectrum.R));
        NormalizedBand g0 = new NormalizedBand(src.get(Spectrum.G));
        NormalizedBand b0 = new NormalizedBand(src.get(Spectrum.B));
        NormalizedBand pa = new NormalizedBand(src.get(Spectrum.PA));

        int blockXSize = r0.getBlockXSize();
        int blockYSize = r0.getBlockYSize();

        int x0 = area.x0();
        int y0 = area.y0();

        for (int yBlock = r0.toBlockY(area.y0()); yBlock <= r0.toBlockY(area.y1()); ++yBlock) {
            for (int xBlock = r0.toBlockX(area.x0()); xBlock <= r0.toBlockX(area.x1()); ++xBlock) {
                for (int y = y0; y < Integer.min(y0 + blockYSize, area.y1()); ++y) {
                    for (int x = x0; x < Integer.min(y0 + blockXSize, area.x1()); ++x) {
                        double pseudoPanchro = r0.get(x, y) + g0.get(x, y) + b0.get(x, y);
                        double correction = pa.get(x, y) / pseudoPanchro;
                        r.set(x, y, r0.get(x, y) * rWeight * correction);
                        g.set(x, y, g0.get(x, y) * gWeight * correction);
                        b.set(x, y, b0.get(x, y) * bWeight * correction);
                    }
                }
                x0 += blockXSize;
            }
            x0 = area.x0();
            y0 += blockYSize;
        }

        r.flushCache();
        g.flushCache();
        b.flushCache();
    }

    @Override
    public void fuse(Map<Spectrum, ? extends Band> dst, Map<Spectrum, ? extends Band> src) {
        fuse(dst, src, new Rectangle(0, 0, src.get(Spectrum.PA).getXSize(), src.get(Spectrum.PA).getYSize()));
    }
}
