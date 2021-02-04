package com.nocmok.pancake.utils;

/**
 * Pojo class, that describes rectangular area on integer grid.
 */
public class Rectangle {

    private int x0;

    private int y0;

    private int xSize;

    private int ySize;

    public Rectangle(int x0, int y0, int xSize, int ySize) {
        this.x0 = x0;
        this.y0 = y0;
        this.xSize = xSize;
        this.ySize = ySize;
    }

    public int x0() {
        return x0;
    }

    public int y0() {
        return y0;
    }

    public int xSize() {
        return xSize;
    }

    public int ySize() {
        return ySize;
    }
}
