package com.nocmok.pancake.utils;

import java.util.Optional;

import com.nocmok.pancake.Pancake;
import com.nocmok.pancake.PancakeBand;
import com.nocmok.pancake.PancakeConstants;
import com.nocmok.pancake.PancakeProgressListener;

/**
 * Limitations: datatypes under UInt32 (exclusive) only integer datatypes only
 * unsigned datatypes
 */
public class HistogramMatching {

    private PancakeProgressListener listener = PancakeProgressListener.empty;

    public void setProgressListener(PancakeProgressListener listener) {
        this.listener = Optional.ofNullable(listener).orElse(PancakeProgressListener.empty);
    }

    public static abstract class Histogram {

        private static Histogram forDataType(int dtype) {
            if (useArray(dtype)) {
                // return new HistogramMap(dtype);
                return new HistogramArray(dtype);
            } else {
                throw new UnsupportedOperationException("histogram for datatypes > 16 bit size not implemented");
            }
        }

        private static boolean useArray(int dtype) {
            int sizeThreshold = 65536;
            int size = (int) Math.pow(256, Pancake.dtBytes(dtype));
            return size <= sizeThreshold;
        }

        public abstract void setScale(double scale);

        /**
         * 
         * @param sample sample value
         * @return how much samples equals to specified sample value
         */
        public abstract int get(long sample);

        protected abstract void set(long sample, int value);

        protected abstract void add(long sample, int value);

        public abstract int size();

        public abstract long minVal();

        public abstract long maxVal();

        public abstract int datatype();
    }

    static class HistogramArray extends Histogram {

        private int dtype;

        private int minVal;

        private int maxVal;

        private int[] hist;

        private double scale;

        HistogramArray(int dtype) {
            this.dtype = dtype;
            this.minVal = (int) Pancake.dtMin(dtype);
            this.maxVal = (int) Pancake.dtMax(dtype);
            int size = (int) Math.pow(256, Pancake.dtBytes(dtype));
            hist = new int[size];
            scale = 1f;
        }

        @Override
        public int get(long sample) {
            return (int) (scale * hist[(int) sample - minVal]);
        }

        @Override
        protected void set(long sample, int value) {
            hist[(int) sample - minVal] = value;
        }

        @Override
        protected void add(long sample, int value) {
            hist[(int) sample - minVal] += value;
        }

        @Override
        public int size() {
            return hist.length;
        }

        @Override
        public void setScale(double scale) {
            this.scale = scale;
        }

        @Override
        public long minVal() {
            return minVal;
        }

        @Override
        public long maxVal() {
            return maxVal;
        }

        @Override
        public int datatype() {
            return dtype;
        }

    }

    static abstract class LookupTable {

        public abstract long get(long sample);

        protected abstract void set(long sample, long value);

        public abstract long minVal();

        public abstract long maxVal();

        public abstract int datatype();

        private static boolean useArray(int dtype) {
            int sizeThreshold = 65536;
            int size = (int) Math.pow(256, Pancake.dtBytes(dtype));
            return size <= sizeThreshold;
        }

        private static LookupTable forDatatype(int dtype) {
            if (useArray(dtype)) {
                return new LookupArray(dtype);
            } else {
                throw new UnsupportedOperationException("histogram for datatypes > 16 bit size not implemented");
                // return new LookupMap(dtype);
            }
        }

    }

    /** Up to Int16 / UInt16 */
    static class LookupArray extends LookupTable {

        private int dtype;

        private int minVal;

        private int maxVal;

        private int[] lookup;

        LookupArray(int dtype) {
            this.dtype = dtype;
            this.minVal = (int) Pancake.dtMin(dtype);
            this.maxVal = (int) Pancake.dtMax(dtype);
            int size = (int) Math.pow(256, Pancake.dtBytes(dtype));
            lookup = new int[size];
        }

        @Override
        public long get(long sample) {
            return lookup[(int) sample - minVal];
        }

        @Override
        public void set(long sample, long value) {
            lookup[(int) sample - minVal] = (int) value;
        }

        @Override
        public long minVal() {
            return minVal;
        }

        @Override
        public long maxVal() {
            return maxVal;
        }

        @Override
        public int datatype() {
            return dtype;
        }
    }

    private LookupTable getLookupTable(Histogram srcHist, Histogram refHist) {
        LookupTable lookup = LookupTable.forDatatype(srcHist.datatype());

        long srcIntensity = srcHist.minVal();
        long refIntensity = refHist.minVal();

        int srcCumSum = 0;
        int refCumSum = refHist.get(refIntensity);

        for (; srcIntensity <= srcHist.maxVal(); ++srcIntensity) {
            srcCumSum += srcHist.get(srcIntensity);

            while (refCumSum < srcCumSum) {
                if (refIntensity >= refHist.maxVal()) {
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
        if (Pancake.dtBytes(band.getRasterDatatype()) > 2) {
            throw new UnsupportedOperationException(
                    "histogram matching for images with data type > 16 bit not implemented");
        }
        Histogram hist = Histogram.forDataType(dtype);
        BandIntTileReader wrapper = new BandIntTileReader(band, dtype);
        _getHistogram(wrapper, hist);
        return hist;
    }

    private Histogram _getHistogram(BandIntTileReader wrapper, Histogram hist) {
        int totalBlocks = wrapper.getBlocksInCol() * wrapper.getBlocksInRow();
        int stepSize = (totalBlocks + Pancake.logsFrequency() - 1) / Pancake.logsFrequency();
        int stepsTotal = (totalBlocks + stepSize - 1) / stepSize;
        int nBlock = 0;

        for (int blockY = 0; blockY < wrapper.getBlocksInCol(); ++blockY) {
            for (int blockX = 0; blockX < wrapper.getBlocksInRow(); ++blockX) {
                wrapper.cacheBlock(blockX, blockY);
                int blocksize = wrapper.blockXSize(blockX) * wrapper.blockYSize(blockY);
                for (int i = 0; i < blocksize; ++i) {
                    int sample = (int) wrapper.get(i);
                    hist.add(sample, 1);
                }

                if (((nBlock + 1) % stepSize == 0) || (nBlock + 1 >= totalBlocks)) {
                    double progress = (nBlock / stepSize + 1) / (double) stepsTotal;
                    listener.listen(PancakeConstants.PROGRESS_HIST_MATCHING, progress,
                            "[Pancake] getting histogram for: " + wrapper.getUnderlyingBand().dataset().path());
                }
                ++nBlock;
            }
        }
        return hist;
    }

    public Histogram getHistogram(PancakeBand band) {
        return getHistogram(band, band.getRasterDatatype());
    }

    private void _applyLookupTable(BandIntTileReader wrapper, LookupTable lookup) {
        int totalBlocks = wrapper.getBlocksInCol() * wrapper.getBlocksInRow();
        int stepSize = (totalBlocks + Pancake.logsFrequency() - 1) / Pancake.logsFrequency();
        int stepsTotal = (totalBlocks + stepSize - 1) / stepSize;
        int nBlock = 0;

        for (int blockY = 0; blockY < wrapper.getBlocksInCol(); ++blockY) {
            for (int blockX = 0; blockX < wrapper.getBlocksInRow(); ++blockX) {
                int blocksize = wrapper.blockXSize(blockX) * wrapper.blockYSize(blockY);
                wrapper.cacheBlock(blockX, blockY);
                for (int i = 0; i < blocksize; ++i) {
                    wrapper.set(i, lookup.get((int) wrapper.get(i)));
                }

                if (((nBlock + 1) % stepSize == 0) || (nBlock + 1 >= totalBlocks)) {
                    double progress = (nBlock / stepSize + 1) / (double) stepsTotal;
                    listener.listen(PancakeConstants.PROGRESS_HIST_MATCHING, progress,
                            "[Pancake] applying lookup table for: " + wrapper.getUnderlyingBand().dataset().path());
                }
                ++nBlock;
            }
        }
        wrapper.flushCache();
    }

    public void matchHistogram(PancakeBand band, Histogram hist) {
        if (Pancake.dtBytes(band.getRasterDatatype()) > 2) {
            throw new UnsupportedOperationException(
                    "histogram matching for images with data type > 16 bit not implemented");
        }

        BandIntTileReader wrapper = new BandIntTileReader(band);
        Histogram bandHist = Histogram.forDataType(band.getRasterDatatype());
        _getHistogram(wrapper, bandHist);
        LookupTable lookup = getLookupTable(bandHist, hist);
        _applyLookupTable(wrapper, lookup);
    }

    private Shape computeBlockSize(Shape nativeBlocksize, Shape imageSize, int dtype) {
        // int blockXSize = Integer.min(imageSize.xsize(), nativeBlocksize.xsize());
        // int blockYSize = Integer.min(imageSize.ysize(), nativeBlocksize.ysize());
        // int bytes = Pancake.dtBytes(dtype);
        // int blockSize = bytes * blockXSize * blockYSize;

        // /** 512 kb */
        // int prefferedBlockSize = 512 * 1024;
        // /** 1 mb */
        // int maxBlockSize = 1024 * 1024;

        // if (blockSize >= prefferedBlockSize) {
        // return Shape.of(blockXSize, blockYSize);
        // }

        // int grow = (prefferedBlockSize + blockSize - 1) / blockSize;
        // if (grow * blockSize > maxBlockSize) {
        // grow -= 1;
        // }

        // if (blockXSize < imageSize.xsize()) {
        // if (grow * blockXSize >= imageSize.xsize()) {
        // return computeBlockSize(Shape.of(imageSize.xsize(), blockYSize), imageSize,
        // dtype);
        // } else {
        // List<Integer> sizes = List.of(128, 256, 512);
        // int bestSize = 512;
        // for (Integer size : sizes) {
        // if (size * size * bytes >= prefferedBlockSize) {
        // bestSize = size;
        // break;
        // }
        // }
        // blockXSize = bestSize;
        // blockYSize = bestSize;
        // }
        // } else if (blockXSize == imageSize.xsize()) {
        // blockYSize *= grow;
        // }

        // return Shape.of(blockXSize, blockYSize);

        return nativeBlocksize;
    }

    public void matchHistogram(PancakeBand src, PancakeBand ref) {
        if (Pancake.dtBytes(src.getRasterDatatype()) > 2) {
            throw new UnsupportedOperationException(
                    "histogram matching for images with data type > 16 bit not implemented");
        }

        int srcSize = src.getXSize() * src.getYSize();
        int refSize = ref.getXSize() * ref.getYSize();

        Shape srcBlockSize = computeBlockSize(Shape.of(src.getBlockXSize(), src.getBlockYSize()),
                Shape.of(src.getXSize(), src.getYSize()), src.getRasterDatatype());
        Shape dstBlockSize = computeBlockSize(Shape.of(src.getBlockXSize(), src.getBlockYSize()),
                Shape.of(src.getXSize(), src.getYSize()), src.getRasterDatatype());

        BandIntTileReader srcWrapper = new BandIntTileReader(src, srcBlockSize.xsize(), srcBlockSize.ysize());
        BandIntTileReader refWrapper = new BandIntTileReader(ref, dstBlockSize.xsize(), dstBlockSize.ysize(),
                src.getRasterDatatype());

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

        LookupTable lookup = getLookupTable(srcHist, refHist);
        _applyLookupTable(srcWrapper, lookup);
    }
}