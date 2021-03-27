package com.nocmok.pancake.utils;

import java.util.HashMap;
import java.util.Map;

import com.nocmok.pancake.Pancake;
import com.nocmok.pancake.PancakeBand;

/**
 * Limitations: datatypes under UInt32 (exclusive) only integer datatypes only
 * unsigned datatypes
 */
public class HistogramMatching {

    public static abstract class Histogram {

        /**
         * 
         * @param dtype band samples data type
         */
        private static Histogram arrange(int size) {
            if (useHashmap(size)) {
                return new HistogramMap(size);
            } else {
                return new HistogramArray(size);
            }
        }

        private static Histogram forDataType(int dtype){
            int size = (int) Math.pow(256, Pancake.getDatatypeSizeBytes(dtype));
            return Histogram.arrange(size);
        }

        private static boolean useHashmap(int variance) {
            int varianceThreshold = 65536;
            return variance > varianceThreshold;
        }

        public abstract void setScale(double scale);

        /**
         * 
         * @param sample sample value
         * @return how much samples equals to specified sample value
         */
        public abstract int get(int sample);

        protected abstract void set(int sample, int value);

        protected abstract void add(int sample, int value);

        public abstract int size();
    }

    static class HistogramArray extends Histogram {

        private int[] hist;

        private double scale;

        HistogramArray(int size) {
            hist = new int[size];
            scale = 1f;
        }

        @Override
        public int get(int sample) {
            return (int) (scale * hist[sample]);
        }

        @Override
        protected void set(int sample, int value) {
            hist[sample] = value;
        }

        @Override
        protected void add(int sample, int value) {
            hist[sample] += value;
        }

        @Override
        public int size() {
            return hist.length;
        }

        @Override
        public void setScale(double scale) {
            this.scale = scale;
        }

    }

    static class HistogramMap extends Histogram {

        private Map<Integer, Integer> hist;

        private int size;

        private double scale;

        HistogramMap(int size) {
            this.size = size;
            hist = new HashMap<>();
            scale = 1f;
        }

        @Override
        public int get(int sample) {
            return (int) (scale * hist.getOrDefault(sample, 0));
        }

        @Override
        protected void set(int sample, int value) {
            hist.put(sample, value);
        }

        @Override
        protected void add(int sample, int value) {
            hist.put(sample, hist.getOrDefault(sample, 0) + 1);
        }

        @Override
        public int size() {
            return size;
        }

        @Override
        public void setScale(double scale) {
            this.scale = scale;
        }
    }

    private Histogram getLookupTable(Histogram srcHist, Histogram refHist) {
        Histogram lookup = Histogram.arrange(refHist.size());

        int srcIntensity = 0;
        int refIntensity = 0;

        int srcCumSum = 0;
        int refCumSum = refHist.get(0);

        for (; srcIntensity < srcHist.size(); ++srcIntensity) {
            srcCumSum += srcHist.get(srcIntensity);

            while (refCumSum < srcCumSum) {
                if (refIntensity >= refHist.size() - 1) {
                    break;
                }
                refIntensity++;
                refCumSum += refHist.get(refIntensity);
            }

            lookup.set(srcIntensity, refIntensity);
        }

        return lookup;
    }

    public Histogram getHistogram(PancakeBand band, int dtype) {
        Histogram hist = Histogram.forDataType(dtype);
        IntBufferedBand wrapper = new IntBufferedBand(band, dtype);
        _getHistogram(wrapper, hist);
        return hist;
    }

    private Histogram _getHistogram(IntBufferedBand wrapper, Histogram hist){
        for (int blockY = 0; blockY < wrapper.getBlocksInCol(); ++blockY) {
            for (int blockX = 0; blockX < wrapper.getBlocksInRow(); ++blockX) {
                wrapper.cacheBlock(blockX, blockY);
                int blocksize = wrapper.blockXSize(blockX) * wrapper.blockYSize(blockY);
                for (int i = 0; i < blocksize; ++i) {
                    int sample = (int) wrapper.get(i);
                    hist.add(sample, 1);
                }
            }
        }
        return hist;
    }

    public Histogram getHistogram(PancakeBand band) {
        return getHistogram(band, band.getRasterDatatype());
    }

    private void applyLookupTable(PancakeBand band, Histogram lookup) {
        IntBufferedBand wrapper = new IntBufferedBand(band);
        _applyLookupTable(wrapper, lookup);
    }

    private void _applyLookupTable(IntBufferedBand wrapper, Histogram lookup){
        for (int blockY = 0; blockY < wrapper.getBlocksInCol(); ++blockY) {
            for (int blockX = 0; blockX < wrapper.getBlocksInRow(); ++blockX) {
                int blocksize = wrapper.blockXSize(blockX) * wrapper.blockYSize(blockY);
                wrapper.cacheBlock(blockX, blockY);
                for (int i = 0; i < blocksize; ++i) {
                    wrapper.set(i, lookup.get((int) wrapper.get(i)));
                }
            }
        }
        wrapper.flushCache();
    }

    public void matchHistogram(PancakeBand band, Histogram hist) {
        IntBufferedBand wrapper = new IntBufferedBand(band);
        Histogram bandHist = Histogram.forDataType(band.getRasterDatatype());
        _getHistogram(wrapper, bandHist);
        Histogram lookup = getLookupTable(bandHist, hist);
        _applyLookupTable(wrapper, lookup);
    }

    public void matchHistogram(PancakeBand src, PancakeBand ref) {
        int srcSize = src.getXSize() * src.getYSize();
        int refSize = ref.getXSize() * ref.getYSize();

        IntBufferedBand srcWrapper = new IntBufferedBand(src);
        IntBufferedBand refWrapper = new IntBufferedBand(ref, src.getRasterDatatype());

        Histogram srcHist = Histogram.forDataType(src.getRasterDatatype());
        Histogram refHist = Histogram.forDataType(src.getRasterDatatype());

        _getHistogram(srcWrapper, srcHist); 
        _getHistogram(refWrapper, refHist);

        if (srcSize > refSize) {
            double scale = (double) srcSize / refSize;
            refHist.setScale(scale);
        } else if (srcSize < refSize) {
            double scale = (double) refSize / srcSize;
            srcHist.setScale(scale);
        }

        Histogram lookup = getLookupTable(srcHist, refHist);
        _applyLookupTable(srcWrapper, lookup);
    }
}
