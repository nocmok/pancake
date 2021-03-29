package com.nocmok.pancake.utils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.nocmok.pancake.Pancake;
import com.nocmok.pancake.PancakeBand;

/**
 * Limitations: only unsigned integer datatypes if datatype is signed, it will
 * be carried like with unsigned datatype with the same bytes
 */
public class IntBufferedBand {

    private PancakeBand pnkband;

    private int blockXSize;

    private int blockYSize;

    /**
     * In general not all blocks have equal size. Sizes of special case blocks
     * listed in fields {@code lastXBlockByteSize}, {@code lastYBlockByteSize} and
     * {@code lastXYBlockByteSize}
     */
    private int blockByteSize;

    private int blocksInRow;

    private int blocksInCol;

    private ByteBuffer blockCache;

    /**
     * 0 - x coordinate 1 - y coordinate values [-1, -1] mean lack of cached block
     */
    private int[] blockInCache = new int[] { -1, -1 };

    /** Whether block cache was modified */
    private boolean isDirty = false;

    // private int _datatype;

    private int nativeDtBytesSize;

    private long maxValue;

    private long minValue;

    private long maxValueNative;

    private long minValueNative;

    private int dtbits;

    private int nativeDtbits;

    public IntBufferedBand(PancakeBand pnkBand) {
        this(pnkBand, Integer.min(pnkBand.getBlockXSize(), pnkBand.getXSize()),
                Integer.min(pnkBand.getBlockYSize(), pnkBand.getYSize()), pnkBand.getRasterDatatype());
    }

    public IntBufferedBand(PancakeBand pnkBand, int dataType) {
        this(pnkBand, Integer.min(pnkBand.getBlockXSize(), pnkBand.getXSize()),
                Integer.min(pnkBand.getBlockYSize(), pnkBand.getYSize()), dataType);
    }

    /**
     * 
     * @param pnkBand
     * @param blockXSize desired cache block width
     * @param blockYSize desired cache block height
     * @param datatype   target data type in which all values desired to be
     *                   converted
     */
    public IntBufferedBand(PancakeBand pnkBand, int blockXSize, int blockYSize, int datatype) {
        if (!Pancake.isIntegerDatatype(datatype)) {
            throw new UnsupportedOperationException(
                    "expected integer datatype, but " + Pancake.getDatatypeName(datatype) + " was provided");
        }
        if (!Pancake.isIntegerDatatype(pnkBand.getRasterDatatype())) {
            throw new UnsupportedOperationException(
                    "expected integer datatype, but " + Pancake.getDatatypeName(datatype) + " was provided");
        }

        this.pnkband = pnkBand;

        // this._datatype = datatype;
        this.nativeDtBytesSize = Pancake.getDatatypeSizeBytes(pnkband.getRasterDatatype());

        this.dtbits = 8 * Pancake.getDatatypeSizeBytes(datatype);
        this.nativeDtbits = 8 * Pancake.getDatatypeSizeBytes(pnkband.getRasterDatatype());

        this.maxValue = (1 << dtbits) - 1;
        this.minValue = 0;
        this.maxValueNative = (1 << nativeDtbits) - 1;
        this.minValueNative = 0;

        this.blockXSize = blockXSize;
        this.blockYSize = blockYSize;

        this.blocksInCol = (pnkband.getYSize() + blockYSize - 1) / blockYSize;
        this.blocksInRow = (pnkband.getXSize() + blockXSize - 1) / blockXSize;

        this.blockByteSize = blockXSize * blockYSize * Pancake.getDatatypeSizeBytes(pnkband.getRasterDatatype());
        this.blockCache = ByteBuffer.allocateDirect(blockByteSize);
        this.blockCache.order(ByteOrder.nativeOrder());
    }

    /**
     * @return x coordinate of block which contains sample with specified x
     *         coordinate
     */
    public int toBlockX(int x) {
        return x / blockXSize;
    }

    /**
     * @return y coordinate of block which contains sample with specified y
     *         coordinate
     */
    public int toBlockY(int y) {
        return y / blockYSize;
    }

    /**
     * @return x coordinate of first sample, which belongs to block with specified x
     *         coordinate
     */
    public int blockXStart(int blockX) {
        return blockX * blockXSize;
    }

    /**
     * @return y coordinate of first sample, which belongs to block with specified y
     *         coordinate
     */
    public int blockYStart(int blockY) {
        return blockY * blockYSize;
    }

    /**
     * @return width of block with specified x coordinate
     */
    public int blockXSize(int blockX) {
        return (blockX + 1 < blocksInRow) ? (blockXSize) : (pnkband.getXSize() - (blocksInRow - 1) * blockXSize);
    }

    /**
     * @return height of block with specified y coordinate
     */
    public int blockYSize(int blockY) {
        return (blockY + 1 < blocksInCol) ? (blockYSize) : (pnkband.getYSize() - (blocksInCol - 1) * blockYSize);
    }

    /**
     * 
     * @param blockX x coordinate of block
     * @param blockY y coordinate of block
     * @return
     */
    private boolean isCached(int blockX, int blockY) {
        return blockInCache[0] == blockX && blockInCache[1] == blockY;
    }

    private boolean hasBlockInCache() {
        return blockInCache[0] != -1 && blockInCache[1] != -1;
    }

    /**
     * Drops currently cached block to band and read specified block to cache.
     * 
     * @param blockX x coordinate of block to cache
     * @param blockY y coordinate of block to cache
     */
    public void cacheBlock(int blockX, int blockY) {
        flushCache();
        int curBlockXSize = blockXSize(blockX);
        int curBlockYSize = blockYSize(blockY);
        try {
            pnkband.readRasterDirect(blockX * blockXSize, blockY * blockYSize, curBlockXSize, curBlockYSize,
                    curBlockXSize, curBlockYSize, pnkband.getRasterDatatype(), blockCache);
        } catch (RuntimeException e) {
            throw new RuntimeException("failed to cache block", e);
        }
        blockInCache[0] = blockX;
        blockInCache[1] = blockY;
    }

    /** Cache specified block only if this block not already cached */
    private void cacheBlockSoft(int blockX, int blockY) {
        if (!isCached(blockX, blockY)) {
            cacheBlock(blockX, blockY);
        }
    }

    /**
     * @return index of first byte in cached block that contains sample at specified
     *         coordinates
     */
    private int flatIndex(int x, int y) {
        int flatIndex = ((y - blockYStart(toBlockY(y))) * blockXSize + (x - blockXStart(toBlockX(x))))
                * nativeDtBytesSize;
        return flatIndex;
    }

    private long translate(long value) {
        return value * maxValueNative / maxValue;
        // return ((value << nativeDtbits) >>> dtbits);
    }

    private long detranslate(long value) {
        return value * maxValue / maxValueNative;
        // return ((value << dtbits) >>> nativeDtbits);
    }

    /**
     * @return value between 0 and (_maxValue - _minValue), which represents
     *         intensity of sample at x, y coordinates
     */
    public long get(int x, int y) {
        cacheBlockSoft(toBlockX(x), toBlockY(y));
        switch (pnkband.getRasterDatatype()) {
        case Pancake.TYPE_BYTE:
        case Pancake.TYPE_UNKNOWN:
            return detranslate(Byte.toUnsignedInt(blockCache.get(flatIndex(x, y))));
        case Pancake.TYPE_INT_16:
        case Pancake.TYPE_UINT_16:
            return detranslate(Short.toUnsignedInt(blockCache.getShort(flatIndex(x, y))));
        case Pancake.TYPE_INT_32:
        case Pancake.TYPE_UINT_32:
            return detranslate(Integer.toUnsignedLong(blockCache.getInt(flatIndex(x, y))));
        default:
            throw new UnsupportedOperationException("unsupported sample data type " + pnkband.getRasterDatatype());
        }
    }

    /**
     * 
     * @param value value between 0 and (_maxValue - _minValue), which represents
     *              intensity of sample at x, y coordinates
     */
    public void set(int x, int y, long value) {
        cacheBlockSoft(toBlockX(x), toBlockY(y));
        value = translate(value);
        switch (pnkband.getRasterDatatype()) {
        case Pancake.TYPE_BYTE:
        case Pancake.TYPE_UNKNOWN:
            blockCache.put(flatIndex(x, y), (byte) (0xff & value));
            break;
        case Pancake.TYPE_UINT_16:
        case Pancake.TYPE_INT_16:
            blockCache.putShort(flatIndex(x, y), (short) (0xffff & value));
            break;
        case Pancake.TYPE_UINT_32:
        case Pancake.TYPE_INT_32:
            blockCache.putInt(flatIndex(x, y), (int) (0xffffffff & value));
            break;
        default:
            throw new UnsupportedOperationException("unsupported sample data type " + pnkband.getRasterDatatype());
        }
        isDirty = true;
    }

    /**
     * 
     * @param i index of i-th element in cached block
     * @return
     */
    public long get(int i) {
        if (!hasBlockInCache()) {
            throw new RuntimeException("attempt to invoke get on empty cache");
        }
        int flatIndex = i * nativeDtBytesSize;
        switch (pnkband.getRasterDatatype()) {
        case Pancake.TYPE_BYTE:
        case Pancake.TYPE_UNKNOWN:
            return detranslate(Byte.toUnsignedInt(blockCache.get(flatIndex)));
        case Pancake.TYPE_INT_16:
        case Pancake.TYPE_UINT_16:
            return detranslate(Short.toUnsignedInt(blockCache.getShort(flatIndex)));
        case Pancake.TYPE_INT_32:
        case Pancake.TYPE_UINT_32:
            return detranslate(Integer.toUnsignedLong(blockCache.getInt(flatIndex)));
        default:
            throw new UnsupportedOperationException("unsupported sample data type " + pnkband.getRasterDatatype());
        }
    }

    /**
     * 
     * @param i     index of i-th element in cached block
     * @param value
     */
    public void set(int i, long value) {
        if (!hasBlockInCache()) {
            throw new RuntimeException("attempt to invoke get on empty cache");
        }
        value = translate(value);
        int flatIndex = i * nativeDtBytesSize;
        switch (pnkband.getRasterDatatype()) {
        case Pancake.TYPE_BYTE:
        case Pancake.TYPE_UNKNOWN:
            blockCache.put(flatIndex, (byte) (0xff & value));
            break;
        case Pancake.TYPE_UINT_16:
        case Pancake.TYPE_INT_16:
            blockCache.putShort(flatIndex, (short) (0xffff & value));
            break;
        case Pancake.TYPE_UINT_32:
        case Pancake.TYPE_INT_32:
            blockCache.putInt(flatIndex, (int) (0xffffffff & value));
            break;
        default:
            throw new UnsupportedOperationException("unsupported sample data type " + pnkband.getRasterDatatype());
        }
        isDirty = true;
    }

    public boolean hasData(int x, int y) {
        long value = get(x, y);
        return ((long) pnkband.getNoData()) != value;
    }

    /**
     * 
     * @return width of band in samples
     */
    public int getXSize() {
        return pnkband.getXSize();
    }

    /**
     * 
     * @return height of band in samples
     */
    public int getYSize() {
        return pnkband.getYSize();
    }

    /**
     * 
     * @return width of block in samples
     */
    public int getBlockXSize() {
        return blockXSize;
    }

    /**
     * 
     * @return height of block in samples
     */
    public int getBlockYSize() {
        return blockYSize;
    }

    public PancakeBand getUnderlyingBand() {
        return pnkband;
    }

    /**
     * 
     * @return how many blocks fits in band row
     */
    public int getBlocksInRow() {
        return blocksInRow;
    }

    /**
     * 
     * @return how many blocks fits in band column
     */
    public int getBlocksInCol() {
        return blocksInCol;
    }

    /**
     * 
     * @return max possible value that underlying band ables to hold
     */
    public long getAbsoluteMaxValue() {
        return (1l << nativeDtbits) - 1;
    }

    /**
     * 
     * @return min possible value that underlying band ables to hold
     */
    public long getAbsoluteMinValue() {
        return 0;
    }

    /**
     * Drops cached block to the underlying band
     */
    public void flushCache() {
        if (blockInCache[0] != -1 && blockInCache[1] != -1) {
            if (isDirty) {
                int curBlockXSize = blockXSize(blockInCache[0]);
                int curBlockYSize = blockYSize(blockInCache[1]);
                try {
                    pnkband.writeRasterDirect(blockInCache[0] * blockXSize, blockInCache[1] * blockYSize, curBlockXSize,
                            curBlockYSize, curBlockXSize, curBlockYSize, pnkband.getRasterDatatype(), blockCache);
                } catch (RuntimeException e) {
                    throw new RuntimeException("failed to flush block cache", e);
                }
                isDirty = false;
            }
        }
    }
}
