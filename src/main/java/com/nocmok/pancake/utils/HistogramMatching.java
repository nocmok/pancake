package com.nocmok.pancake.utils;

import java.util.HashMap;
import java.util.Map;

import com.nocmok.pancake.Pancake;
import com.nocmok.pancake.PancakeBand;

/** Limitations:
 * datatypes under UInt32 (exclusive)
 * only integer datatypes
 * only unsigned datatypes
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

        private static boolean useHashmap(int variance) {
            int varianceThreshold = 65536;
            return variance > varianceThreshold;
        }

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

        HistogramArray(int size) {
            hist = new int[size];
        }

        @Override
        public int get(int sample) {
            return hist[sample];
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

        
    }

    static class HistogramMap extends Histogram {

        private Map<Integer, Integer> hist;

        private int size;

        HistogramMap(int size){
            this.size = size;
            hist = new HashMap<>();
        }

        @Override
        public int get(int sample) {
            return hist.getOrDefault(sample, 0);
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
    }

    private Histogram getLookupTable(Histogram srcHist, Histogram refHist) {
        Histogram lookup = Histogram.arrange(refHist.size());

        int srcIntensity = 0;
        int refIntensity = 0;

        int srcCumSum = 0;
        int refCumSum = refHist.get(0);

        for(; srcIntensity < srcHist.size(); ++srcIntensity){
            srcCumSum += srcHist.get(srcIntensity);
            
            while(refCumSum < srcCumSum){
                if(refIntensity >= refHist.size() - 1){
                    break;
                }
                refIntensity ++;
                refCumSum += refHist.get(refIntensity);
            }
            
            lookup.set(srcIntensity, refIntensity);
        }

        return lookup;
    }

    public Histogram getHistogram(PancakeBand band, int dtype) {
        int size = (int)Math.pow(256, Pancake.getDatatypeSizeBytes(dtype));
        Histogram hist = Histogram.arrange(size);
        IntBufferedBand wrapper = new IntBufferedBand(band, dtype);
        for(int blockY = 0; blockY < wrapper.getBlocksInCol(); ++blockY){
            for(int blockX = 0; blockX < wrapper.getBlocksInRow(); ++blockX){
                wrapper.cacheBlock(blockX, blockY);
                int blocksize = wrapper.blockXSize(blockX) * wrapper.blockYSize(blockY);
                for(int i = 0; i < blocksize; ++i){
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

    public void matchHistogram(PancakeBand band, Histogram hist) {
        Histogram bandHist = getHistogram(band);
        Histogram lookup = getLookupTable(bandHist, hist);

        IntBufferedBand wrapper = new IntBufferedBand(band);
        for(int blockY = 0; blockY < wrapper.getBlocksInCol(); ++blockY){
            for(int blockX = 0; blockX < wrapper.getBlocksInRow(); ++blockX){
                int blocksize = wrapper.blockXSize(blockX) * wrapper.blockYSize(blockY);
                wrapper.cacheBlock(blockX, blockY);
                for(int i = 0; i < blocksize; ++i){
                    wrapper.set(i, lookup.get((int)wrapper.get(i)));
                }
            }
        }
        wrapper.flushCache();
    }

    public void matchHistogram(PancakeBand src, PancakeBand ref) {
        matchHistogram(src, getHistogram(ref, src.getRasterDatatype()));
    }
}