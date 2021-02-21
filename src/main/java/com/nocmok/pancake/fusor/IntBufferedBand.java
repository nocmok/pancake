package com.nocmok.pancake.fusor;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Optional;

import com.nocmok.pancake.Pancake;

import org.gdal.gdal.Band;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;

class IntBufferedBand {

    private PancakeBand _pnkband;

    private int blockXSize;

    private int blockYSize;

    /**
     * In general not all blocks have equal size. Sizes of special case blocks
     * listed in fields {@code lastXBlockByteSize}, {@code lastYBlockByteSize} and
     * {@code lastXYBlockByteSize}
     */
    private int blockByteSize;

    private int lastXBlockByteSize;

    private int lastYBlockByteSize;

    private int lastXYBlockByteSize;

    private int blocksInRow;

    private int blocksInCol;

    private ByteBuffer blockCache;

    /**
     * 0 - x coordinate 1 - y coordinate values [-1, -1] mean lack of cached block
     */
    private int[] blockInCache = new int[] { -1, -1 };

    /** Whether block cache was modified */
    private boolean isDirty = false;

    private int _datatype;

    private int dataTypeBytesSize;

    private long _maxValue;

    private long _minValue;

    public IntBufferedBand(PancakeBand pnkBand) {
        this(pnkBand, Integer.min(pnkBand.getBlockXSize(), pnkBand.getXSize()),
                Integer.min(pnkBand.getBlockYSize(), pnkBand.getYSize()), pnkBand.getRasterDatatype());
    }

    public IntBufferedBand(PancakeBand pnkBand, int dataType) {
        this(pnkBand, Integer.min(pnkBand.getBlockXSize(), pnkBand.getXSize()),
                Integer.min(pnkBand.getBlockYSize(), pnkBand.getYSize()), dataType);
    }

    public IntBufferedBand(PancakeBand pnkBand, int blockXSize, int blockYSize, int datatype) {
        this._pnkband = pnkBand;
        Band wrappedBand = pnkBand.getUnderlyingBand();
        this._datatype = datatype;
        this.dataTypeBytesSize = gdal.GetDataTypeSize(datatype) / 8;
        if (!Pancake.isIntegerDatatype(datatype)) {
            throw new UnsupportedOperationException(
                    "expected integer datatype, but " + gdal.GetDataTypeName(datatype) + " was provided");
        }
        Double[] dtMaxValue = new Double[] { Double.valueOf(0) };
        Double[] dtMinValue = new Double[] { Double.valueOf(0) };
        wrappedBand.GetMaximum(dtMaxValue);
        wrappedBand.GetMinimum(dtMinValue);
        _maxValue = Optional.ofNullable(dtMaxValue[0]).orElse((double) getDataTypeMaxValue(datatype)).longValue();
        _minValue = Optional.ofNullable(dtMinValue[0]).orElse(0.0).longValue();

        this.blockXSize = blockXSize;
        this.blockYSize = blockYSize;

        this.blocksInCol = (wrappedBand.getYSize() + blockYSize - 1) / blockYSize;
        this.blocksInRow = (wrappedBand.getXSize() + blockXSize - 1) / blockXSize;

        this.blockByteSize = blockXSize * blockYSize * dataTypeBytesSize;
        this.lastXBlockByteSize = blockYSize * blockXSize(blocksInCol - 1) * dataTypeBytesSize;
        this.lastYBlockByteSize = blockXSize * blockYSize(blocksInRow - 1) * dataTypeBytesSize;
        this.lastXYBlockByteSize = blockXSize(blocksInCol - 1) * blockYSize(blocksInRow - 1) * dataTypeBytesSize;

        this.blockCache = ByteBuffer.allocateDirect(blockByteSize);
        this.blockCache.order(ByteOrder.nativeOrder());
    }

    private long getDataTypeMaxValue(int dataType) {
        switch (dataType) {
            case Pancake.TYPE_BYTE:
            case Pancake.TYPE_UNKNOWN:
                return 0xff;
            case Pancake.TYPE_INT_16:
                return 0x7fff;
            case Pancake.TYPE_UINT_16:
                return 0xffff;
            case Pancake.TYPE_INT_32:
                return 0x7fffffff;
            case Pancake.TYPE_UINT_32:
                return 0xffffffffL;
            default:
                throw new UnsupportedOperationException("unsupported sample data type " + dataType);
        }
    }

    private int getBlockSize(int blockX, int blockY) {
        if (blockX + 1 == blocksInRow && blockY + 1 == blocksInCol) {
            return lastXYBlockByteSize;
        }
        if (blockX + 1 == blocksInRow) {
            return lastXBlockByteSize;
        }
        if (blockY + 1 == blocksInCol) {
            return lastYBlockByteSize;
        }
        return blockByteSize;
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
        return (blockX + 1 < blocksInRow) ? (blockXSize) : (_pnkband.getXSize() - (blocksInRow - 1) * blockXSize);
    }

    /**
     * @return height of block with specified y coordinate
     */
    public int blockYSize(int blockY) {
        return (blockY + 1 < blocksInCol) ? (blockYSize) : (_pnkband.getYSize() - (blocksInCol - 1) * blockYSize);
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

    private boolean hasBlockInCache(){
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
        int code = _pnkband.readRasterDirect(blockX * blockXSize, blockY * blockYSize, curBlockXSize, curBlockYSize,
                curBlockXSize, curBlockYSize, _datatype, blockCache);
        if (code == gdalconst.CE_Failure) {
            throw new RuntimeException("failed to cache block, due to error: " + gdal.GetLastErrorMsg());
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
                * dataTypeBytesSize;
        return flatIndex;
    }

    private long translate(long value) {
        return value + _minValue;
    }

    private long detranslate(long value) {
        return value - _minValue;
    }

    /**
     * @return value between 0 and (_maxValue - _minValue), which represents
     *         intensity of sample at x, y coordinates
     */
    public long get(int x, int y) {
        cacheBlockSoft(toBlockX(x), toBlockY(y));
        switch (_datatype) {
            case Pancake.TYPE_BYTE:
            case Pancake.TYPE_UNKNOWN:
                return detranslate(Byte.toUnsignedInt(blockCache.get(flatIndex(x, y))));
            case Pancake.TYPE_INT_16:
                return detranslate(blockCache.getShort(flatIndex(x, y)));
            case Pancake.TYPE_UINT_16:
                return detranslate(Short.toUnsignedInt(blockCache.getShort(flatIndex(x, y))));
            case Pancake.TYPE_INT_32:
                return detranslate(blockCache.getInt(flatIndex(x, y)));
            case Pancake.TYPE_UINT_32:
                return detranslate(Integer.toUnsignedLong(blockCache.getInt(flatIndex(x, y))));
            default:
                throw new UnsupportedOperationException("unsupported sample data type " + _datatype);
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
        switch (_datatype) {
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
                throw new UnsupportedOperationException("unsupported sample data type " + _datatype);
        }
        isDirty = true;
    }

    /**
     * 
     * @param i index of i-th element in cached block
     * @return
     */
    public long get(int i) {
        if(!hasBlockInCache()){
            throw new RuntimeException("attempt to invoke get on empty cache");
        }
        int flatIndex = i * dataTypeBytesSize;
        switch (_datatype) {
            case Pancake.TYPE_BYTE:
            case Pancake.TYPE_UNKNOWN:
                return detranslate(Byte.toUnsignedInt(blockCache.get(flatIndex)));
            case Pancake.TYPE_INT_16:
                return detranslate(blockCache.getShort(flatIndex));
            case Pancake.TYPE_UINT_16:
                return detranslate(Short.toUnsignedInt(blockCache.getShort(flatIndex)));
            case Pancake.TYPE_INT_32:
                return detranslate(blockCache.getInt(flatIndex));
            case Pancake.TYPE_UINT_32:
                return detranslate(Integer.toUnsignedLong(blockCache.getInt(flatIndex)));
            default:
                throw new UnsupportedOperationException("unsupported sample data type " + _datatype);
        }
    }

    /**
     * 
     * @param i index of i-th element in cached block
     * @param value
     */
    public void set(int i, long value){
        if(!hasBlockInCache()){
            throw new RuntimeException("attempt to invoke get on empty cache");
        }
        value = translate(value);
        int flatIndex = i * dataTypeBytesSize;
        switch (_datatype) {
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
                throw new UnsupportedOperationException("unsupported sample data type " + _datatype);
        }
        isDirty = true;
    }

    public boolean hasData(int x, int y) {
        long value = get(x, y);
        return ((long) _pnkband.getNoData()) != value;
    }

    /**
     * 
     * @return width of band in samples
     */
    public int getXSize() {
        return _pnkband.getXSize();
    }

    /**
     * 
     * @return height of band in samples
     */
    public int getYSize() {
        return _pnkband.getYSize();
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
        return _pnkband;
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
        return _maxValue;
    }

    /**
     * 
     * @return min possible value that underlying band ables to hold
     */
    public long getAbsoluteMinValue() {
        return _minValue;
    }

    /**
     * Drops cached block to the underlying band
     */
    public void flushCache() {
        if (blockInCache[0] != -1 && blockInCache[1] != -1) {
            if (isDirty) {
                int curBlockXSize = blockXSize(blockInCache[0]);
                int curBlockYSize = blockYSize(blockInCache[1]);
                int code = _pnkband.writeRasterDirect(blockInCache[0] * blockXSize, blockInCache[1] * blockYSize,
                        curBlockXSize, curBlockYSize, curBlockXSize, curBlockYSize, _datatype, blockCache);
                if (code == gdalconst.CE_Failure) {
                    throw new RuntimeException("failed to drop block cache, due to error: " + gdal.GetLastErrorMsg());
                }
                isDirty = false;
            }
        }
    }
}
