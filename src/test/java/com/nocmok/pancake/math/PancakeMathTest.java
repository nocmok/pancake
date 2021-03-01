package com.nocmok.pancake.math;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PancakeMathTest {

    @Test
    public void testReverse() {
        assertEquals(0, PancakeMath.reverse(0));
        assertEquals(0xffffffff, PancakeMath.reverse(0xffffffff));

        assertEquals(0x80000000, PancakeMath.reverse(0x00000001));
        assertEquals(0x00000001, PancakeMath.reverse(0x80000000));

        assertEquals(0x08000000, PancakeMath.reverse(0x00000010));
        assertEquals(0x00000010, PancakeMath.reverse(0x08000000));

        assertEquals(0x00800000, PancakeMath.reverse(0x00000100));
        assertEquals(0x00000100, PancakeMath.reverse(0x00800000));

        assertEquals(0x00080000, PancakeMath.reverse(0x00001000));
        assertEquals(0x00001000, PancakeMath.reverse(0x00080000));

        assertEquals(0xaaaaaaaa, PancakeMath.reverse(0x55555555));
        assertEquals(0x55555555, PancakeMath.reverse(0xaaaaaaaa));
    }

    @Test
    public void testReverseNLeast() {
        assertEquals(0x00008000, PancakeMath.reverseNLeast(0x00000001, 16));
        assertEquals(0x00000001, PancakeMath.reverseNLeast(0x00008000, 16));
    }
}
