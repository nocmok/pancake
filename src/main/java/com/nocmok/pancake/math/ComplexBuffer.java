package com.nocmok.pancake.math;

public interface ComplexBuffer {

    /**
     * Getter.
     * 
     * @param i
     * @return
     */
    public double re(int i);

    /**
     * Setter.
     * 
     * @param i
     * @param value
     */
    public void re(int i, double value);

    /**
     * Getter.
     * 
     * @param t
     * @return
     */
    public double im(int t);

    /**
     * Setter.
     * 
     * @param i
     * @param value
     */
    public void im(int i, double value);

    /**
     * Getter.
     * 
     * @param i
     * @return
     */
    public Complex get(int i);

    /**
     * Setter.
     * 
     * @param i
     * @param re
     * @param im
     */
    public void set(int i, double re, double im);

    /**
     * Setter.
     * 
     * @param i
     * @param complex
     */
    public void set(int i, Complex complex);

    /**
     * 
     * @return size of buffer.
     */
    public int size();
}