package com.nocmok.pancake;

import java.io.File;
import java.util.List;

public interface PancakeDataset {

    public List<PancakeBand> bands();

    public File path();

    public void flushCache();
}
