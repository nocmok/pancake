package com.nocmok.pancake;

import java.util.List;

public enum Spectrum {

    /** Undefined spectrum */
    NONE("none"),

    /** Red */
    R("r"),

    /** Green */
    G("g"),

    /** Blue */
    B("b"),

    /** Near infrated */
    NI("ni"),

    /** Panchromatic band */
    PA("pa")

    ;

    private String name;

    private Spectrum(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static List<Spectrum> all() {
        return List.<Spectrum>of(Spectrum.values());
    }

    public static List<Spectrum> RGB() {
        return List.of(R, G, B);
    }

    public static List<Spectrum> RGBNI(){
        return List.of(R, G, B, NI);
    }
}
