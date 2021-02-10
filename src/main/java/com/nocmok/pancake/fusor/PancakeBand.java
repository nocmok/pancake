package com.nocmok.pancake.fusor;

import org.gdal.gdal.Band;

public interface PancakeBand {

    /**
     * 
     * @return value in range [0, 1] which represents sample intensity at x, y
     */
    public double get(int x, int y);

    /**
     * set sample at x, y to value in range [0, 1] which represents sample intensity
     */
    public void set(int x, int y, double value);

    /**
     * 
     * @return width of this band
     */
    public int getXSize();

    /**
     * 
     * @return height of this band
     */
    public int getYSize();

    /**
     * 
     * @return width of block in samples units
     */
    public int getBlockXSize();

    /**
     * 
     * @return height of block in samples units
     */
    public int getBlockYSize();

    /**
     * @return x coordinate of block which contains sample with specified x
     *         coordinate
     */
    public int toBlockX(int x);

    /**
     * @return y coordinate of block which contains sample with specified y
     *         coordinate
     */
    public int toBlockY(int y);

    /**
     * 
     * @return gdal band which wrapped by this band
     */
    public Band getUnderlyingBand();

    /**
     * Drops cached block to the underlying band
     */
    public void flushCache();
}
