package com.nocmok.pancake.fusor;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.nocmok.pancake.Pancake;

import org.gdal.gdal.Band;
import org.gdal.gdal.gdal;

public class NormalizedBand {

    private Band band;

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

    private int dataTypeBytesSize;

    private double dataTypeMaxValueFloat;

    private double dataTypeMinValueFloat;

    public NormalizedBand(Band band) {
        this.band = band;
        this.blockXSize = Integer.min(band.GetBlockXSize(), band.getXSize());
        this.blockYSize = Integer.min(band.GetBlockYSize(), band.getYSize());

        this.dataTypeBytesSize = gdal.GetDataTypeSize(band.GetRasterDataType()) / 8;

        Double[] dtMaxValue = new Double[] { Double.valueOf(0) };
        Double[] dtMinValue = new Double[] { Double.valueOf(0) };
        band.GetMaximum(dtMaxValue);
        band.GetMinimum(dtMinValue);
        this.dataTypeMaxValueFloat = dtMaxValue[0] != null ? dtMaxValue[0] : getDataTypeMaxValue(band.getDataType());
        this.dataTypeMinValueFloat = dtMinValue[0] != null ? dtMinValue[0] : 0;

        this.blocksInCol = (band.getYSize() + blockYSize - 1) / blockYSize;
        this.blocksInRow = (band.getXSize() + blockXSize - 1) / blockXSize;

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
    private int toBlockX(int x) {
        return x / blockXSize;
    }

    /**
     * @return y coordinate of block which contains sample with specified y
     *         coordinate
     */
    private int toBlockY(int y) {
        return y / blockYSize;
    }

    /**
     * @return x coordinate of first sample, which belongs to block with specified x
     *         coordinate
     */
    private int blockXStart(int blockX) {
        return blockX * blockXSize;
    }

    /**
     * @return y coordinate of first sample, which belongs to block with specified y
     *         coordinate
     */
    private int blockYStart(int blockY) {
        return blockY * blockYSize;
    }

    /**
     * @return width of block with specified x coordinate
     */
    private int blockXSize(int blockX) {
        return (blockX + 1 < blocksInRow) ? (blockXSize) : (band.getXSize() - (blocksInRow - 1) * blockXSize);
    }

    /**
     * @return height of block with specified y coordinate
     */
    private int blockYSize(int blockY) {
        return (blockY + 1 < blocksInCol) ? (blockYSize) : (band.getYSize() - (blocksInCol - 1) * blockYSize);
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
    private void cacheBlock(int blockX, int blockY) {
        flushCache();
        // band.ReadBlock_Direct(blockX, blockY, blockCache);

        int curBlockXSize = blockXSize(blockX);
        int curBlockYSize = blockYSize(blockY);

        band.ReadRaster_Direct(blockX * blockXSize, blockY * blockYSize, curBlockXSize, curBlockYSize, curBlockXSize,
                curBlockYSize, band.GetRasterDataType(), blockCache);

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
        switch (band.getDataType()) {
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
                throw new UnsupportedOperationException("unsupported sample data type " + band.getDataType());
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
        switch (band.getDataType()) {
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
                throw new UnsupportedOperationException("unsupported sample data type " + band.getDataType());
        }
    }

    /**
     * 
     * @return width of band in samples
     */
    public int getXSize() {
        return band.getXSize();
    }

    /**
     * 
     * @return height of band in samples
     */
    public int getYSize() {
        return band.getYSize();
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

    public Band getUnderlyingBand() {
        return band;
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
            // band.WriteBlock_Direct(blockInCache[0], blockInCache[1], blockCache);

            int curBlockXSize = blockXSize(blockInCache[0]);
            int curBlockYSize = blockYSize(blockInCache[1]);

            band.WriteRaster_Direct(blockInCache[0] * blockXSize, blockInCache[1] * blockYSize, curBlockXSize,
                    curBlockYSize, curBlockXSize, curBlockYSize, band.getDataType(), blockCache);
            
                    // System.out.println("band " +
            // gdal.GetColorInterpretationName(band.GetColorInterpretation())
            // + " cache dropped for block " + "[" + blockInCache[0] + "," + blockInCache[1]
            // + "]");
        }
    }
}
