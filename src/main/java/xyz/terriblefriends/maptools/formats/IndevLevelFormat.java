package xyz.terriblefriends.maptools.formats;

import xyz.terriblefriends.maptools.*;
import xyz.terriblefriends.maptools.nbt.*;
import xyz.terriblefriends.maptools.util.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.*;

public class IndevLevelFormat implements LevelFormat {
    public File levelFile;
    public byte[] blocks = null;
    public byte[] blockDvs = null;

    public int originX = 0;
    public int originZ = 0;

    public int length;
    public int width;
    public int height;
    public boolean floating;

    public LevelData levelData;
    public boolean fixHeight;
    public boolean fixMarkPopulated;
    public boolean fixCloth;
    public boolean warnDangerousBlocks = true;
    public boolean exportLevelInfo;
    public List<AlphaChunk> chunks = new ArrayList<>();

    public List<NBTTagCompound> entities = new ArrayList<>();
    public List<NBTTagCompound> tileEntities = new ArrayList<>();

    private static final HashMap<Integer, Integer> FIX_CLOTH = new HashMap<>();

    public IndevLevelFormat(File levelFile) {
        this.levelFile = levelFile;
    }

    @Override
    public void read() throws Exception {
        try {
            NBTTagCompound rootCompound = CompressedStreamTools.readCompressed(this.levelFile);

            NBTTagCompound mapCompound = rootCompound.getCompoundTag("Map");

            // this isn't intuitive at all, thx notch
            // just gotta trust the process :/
            this.width = mapCompound.getShort("Length");
            this.length = mapCompound.getShort("Width");

            this.height = mapCompound.getShort("Height");

            this.blocks = mapCompound.getByteArray("Blocks");
            this.blockDvs = mapCompound.getByteArray("Data");
            NBTTagList spawnPosList = mapCompound.getTagList("Spawn", 2);

            this.levelData = new LevelData();

            this.levelData.spawnX = spawnPosList.getShortAt(0);
            this.levelData.spawnY = spawnPosList.getShortAt(1);
            this.levelData.spawnZ = spawnPosList.getShortAt(2);

            NBTTagCompound environmentCompound = rootCompound.getCompoundTag("Environment");

            if (!environmentCompound.hasKey("TimeOfDay")) {
                this.levelData.time = 0;
            }
            else {
                // day time was slightly different in indev, so this adjusts it to be close enough to the original time of day
                this.levelData.time = environmentCompound.getShort("TimeOfDay") + 2000;
            }

            // this works for default level types, and probably is close enough if you customized your level too.
            this.floating = environmentCompound.getShort("SurroundingWaterHeight") < 0;

            this.levelData.lastPlayed = rootCompound.getCompoundTag("About").getLong("CreatedOn");

            NBTTagList entityList = rootCompound.getTagList("Entities", 10);

            for (int i = 0; i < entityList.tagCount(); i++) {
                NBTTagCompound entityTag = entityList.getCompoundTagAt(i);
                if (entityTag.getString("id").equals("LocalPlayer")) {
                    this.levelData.player = entityTag;
                }
                this.entities.add(entityTag);
            }

            NBTTagList tileEntityList = rootCompound.getTagList("TileEntities", 10);

            for (int i = 0; i < tileEntityList.tagCount(); i++) {
                this.tileEntities.add(tileEntityList.getCompoundTagAt(i));
            }
        }
        catch (Exception e) {
            System.err.println("Failed to read indev level "+this.levelFile.getAbsolutePath()+"!");
            e.printStackTrace();
        }
    }

    /*@Override
    public void write(File f) throws Exception {

    }*/

    @Override
    public AlphaChunk getChunk(int x, int z) {
        for (AlphaChunk c : this.chunks) {
            if (c.xPos == x && c.zPos == z) {
                return c;
            }
        }

        return null;
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

    }*/

    @Override
    public void exportLevel(LevelFormat to) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        // fix height really only matters for height 64 as otherwise we'd be removing things - obviously we don't want that!
        this.fixHeight = this.height == 64;

        if (this.fixHeight) {
            if (!Arguments.INSTANCE.indevFixHeight.isPresent()) {
                System.out.println("Do you wish to enable the height fix? (Y/N)");
                System.out.println("This will move the world up to y 32.");
                System.out.println("This is recommended on non-deep worlds to allow sea levels to match with new terrain.");

                this.fixHeight = Arguments.askYesNo(reader);
            }
            else {
                this.fixHeight = Arguments.INSTANCE.indevFixHeight.get();
            }
        }

        if (!Arguments.INSTANCE.finiteOrigin.isPresent()) {
            System.out.println("Do you wish to change the origin of the world's placement? (Y/N)");
            System.out.println("This is useful if you wish to combine multiple Classic or Indev saves into one map.");

            if (Arguments.askYesNo(reader)) {
                System.out.println("Please enter the origin chunk X position:");
                while (true) {
                    try {
                        this.originX = Integer.parseInt(reader.readLine());
                        break;
                    } catch (NumberFormatException e) {
                        System.out.println("Not a valid integer!");
                    }
                }
                System.out.println("Please enter the origin chunk Z position:");
                while (true) {
                    try {
                        this.originZ = Integer.parseInt(reader.readLine());
                        break;
                    } catch (NumberFormatException e) {
                        System.out.println("Not a valid integer!");
                    }
                }
            }
        }
        else {
            ChunkPos pos = Arguments.INSTANCE.finiteOrigin.get();
            this.originX = pos.x;
            this.originZ = pos.z;
        }

        if (!Arguments.INSTANCE.exportLevelData.isPresent()) {
            System.out.println("Do you wish to export level info? (Y/N)");
            System.out.println("This is the player, seed, world spawn, and time.");

            this.exportLevelInfo = Arguments.askYesNo(reader);
        }
        else {
            this.exportLevelInfo = Arguments.INSTANCE.exportLevelData.get();
        }

        if (this.exportLevelInfo) {
            if (!Arguments.INSTANCE.seed.isPresent()) {
                System.out.println("Please choose a world seed: (r for random)");
                while (true) {
                    String s2 = reader.readLine().toLowerCase();
                    if (s2.equals("r")) {
                        this.levelData.seed = new Random().nextLong();
                        break;
                    }
                    try {
                        this.levelData.seed = Long.parseLong(s2);
                        break;
                    } catch (NumberFormatException e) {
                        System.out.println("Not a number!");
                    }
                }
            } else {
                this.levelData.seed = Arguments.INSTANCE.seed.get();
            }
        }

        if (!Arguments.INSTANCE.indevFixMarkPopulated.isPresent()) {
            System.out.println("Do you wish to mark all chunks as populated? (Y/N)");
            System.out.println("This will protect most of your world, except for the northern and western most 8 blocks.");
            System.out.println("Will not work for Infdev 20100327.");

            this.fixMarkPopulated = Arguments.askYesNo(reader);
        }
        else {
            this.fixMarkPopulated = Arguments.INSTANCE.indevFixMarkPopulated.get();
        }

        if (!Arguments.INSTANCE.indevFixCloth.isPresent()) {
            System.out.println("Do you wish to convert cloth to wool? (Y/N)");
            System.out.println("This will only convert placed blocks, for the purposes of preserving builds / art.");
            System.out.println("Of course, the color matching is best effort - perfect color matching is impossible.");
            System.out.println("They won't appear visually until Beta 1.2.");

            this.fixCloth = Arguments.askYesNo(reader);
        }
        else {
            this.fixCloth = Arguments.INSTANCE.indevFixCloth.get();
        }

        System.out.println("Converting to chunks...");

        if (this.length % 16 != 0 || this.width % 16 != 0) {
            throw new IllegalStateException("Cannot convert Indev levels with a length, width, or height that is not a multiple of 16! Sorry :(");
        }

        if (this.height == 256) {
            throw new UnsupportedOperationException("Upgrading levels with a height of 256 is not yet supported!");
        }

        // init new alpha chunks
        AlphaChunk[][] chunkArray = new AlphaChunk[this.length / 16][this.width / 16];

        for (int chunkX = 0; chunkX < this.length / 16; chunkX++) {
            for (int chunkZ = 0; chunkZ < this.width / 16; chunkZ++) {
                AlphaChunk chunk = new AlphaChunk();
                chunk.xPos = chunkX + this.originX;
                chunk.zPos = chunkZ + this.originZ;

                chunkArray[chunkX][chunkZ] = chunk;
                this.chunks.add(chunk);
            }
        }

        // get blocks from indev and copy into new chunks
        int blockX = 0;
        int blockY = this.fixHeight ? 32 : 0;
        int blockZ = 0;

        for (int blockIndex = 0; blockIndex < this.blocks.length; blockIndex++) {
            AlphaChunk c = chunkArray[blockX / 16][blockZ / 16];

            int id = this.blocks[blockIndex];
            int dv = this.blockDvs[blockIndex] >> 4;
            int light = this.blockDvs[blockIndex] & 15;

            // wool conversion dvs debug
            /*if (id != 0) {
                id = 35;
            }*/

            if (this.fixCloth && FIX_CLOTH.containsKey(id)) {
                dv = FIX_CLOTH.get(id);
                id = 35;
            }

            if (this.warnDangerousBlocks && ((!this.fixCloth && FIX_CLOTH.containsKey(id) && id != 35) || id == 52 || id == 53 || id == 55)) {
                System.out.println("WARNING: You have dangerous blocks placed in your world!");
                System.out.println("These are blocks that had their IDs removed in Infdev 20100624, and were later recycled.");
                System.out.println("More specifically, this is cloth, infinite water, infinite lava, and gears.");
                System.out.println("Loading them in versions where they don't exist will softlock you until you update to a version where they exist.");
                System.out.println("You can safely store them in chests, as long as you don't open them.");
                System.out.println("This message won't repeat for every block, but here's the first that triggered it:");
                System.out.printf("X: %d Y: %d Z: %d%n", blockX, blockY, blockZ);
                this.warnDangerousBlocks = false;
            }

            c.setBlock(blockX % 16, blockY, blockZ % 16, id, dv);
            c.setBlockLight(blockX % 16, blockY, blockZ % 16, light);

            if (++blockX == this.length) {
                blockX = 0;
                if (++blockZ == this.width) {
                    blockZ = 0;
                    blockY++;
                }
            }
        }

        if (this.fixHeight) {
            int fillId = this.floating ? 0 : 1;
            for (blockX = 0; blockX < this.length; blockX++) {
                for (blockY = 0; blockY < 32; blockY++) {
                    for (blockZ = 0; blockZ < this.width; blockZ++) {
                        AlphaChunk c = chunkArray[blockX / 16][blockZ / 16];
                        c.setBlock(blockX % 16, blockY, blockZ % 16, fillId, 0);
                    }
                }
            }
        }

        // recalculate height map
        for (AlphaChunk chunk : this.chunks) {
            chunk.calculateHeightmap();

            if (this.fixMarkPopulated) {
                chunk.populated = true;
            }
        }

        System.out.println("Converting entities...");
        for (NBTTagCompound entity : this.entities) {
            // fix pos and motion from float to double for all entities
            NBTTagList posList = entity.getTagList("Pos", 5);
            double entityX = posList.getFloatAt(0) + (this.originX * 16);
            double entityY = posList.getFloatAt(1) + (this.fixHeight ? 32 : 0);
            double entityZ = posList.getFloatAt(2) + (this.originZ * 16);
            entity.removeTag("Pos");
            NBTTagList newPosList = new NBTTagList();
            newPosList.appendTag(new NBTTagDouble(entityX));
            newPosList.appendTag(new NBTTagDouble(entityY));
            newPosList.appendTag(new NBTTagDouble(entityZ));
            entity.setTag("Pos", newPosList);

            NBTTagList motionList = entity.getTagList("Motion", 5);
            double motionX = motionList.getFloatAt(0);
            double motionY = motionList.getFloatAt(1);
            double motionZ = motionList.getFloatAt(2);
            entity.removeTag("Motion");
            NBTTagList newMotionList = new NBTTagList();
            newMotionList.appendTag(new NBTTagDouble(motionX));
            newMotionList.appendTag(new NBTTagDouble(motionY));
            newMotionList.appendTag(new NBTTagDouble(motionZ));
            entity.setTag("Motion", newMotionList);

            String id = entity.getString("id");

            if (id.equals("LocalPlayer")) {
                // some old indev versions don't store the player's health, so set it to 20 so they don't die instantly if it's missing
                if (!entity.hasKey("Health")) {
                    entity.setShort("Health", (short)20);
                }
                this.levelData.player = entity;
                // skip other entity processing
                continue;
            }

            if (id.equals("Painting")) {
                // paintings store the block they are attached to, so fix it
                int x = entity.getInteger("TileX") + (this.originX * 16);
                int y = entity.getInteger("TileY") + (this.fixHeight ? 32 : 0);
                int z = entity.getInteger("TileZ") + (this.originZ * 16);
                entity.setInteger("TileX", x);
                entity.setInteger("TileY", y);
                entity.setInteger("TileZ", z);
            }

            // put the entity into its respective chunk

            int chunkX = MathHelper.floor(entityX / 16);
            int chunkZ = MathHelper.floor(entityZ / 16);

            if (id.equals("Painting")) {
                System.out.println("[DEBUG] entity pos "+entityX+" "+entityZ);
                System.out.println("[DEBUG] entity chunk "+chunkX+" "+chunkZ);
            }

            AlphaChunk c = this.getChunk(chunkX, chunkZ);

            if (c != null) {
                c.entities.appendTag(entity);
            }
        }

        System.out.println("Converting tile entities...");
        for (NBTTagCompound tileEntity : this.tileEntities) {
            // unpack position
            int posPacked = tileEntity.getInteger("Pos");
            int tileX = posPacked % 1024 + (this.originX * 16);
            int tileY = (posPacked >> 10) % 1024 + (this.fixHeight ? 32 : 0);
            int tileZ = (posPacked >> 20) % 1024 + (this.originZ * 16);
            tileEntity.removeTag("Pos");
            tileEntity.setInteger("x", tileX);
            tileEntity.setInteger("y", tileY);
            tileEntity.setInteger("z", tileZ);

            int chunkX = MathHelper.floor(tileX / 16d);
            int chunkZ = MathHelper.floor(tileZ / 16d);

            //System.out.println("[DEBUG] entity chunk "+chunkX+" "+chunkZ+" "+tileX+" "+tileZ);

            AlphaChunk c = this.getChunk(chunkX, chunkZ);

            if (c != null) {
                c.tileEntities.appendTag(tileEntity);
            }
        }

        System.out.println("Writing to new format...");

        // set chunks in new format
        for (AlphaChunk c : this.chunks) {
            to.setChunk(c.xPos, c.zPos, c);
        }

        if (this.exportLevelInfo) {
            NBTTagList inventory = this.levelData.player.getTagList("Inventory", 10);
            for (int i = 0; i < inventory.tagCount(); i++) {
                int id = inventory.getCompoundTagAt(i).getShort("id");
                if ((FIX_CLOTH.containsKey(id) && id != 35) || id == 52 || id == 53 || id == 55) {
                    System.out.println("WARNING: You have dangerous blocks in your inventory!");
                    System.out.println("These are blocks that had their IDs removed in Infdev 20100624, and were later recycled.");
                    System.out.println("More specifically, this is cloth, infinite water, infinite lava, and gears.");
                    System.out.println("Loading them in versions where they don't exist will softlock you until you update to a version where they exist.");
                    System.out.println("You can safely store them in chests, as long as you don't open them.");
                    break;
                }
            }

            to.setLevelData(this.levelData);
        }

        to.write();
    }

    @Override
    public void setLevelData(LevelData levelData) {
        this.levelData = levelData;
    }

    @Override
    public LevelData getLevelData() {
        return this.levelData;
    }

    private int getBlockIndex(int x, int y, int z) {
        return x + (z * this.length) + (y * this.length * this.width);
    }

    static {
        FIX_CLOTH.put(21, 14);
        FIX_CLOTH.put(22, 1);
        FIX_CLOTH.put(23, 4);
        FIX_CLOTH.put(24, 5);
        FIX_CLOTH.put(25, 5);
        FIX_CLOTH.put(26, 5);
        FIX_CLOTH.put(27, 3);
        FIX_CLOTH.put(28, 9);
        FIX_CLOTH.put(29, 11);
        FIX_CLOTH.put(30, 11);
        FIX_CLOTH.put(31, 10);
        FIX_CLOTH.put(32, 2);
        FIX_CLOTH.put(33, 14);
        FIX_CLOTH.put(34, 7);
        FIX_CLOTH.put(35, 8);
        FIX_CLOTH.put(36, 0);
    }
}
