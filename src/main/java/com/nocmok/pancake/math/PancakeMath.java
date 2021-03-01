package com.nocmok.pancake.math;

public class PancakeMath {

    /**
     * Reverse all bits in integer.
     * 
     * @param value
     * @return
     */
    public static int reverse(int value) {
        value = ((value & 0xffff0000) >>> 16) | ((value & 0x0000ffff) << 16);
        value = ((value & 0xff00ff00) >>> 8) | ((value & 0x00ff00ff) << 8);
        value = ((value & 0xf0f0f0f0) >>> 4) | ((value & 0x0f0f0f0f) << 4);
        value = ((value & 0xcccccccc) >>> 2) | ((value & 0x33333333) << 2);
        value = ((value & 0xaaaaaaaa) >>> 1) | ((value & 0x55555555) << 1);
        return value;
    }

    /**
     * Reverse n least significant bits in integer.
     * 
     * @param value
     * @param nLeast
     * @return
     */
    public static int reverseNLeast(int value, int nLeast) {
        nLeast = Integer.min(nLeast, 32);
        int result = reverse(value);
        result = result >>> (32 - nLeast);
        return result;
    }

    /**
     * 
     * @param value
     * @return true if value is power of 2, false otherwise
     */
    public static boolean isPow2(int value) {
        if (value < 0) {
            value = -value;
        }
        int sum = 0;
        for (int i = 0; i < 32; ++i) {
            if ((value & (1 << i)) != 0) {
                ++sum;
                if (sum > 1) {
                    return false;
                }
            }
        }
        return sum == 1;
    }
}
