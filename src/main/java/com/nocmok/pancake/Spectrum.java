package com.nocmok.pancake;

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
}
