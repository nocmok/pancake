package com.nocmok.pancake.fusor;

import java.util.Map;

import com.nocmok.pancake.Spectrum;
import com.nocmok.pancake.utils.Rectangle;

/**
 * This interface specifies api for objects, that apply fusion algorithm to
 * fully prepared bands. The objects of this interface not aimed to perform any
 * preprocessing work like upsampling or alignment, unrelative to fusion
 * algorithm.
 */
public interface Fusor {

    /**
     * This method expects all bands to be the same resolution.
     * 
     * @param dst destination bands to which result will be written
     * @param src source bands, that have to be fused
     */
    public void fuse(Map<Spectrum, PancakeBand> dst, Map<Spectrum, PancakeBand> src);

    /**
     * 
     * @param dst  destination bands to which result will be written
     * @param src  source bands, that have to be fused
     * @param area target area to fuse
     */
    public void fuse(Map<Spectrum, PancakeBand> dst, Map<Spectrum, PancakeBand> src, Rectangle area);

}
