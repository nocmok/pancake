package com.nocmok.pancake.utils;

public class Shape {

    private int xsize;

    private int ysize;

    public Shape(int xsize, int ysize) {
        this.xsize = xsize;
        this.ysize = ysize;
    }

    public int xsize() {
        return xsize;
    }

    public int ysize() {
        return ysize;
    }

    public int size(){
        return xsize * ysize;
    }

    public static Shape of(int xsize, int ysize) {
        return new Shape(xsize, ysize);
    }
}
