package com.nocmok.pancake;

@FunctionalInterface
public interface PancakeProgressListener {

    public void listen(Integer phase, Double progress, String message);

    public static final PancakeProgressListener empty = new PancakeProgressListener(){
        @Override
        public void listen(Integer phase, Double progress, String message) {
            
        }
    };
}
