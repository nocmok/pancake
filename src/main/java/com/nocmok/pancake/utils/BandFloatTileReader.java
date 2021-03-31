package com.nocmok.pancake.utils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.nocmok.pancake.Pancake;
import com.nocmok.pancake.PancakeBand;

import org.gdal.gdal.Band;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;

public class BandFloatTileReader {

    private PancakeBand pnkband;

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

    private int datatype;

    private int dataTypeBytesSize;

    private double dataTypeMaxValueFloat;

    private double dataTypeMinValueFloat;

    public BandFloatTileReader(PancakeBand pnkband) {
        this(pnkband, Integer.min(pnkband.getBlockXSize(), pnkband.getXSize()),
                Integer.min(pnkband.getBlockYSize(), pnkband.getYSize()));
    }

    public BandFloatTileReader(PancakeBand pnkband, int blockXSize, int blockYSize) {
        this.pnkband = pnkband;
        Band underlyingBand = pnkband.getUnderlyingBand();

        this.datatype = pnkband.getRasterDatatype();
        this.blockXSize = blockXSize;
        this.blockYSize = blockYSize;
        this.dataTypeBytesSize = Pancake.getDatatypeSizeBytes(datatype);

        Double[] dtMaxValue = new Double[] { Double.valueOf(0) };
        Double[] dtMinValue = new Double[] { Double.valueOf(0) };
        underlyingBand.GetMaximum(dtMaxValue);
        underlyingBand.GetMinimum(dtMinValue);
        this.dataTypeMaxValueFloat = dtMaxValue[0] != null ? dtMaxValue[0] : getDataTypeMaxValue(datatype);
        this.dataTypeMinValueFloat = dtMinValue[0] != null ? dtMinValue[0] : 0;

        this.blocksInCol = (pnkband.getYSize() + blockYSize - 1) / blockYSize;
        this.blocksInRow = (pnkband.getXSize() + blockXSize - 1) / blockXSize;

        this.blockByteSize = blockXSize * blockYSize * dataTypeBytesSize;
        this.lastXBlockByteSize = blockYSize * blockXSize(blocksInCol - 1) * dataTypeBytesSize;
        this.lastYBlockByteSize = blockXSize * blockYSize(blocksInRow - 1) * dataTypeBytesSize;
        this.lastXYBlockByteSize = blockXSize(blocksInCol - 1) * blockYSize(blocksInRow - 1) * dataTypeBytesSize;

        this.blockCache = ByteBuffer.allocateDirect(blockByteSize);
        this.blockCache.order(ByteOrder.nativeOrder());
    }

    private double getDataTypeMaxValue(int dataType) {
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
            case Pancake.TYPE_FLOAT_32:
                return Float.MAX_VALUE;
            case Pancake.TYPE_FLOAT_64:
                return Double.MAX_VALUE;
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

    private double normalizeInt(long value) {
        return (value - dataTypeMinValueFloat) / (dataTypeMaxValueFloat - dataTypeMinValueFloat);
    }

    private double normalizeFloat(double value) {
        return (value - dataTypeMinValueFloat) / (dataTypeMaxValueFloat - dataTypeMinValueFloat);
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
        int code = pnkband.readRasterDirect(blockX * blockXSize, blockY * blockYSize, curBlockXSize, curBlockYSize,
                curBlockXSize, curBlockYSize, datatype, blockCache);
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

    public double get(int x, int y) {
        cacheBlockSoft(toBlockX(x), toBlockY(y));
        switch (datatype) {
            case Pancake.TYPE_BYTE:
            case Pancake.TYPE_UNKNOWN:
                return normalizeInt(Byte.toUnsignedInt(blockCache.get(flatIndex(x, y))));
            case Pancake.TYPE_INT_16:
                return normalizeInt(blockCache.getShort(flatIndex(x, y)));
            case Pancake.TYPE_UINT_16:
                return normalizeInt(Short.toUnsignedInt(blockCache.getShort(flatIndex(x, y))));
            case Pancake.TYPE_INT_32:
                return normalizeInt(blockCache.getInt(flatIndex(x, y)));
            case Pancake.TYPE_UINT_32:
                return normalizeInt(Integer.toUnsignedLong(blockCache.getInt(flatIndex(x, y))));
            case Pancake.TYPE_FLOAT_32:
                return normalizeFloat(blockCache.getFloat(flatIndex(x, y)));
            case Pancake.TYPE_FLOAT_64:
                return normalizeFloat(blockCache.getDouble(flatIndex(x, y)));
            default:
                throw new UnsupportedOperationException("unsupported sample data type " + datatype);
        }
    }

    private double denormalizeFloat(double value) {
        return dataTypeMaxValueFloat * value;
    }

    private int denormalizeInt(double value) {
        return (int) (value * dataTypeMaxValueFloat);
    }

    public void set(int x, int y, double value) {
        cacheBlockSoft(toBlockX(x), toBlockY(y));
        switch (datatype) {
            case Pancake.TYPE_BYTE:
            case Pancake.TYPE_UNKNOWN:
                blockCache.put(flatIndex(x, y), (byte) (0xff & denormalizeInt(value)));
                break;
            case Pancake.TYPE_UINT_16:
            case Pancake.TYPE_INT_16:
                blockCache.putShort(flatIndex(x, y), (short) (0xffff & denormalizeInt(value)));
                break;
            case Pancake.TYPE_UINT_32:
            case Pancake.TYPE_INT_32:
                blockCache.putInt(flatIndex(x, y), denormalizeInt(value));
                break;
            case Pancake.TYPE_FLOAT_32:
                blockCache.putFloat(flatIndex(x, y), (float) denormalizeFloat(value));
                break;
            case Pancake.TYPE_FLOAT_64:
                blockCache.putDouble(flatIndex(x, y), denormalizeFloat(value));
                break;
            default:
                throw new UnsupportedOperationException("unsupported sample data type " + datatype);
        }
        isDirty = true;
    }

    /**
     * 
     * @param i index of i-th element in cached block
     * @return
     */
    public double get(int i) {
        if (!hasBlockInCache()) {
            throw new RuntimeException("attempt to invoke get on empty cache");
        }
        int flatIndex = i * dataTypeBytesSize;
        switch (datatype) {
            case Pancake.TYPE_BYTE:
            case Pancake.TYPE_UNKNOWN:
                return normalizeInt(Byte.toUnsignedInt(blockCache.get(flatIndex)));
            case Pancake.TYPE_INT_16:
                return normalizeInt(blockCache.getShort(flatIndex));
            case Pancake.TYPE_UINT_16:
                return normalizeInt(Short.toUnsignedInt(blockCache.getShort(flatIndex)));
            case Pancake.TYPE_INT_32:
                return normalizeInt(blockCache.getInt(flatIndex));
            case Pancake.TYPE_UINT_32:
                return normalizeInt(Integer.toUnsignedLong(blockCache.getInt(flatIndex)));
            case Pancake.TYPE_FLOAT_32:
                return normalizeFloat(blockCache.getFloat(flatIndex));
            case Pancake.TYPE_FLOAT_64:
                return normalizeFloat(blockCache.getDouble(flatIndex));
            default:
                throw new UnsupportedOperationException("unsupported sample data type " + datatype);
        }
    }

    /**
     * 
     * @param i     index of i-th element in cached block
     * @param value
     */
    public void set(int i, double value) {
        if (!hasBlockInCache()) {
            throw new RuntimeException("attempt to invoke get on empty cache");
        }
        int flatIndex = i * dataTypeBytesSize;
        switch (datatype) {
            case Pancake.TYPE_BYTE:
            case Pancake.TYPE_UNKNOWN:
                blockCache.put(flatIndex, (byte) (0xff & denormalizeInt(value)));
                break;
            case Pancake.TYPE_UINT_16:
            case Pancake.TYPE_INT_16:
                blockCache.putShort(flatIndex, (short) (0xffff & denormalizeInt(value)));
                break;
            case Pancake.TYPE_UINT_32:
            case Pancake.TYPE_INT_32:
                blockCache.putInt(flatIndex, denormalizeInt(value));
                break;
            case Pancake.TYPE_FLOAT_32:
                blockCache.putFloat(flatIndex, (float) denormalizeFloat(value));
                break;
            case Pancake.TYPE_FLOAT_64:
                blockCache.putDouble(flatIndex, denormalizeFloat(value));
                break;
            default:
                throw new UnsupportedOperationException("unsupported sample data type " + datatype);
        }
        isDirty = true;
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
    public double getAbsoluteMaxValue() {
        return dataTypeMaxValueFloat;
    }

    /**
     * 
     * @return min possible value that underlying band ables to hold
     */
    public double getAbsoluteMinValue() {
        return dataTypeMinValueFloat;
    }

    /**
     * Drops cached block to the underlying band
     */
    public void flushCache() {
        if (blockInCache[0] != -1 && blockInCache[1] != -1) {
            if (isDirty) {
                int curBlockXSize = blockXSize(blockInCache[0]);
                int curBlockYSize = blockYSize(blockInCache[1]);
                pnkband.writeRasterDirect(blockInCache[0] * blockXSize, blockInCache[1] * blockYSize, curBlockXSize,
                        curBlockYSize, curBlockXSize, curBlockYSize, datatype, blockCache);

                isDirty = false;
            }
        }
    }
}
