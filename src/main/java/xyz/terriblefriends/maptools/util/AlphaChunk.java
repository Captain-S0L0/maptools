package xyz.terriblefriends.maptools.util;

import xyz.terriblefriends.maptools.nbt.NBTTagList;

import java.util.HashSet;

public class AlphaChunk implements Chunk {
    public NBTTagList entities = new NBTTagList();
    public NBTTagList tileEntities = new NBTTagList();
    public byte[] blocks = new byte[32768];
    public byte[] heightMap = new byte[256];
    public NibbleArray blockDvs = new NibbleArray(32768);
    public NibbleArray blockLight = new NibbleArray(32768);
    public NibbleArray skyLight = new NibbleArray(32768);

    public boolean populated = false;

    public int xPos = 0;
    public int zPos = 0;

    private static final HashSet<Integer> OPAQUE_IDS = new HashSet<>();

    @Override
    public void setBlock(int x, int y, int z, int id, int dv) {
        this.blocks[this.getBlockIndex(x, y, z)] = (byte)id;
        this.blockDvs.setNibble(x, y, z, dv);
    }

    @Override
    public void setBlockLight(int x, int y, int z, int light) {
        this.blockLight.setNibble(x, y, z, light);
    }

    public void calculateBasicSkylight() {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < 128; y++) {
                    if (this.heightMap[z << 4 | x] > y) {
                        this.skyLight.setNibble(x, y, z, 15);
                    }
                    else {
                        this.skyLight.setNibble(x, y, z, 0);
                    }
                }
            }
        }
    }

    @Override
    public void calculateHeightmap() {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 127; y >= 0; y--) {
                    if (OPAQUE_IDS.contains((int)this.blocks[this.getBlockIndex(x,y,z)])) {
                        this.heightMap[z << 4 | x] = (byte)y;
                        break;
                    }
                }
            }
        }
    }

    private int getBlockIndex(int x, int y, int z) {
        return x << 11 | z << 7 | y;
    }

    static {
        int[] ids = new int[]{1,2,3,4,5,7,12,13,14,15,16,17,19,35,41,42,44,45,46,47,48,49,54,56,57,58,61,62};
        for (int i : ids) {
            OPAQUE_IDS.add(i);
        }
    }

}
