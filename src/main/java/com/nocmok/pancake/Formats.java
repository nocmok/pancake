package com.nocmok.pancake;

import java.util.HashMap;
import java.util.Map;

import org.gdal.gdal.Driver;
import org.gdal.gdal.gdal;

public enum Formats {

    GTiff("GTiff") {
        @Override
        PancakeOptions toDriverOptions(PancakeOptions options) {
            PancakeOptions driverOptions = new PancakeOptions();

            driverOptions.put("INTERLEAVE", options.getStringOr(PancakeConstants.KEY_INTERLEAVE, "BAND"));
            driverOptions.put("TILED", options.getBoolOr(PancakeConstants.KEY_TILED, true) ? "YES" : "NO");
            driverOptions.put("BLOCKXSIZE", options.getIntOr(PancakeConstants.KEY_BLOCKXSIZE, 256));
            driverOptions.put("BLOCKYSIZE", options.getIntOr(PancakeConstants.KEY_BLOCKYSIZE, 256));
            driverOptions.put("COMPRESS", options.getStringOr(PancakeConstants.KEY_COMPRESSION, "NONE"));
            driverOptions.put("NUM_THREADS", options.getStringOr(PancakeConstants.KEY_COMPRESSION_NUM_THREADS, "1"));
            driverOptions.put("PHOTOMETRIC", options.getStringOr(PancakeConstants.KEY_PHOTOMETRIC, "MINISBLACK"));
            driverOptions.put("BIGTIFF", options.getStringOr(PancakeConstants.KEY_BIGTIFF, "NO"));

            int compressionQuality = options.getIntOr(PancakeConstants.KEY_COMPRESSION_QUALITY, 0);
            if (compressionQuality > 0) {
                int specificQuality;
                Compression compression = Compression
                        .byName(options.getStringOr(PancakeConstants.KEY_COMPRESSION, "NONE"));
                switch (compression) {
                    case JPEG:
                        compressionQuality = options.getIntOr(PancakeConstants.KEY_COMPRESSION_QUALITY, 75);
                        specificQuality = compressionQuality;
                        driverOptions.put("JPEG_QUALITY", specificQuality);
                        break;
                    case Deflate:
                        compressionQuality = options.getIntOr(PancakeConstants.KEY_COMPRESSION_QUALITY, 70);
                        specificQuality = (int) (((double) compressionQuality) * 9 / 100);
                        driverOptions.put("ZLEVEL", specificQuality);
                        break;
                    case LERC_DEFLATE:
                        compressionQuality = options.getIntOr(PancakeConstants.KEY_COMPRESSION_QUALITY, 50);
                        specificQuality = (int) (((double) compressionQuality) * 9 / 100);
                        driverOptions.put("ZLEVEL", specificQuality);
                        break;
                    case ZSTD:
                    case LERC_ZSTD:
                        compressionQuality = options.getIntOr(PancakeConstants.KEY_COMPRESSION_QUALITY, 42);
                        specificQuality = (int) ((double) compressionQuality) / 100 * 9;
                        driverOptions.put("ZSTD_LEVEL", specificQuality);
                        break;
                    case WEBP:
                        compressionQuality = options.getIntOr(PancakeConstants.KEY_COMPRESSION_QUALITY, 75);
                        specificQuality = compressionQuality;
                        driverOptions.put("WEBP_LEVEL", specificQuality);
                        break;
                    default:
                        break;
                }
            }
            return driverOptions;
        }
    },

    VRT("VRT") {
        @Override
        public PancakeOptions toDriverOptions(PancakeOptions options) {
            return new PancakeOptions();
        }
    },

    ;

    private static final Map<String, Formats> nameFormatMapping = new HashMap<>();

    static {
        for (Formats format : Formats.values()) {
            nameFormatMapping.put(format.driverName().toLowerCase(), format);
        }
    }

    public static Formats byName(String name) {
        return nameFormatMapping.get(name.toLowerCase());
    }

    private final String driverName;

    private Formats(String driverName) {
        this.driverName = driverName;
    }

    String driverName() {
        return driverName;
    }

    Driver getDriver() {
        return gdal.GetDriverByName(driverName);
    }

    /**
     * Translate pancake specific image option notation, to gdal driver specific
     * creation options notation
     */
    abstract PancakeOptions toDriverOptions(PancakeOptions options);
}
