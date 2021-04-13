package com.nocmok.pancake;

import java.io.Closeable;
import java.io.File;
import java.util.List;

public interface PancakeDataset extends Closeable {

    public List<PancakeBand> bands();

    public File path();

    public void flushCache();

    public int xSize();

    public int ySize();

    public Formats format();

    public String formatString();
}
