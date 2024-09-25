package xyz.terriblefriends.maptools.formats;

import xyz.terriblefriends.maptools.util.AlphaChunk;
import xyz.terriblefriends.maptools.util.LevelData;
import xyz.terriblefriends.maptools.nbt.CompressedStreamTools;
import xyz.terriblefriends.maptools.nbt.NBTTagCompound;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class AlphaLevelFormat implements LevelFormat {
    public File worldDirectory;
    public LevelData levelData;

    public List<AlphaChunk> chunks = new ArrayList<>();

    public AlphaLevelFormat(File worldDirectory) {
        this.worldDirectory = worldDirectory;
    }

    @Override
    public void read() throws Exception {
        if (!this.worldDirectory.exists() && !this.worldDirectory.mkdirs()) {
            throw new RuntimeException("Failed to create directory for "+this.worldDirectory.getAbsolutePath()+"!");
        }
    }

    @Override
    public void write() throws Exception {
        System.out.println("Writing chunks...");
        for (AlphaChunk chunk : this.chunks) {
            if (chunk == null) {
                continue;
            }

            File chunkFile = this.chunkFileForXZ(chunk.xPos, chunk.zPos, true);

            NBTTagCompound levelTag = new NBTTagCompound();
            levelTag.setTag("Entities", chunk.entities);
            levelTag.setTag("TileEntities", chunk.tileEntities);
            levelTag.setInteger("xPos", chunk.xPos);
            levelTag.setInteger("zPos", chunk.zPos);
            levelTag.setByteArray("Blocks", chunk.blocks);
            levelTag.setByteArray("Data", chunk.blockDvs.data);
            levelTag.setByteArray("BlockLight", chunk.blockLight.data);
            levelTag.setByteArray("SkyLight", chunk.skyLight.data);
            levelTag.setByteArray("HeightMap", chunk.heightMap);
            levelTag.setBoolean("TerrainPopulated", chunk.populated);

            NBTTagCompound chunkTag = new NBTTagCompound();
            chunkTag.setTag("Level", levelTag);

            CompressedStreamTools.writeCompressed(chunkTag, chunkFile);
        }

        File levelFile = new File(this.worldDirectory, "level.dat");

        if (this.levelData != null) {
            System.out.println("Writing level data...");
            NBTTagCompound dataTag = new NBTTagCompound();
            dataTag.setTag("Player", this.levelData.player);
            dataTag.setLong("LastPlayed", this.levelData.lastPlayed);
            dataTag.setLong("RandomSeed", this.levelData.seed);
            dataTag.setLong("Time", this.levelData.time);
            dataTag.setInteger("SpawnX", this.levelData.spawnX);
            dataTag.setInteger("SpawnY", this.levelData.spawnY);
            dataTag.setInteger("SpawnZ", this.levelData.spawnZ);

            NBTTagCompound levelTag = new NBTTagCompound();
            levelTag.setTag("Data", dataTag);

            CompressedStreamTools.writeCompressed(levelTag, levelFile);
        }

        System.out.println("Calculating new level size...");

        if (levelFile.exists()) {

            long size = Files.walk(this.worldDirectory.toPath())
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".dat") && !p.getFileName().toString().equals("level.dat"))
                    .mapToLong(p -> p.toFile().length())
                    .sum();

            NBTTagCompound levelTag = CompressedStreamTools.readCompressed(levelFile);
            NBTTagCompound dataTag = levelTag.getCompoundTag("Data");
            dataTag.setLong("SizeOnDisk", size);
            CompressedStreamTools.writeCompressed(levelTag, levelFile);
        }

        System.out.println("Write done!");
    }

    @Override
    public AlphaChunk getChunk(int x, int z) {
        for (AlphaChunk c : this.chunks) {
            if (c.xPos == x && c.zPos == z) {
                return c;
            }
        }

        return null;

        /*File f = this.chunkFileForXZ(x, z, false);

        if (!f.exists()) {
            return null;
        }

        NBTTagCompound chunkTag;
        try {
            chunkTag = CompressedStreamTools.read(f);
        }
        catch (Exception e) {
            System.err.println("Failed to read alpha chunk "+x+" "+z+"!");
            e.printStackTrace();
            return null;
        }

        NBTTagCompound levelTag = chunkTag.getCompoundTag("Level");
        AlphaChunk chunk = new AlphaChunk();
        chunk.xPos = levelTag.getInteger("xPos");
        chunk.zPos = levelTag.getInteger("zPos");
        chunk.populated = levelTag.getBoolean("TerrainPopulated");
        chunk.blocks = levelTag.getByteArray("Blocks");
        chunk.blockDvs = new NibbleArray(levelTag.getByteArray("Data"));
        this.chunks.add(chunk);

        return chunk;*/
    }

    @Override
    public void setChunk(int x, int z, AlphaChunk chunk) {
        for (int i = 0; i < this.chunks.size(); i++) {
            AlphaChunk c = this.chunks.get(i);
            if (c.xPos == x && c.zPos == z) {
                this.chunks.set(i, chunk);
                return;
            }
        }
        this.chunks.add(chunk);
    }

    /*@Override
    public void importLevel(LevelFormat from) throws Exception {

    }

    @Override
    public void exportLevel(LevelFormat to) throws Exception {

    }*/

    private File chunkFileForXZ(int x, int z, boolean create) {
        String fileName = "c." + Integer.toString(x, 36) + "." + Integer.toString(z, 36) + ".dat";
        String xName = Integer.toString(x & 63, 36);
        String zName = Integer.toString(z & 63, 36);
        File f = new File(this.worldDirectory, xName);
        if(!f.exists() && create && !f.mkdir()) {
            throw new RuntimeException("Failed to create directory for "+f.getAbsolutePath()+"!");
        }

        f = new File(f, zName);
        if(!f.exists() && create && !f.mkdir()) {
            throw new RuntimeException("Failed to create directory for "+f.getAbsolutePath()+"!");
        }

        f = new File(f, fileName);
        return f;
    }

    @Override
    public void setLevelData(LevelData levelData) {
        this.levelData = levelData;
    }

    @Override
    public LevelData getLevelData() {
        return this.levelData;
    }
}
