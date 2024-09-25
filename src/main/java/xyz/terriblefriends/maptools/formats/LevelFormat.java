package xyz.terriblefriends.maptools.formats;

import xyz.terriblefriends.maptools.util.AlphaChunk;
import xyz.terriblefriends.maptools.util.LevelData;

public interface LevelFormat {
    default void read() throws Exception {
        throw new UnsupportedOperationException();
    }

    default void write() throws Exception {
        throw new UnsupportedOperationException();
    }

    default AlphaChunk getChunk(int x, int z) {
        throw new UnsupportedOperationException();
    }

    default void setChunk(int x, int z, AlphaChunk chunk) {
        throw new UnsupportedOperationException();
    }

    default LevelData getLevelData() {
        throw new UnsupportedOperationException();
    }

    default void setLevelData(LevelData data) {
        throw new UnsupportedOperationException();
    }

    default void importLevel(LevelFormat from) throws Exception {
        throw new UnsupportedOperationException();
    }

    default void exportLevel(LevelFormat to) throws Exception {
        throw new UnsupportedOperationException();
    }
}
