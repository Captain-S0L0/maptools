package xyz.terriblefriends.maptools.util;

public interface Chunk {
    void setBlock(int x, int y, int z, int id, int dv);

    void setBlockLight(int x, int y, int z, int light);

    void calculateHeightmap();
}
