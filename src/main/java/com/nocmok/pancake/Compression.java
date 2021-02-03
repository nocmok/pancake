package com.nocmok.pancake;

public enum Compression {

    /** without compression */
    NONE("NONE"),

    /** JPEG compression */
    JPEG("JPEG"),

    /** LZW compression */
    LZW("LZW"),

    /** Byte-oriented, run length compression */
    PackBits("PACKBITS"),

    /** "Zip-in-TIFF" compression */
    Deflate("DEFLATE"),

    /** This compression should only be used with 1bit (NBITS=1) data */
    CCITTRLE("CCITTRLE"),

    /** This compression should only be used with 1bit (NBITS=1) data */
    CCITTFAX3("CCITTFAX3"),

    /** This compression should only be used with 1bit (NBITS=1) data */
    CCITTFAX4("CCITTFAX4"),

    LZMA("LZMA"),

    ZSTD("ZSTD"),

    LERC("LERC"),

    LERC_DEFLATE("LERC_DEFLATE"),

    LERC_ZSTD("LERC_ZSTD"),

    WEBP("WEBP"),

    ;

    private final String gdalIdentifier;

    private Compression(String gdalIdentifier) {
        this.gdalIdentifier = gdalIdentifier;
    }

    public String gdalIdentifier() {
        return this.gdalIdentifier;
    }
}
