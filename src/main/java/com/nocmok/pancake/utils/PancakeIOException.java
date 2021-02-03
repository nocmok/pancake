package com.nocmok.pancake.utils;

public class PancakeIOException extends RuntimeException {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public PancakeIOException() {
        super();
    }

    public PancakeIOException(String message) {
        super(message);
    }

    public PancakeIOException(Throwable cause) {
        super(cause);
    }

    public PancakeIOException(String message, Throwable cause) {
        super(message, cause);
    }
}
