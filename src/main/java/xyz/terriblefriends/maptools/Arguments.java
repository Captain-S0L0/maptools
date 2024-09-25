package xyz.terriblefriends.maptools;

import xyz.terriblefriends.maptools.formats.LevelFormats;
import xyz.terriblefriends.maptools.nbt.CompressedStreamTools;
import xyz.terriblefriends.maptools.nbt.NBTTagCompound;
import xyz.terriblefriends.maptools.util.ChunkPos;
import xyz.terriblefriends.maptools.util.Operation;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Locale;
import java.util.Optional;

public class Arguments {

    public static final Arguments INSTANCE = new Arguments();

    public File inFile = null;
    public File outFile = null;
    public LevelFormats inFormat = LevelFormats.INVALID;
    public LevelFormats outFormat = LevelFormats.INVALID;
    public Operation operation = Operation.NOOP;
    public Optional<Boolean> indevFixHeight = Optional.empty();
    public Optional<Long> seed = Optional.empty();
    public Optional<Boolean> indevFixMarkPopulated = Optional.empty();
    public Optional<ChunkPos> finiteOrigin = Optional.empty();
    public Optional<Boolean> exportLevelData = Optional.empty();
    public Optional<Boolean> indevFixCloth = Optional.empty();

    private static final String STRING_NO_ARGUMENT = "No argument!";

    public void processArgs(String[] args) {

        Iterator<String> argumentsIterator = Arrays.stream(args).iterator();

        while (argumentsIterator.hasNext()) {
            String arg = argumentsIterator.next();

            if (arg.startsWith("-")) {
                for (char c : arg.substring(1).toCharArray()) {
                    String s;

                    switch (c) {
                        case 'h':
                        case 'H':
                        case '?':
                            this.printUsage();
                            break;
                        case 'i':
                            if (!argumentsIterator.hasNext()) {
                                System.out.println(STRING_NO_ARGUMENT);
                                break;
                            }
                            this.inFile = new File(argumentsIterator.next());
                            this.inFormat = this.determineFormat(this.inFile);
                            break;
                        case 'I':
                            if (!argumentsIterator.hasNext()) {
                                System.out.println(STRING_NO_ARGUMENT);
                                break;
                            }
                            this.inFormat = this.stringToFormat(argumentsIterator.next().toLowerCase(Locale.ROOT));
                            break;
                        case 'o':
                            if (!argumentsIterator.hasNext()) {
                                System.out.println(STRING_NO_ARGUMENT);
                                break;
                            }
                            this.outFile = new File(argumentsIterator.next());
                            this.outFormat = this.determineFormat(this.outFile);
                            break;
                        case 'O':
                            if (!argumentsIterator.hasNext()) {
                                System.out.println(STRING_NO_ARGUMENT);
                                break;
                            }
                            this.outFormat = this.stringToFormat(argumentsIterator.next().toLowerCase(Locale.ROOT));
                            break;
                        case 'c':
                            this.operation = Operation.CONVERT;
                            break;
                        case 'e':
                            this.operation = Operation.EDIT;
                            break;
                        case 'w':
                            this.indevFixHeight = Optional.of(true);
                            break;
                        case 'W':
                            this.indevFixHeight = Optional.of(false);
                            break;
                        case 'p':
                            this.indevFixMarkPopulated = Optional.of(true);
                            break;
                        case 'P':
                            this.indevFixMarkPopulated = Optional.of(false);
                            break;
                        case 's':
                            if (!argumentsIterator.hasNext()) {
                                System.out.println(STRING_NO_ARGUMENT);
                                break;
                            }
                            try {
                                this.seed = Optional.of(Long.parseLong(argumentsIterator.next()));
                            }
                            catch (NumberFormatException e) {
                                System.out.println("Argument was not a number!");
                            }
                            break;
                        case 'g':
                            if (!argumentsIterator.hasNext()) {
                                System.out.println(STRING_NO_ARGUMENT);
                                break;
                            }
                            ChunkPos pos = new ChunkPos();
                            try {
                                pos.x = Integer.parseInt(argumentsIterator.next());

                                if (!argumentsIterator.hasNext()) {
                                    System.out.println(STRING_NO_ARGUMENT);
                                    break;
                                }

                                pos.z = Integer.parseInt(argumentsIterator.next());
                                this.finiteOrigin = Optional.of(pos);
                            }
                            catch (NumberFormatException e) {
                                System.out.println("Argument was not a number!");
                            }
                            break;
                        case 'l':
                            this.exportLevelData = Optional.of(true);
                            break;
                        case 'L':
                            this.exportLevelData = Optional.of(false);
                            break;
                        case 'a':
                            this.indevFixCloth = Optional.of(true);
                            break;
                        case 'A':
                            this.indevFixCloth = Optional.of(false);
                            break;
                        default:
                            System.out.println("Unrecognized flag "+c+"!");
                            break;
                    }
                }
            }
            else if (arg.startsWith("--")) {
                // TODO support verbose arguments
                switch (arg.substring(2)) {
                    case "help":
                        this.printUsage();
                        break;

                    default:
                        System.out.println("Unrecognized argument " + arg);
                        break;
                }
            }
        }
    }

    private void printUsage() {
        // free b d f j k m n q r t u v x y z

        System.out.println("Usage:");
        System.out.println();
        System.out.println("Specify path of worlds:");
        System.out.println("(should either be level.dat for Classic, <name>.mclevel for Indev, or the world folder for Alpha, McRegion, or Anvil.)");
        System.out.println("  -i <path> : input world");
        System.out.println("  -o <path> : where to write the world to when converting. doesn't need to exist");
        System.out.println();
        System.out.println("Manually specify the format of the world, in case automatic parsing fails:");
        System.out.println("  -I <classic/indev/alpha/mcregion/mcanvil> : format of input world");
        System.out.println("  -O <classic/indev/alpha/mcregion/mcanvil> : format of world to write when converting");
        System.out.println();
        System.out.println("Operations:");
        System.out.println("  -e : edit things about the world");
        System.out.println();
        System.out.println("  -c : convert");
        System.out.println();
        System.out.println("    Indev height fix:");
        System.out.println("    Moves the world up so sea level matches with new terrain. Only available for non-deep worlds.");
        System.out.println("      - w / W : enable / disable");
        System.out.println("    Indev population fix:");
        System.out.println("    Marks all generated chunks as populated, protecting all but the northernmost and westernmost 8 blocks of your world.");
        System.out.println("    Will not work for Infdev 20100327.");
        System.out.println("      - p / P : enable / disable");
        System.out.println("    - g <x> <z> : specify the north west origin of where to place finite levels, in chunk position.");
        System.out.println("    - s : specify seed, if applicable (for classic or indev levels which don't store a seed)");
        System.out.println("    Export level data: export the player, world spawn, seed, and world time. Useful for merging multiple worlds.");
        System.out.println("      - l / L : enable / disable");
        System.out.println("    Indev cloth fix:");
        System.out.println("    Cloth was removed and later replaced with wool, so this tries to best color match placed old cloth blocks to wool.");
        System.out.println("    Will not appear visually until Beta 1.2.");
        System.out.println("      - a / A : enable / disable");

    }

    public LevelFormats stringToFormat(String s) {
        LevelFormats r;
        switch (s) {
            case "classic":
                r = LevelFormats.CLASSIC;
                break;
            case "indev":
                r = LevelFormats.INDEV;
                break;
            case "alpha":
                r = LevelFormats.ALPHA;
                break;
            case "mcregion":
                r = LevelFormats.MCREGION;
                break;
            case "mcanvil":
                r = LevelFormats.MCANVIL;
                break;
            default:
                r = LevelFormats.INVALID;
                System.out.println("Unrecognized format "+s+"!");
                break;
        }
        return r;
    }

    public LevelFormats determineFormat(File f) {
        if (!f.exists()) {
            //System.out.println("[DEBUG] file does not exist");
            return LevelFormats.FILENOTFOUND;
        }

        if (f.isDirectory()) {
            //System.out.println("[DEBUG] file is directory");
            File levelFile = new File(f, "level.dat");

            if (!levelFile.exists()) {
                //System.out.println("[DEBUG] level.dat missing");
                return LevelFormats.INVALID;
            }

            NBTTagCompound levelTag;
            try {
                levelTag = CompressedStreamTools.readCompressed(levelFile);

                int version = levelTag.getCompoundTag("Data").getInteger("version");
                //System.out.println("[DEBUG] data version is "+version);

                switch (version) {
                    case 0:
                        return LevelFormats.ALPHA;
                    case 19132:
                        return LevelFormats.MCREGION;
                    case 19133:
                        return LevelFormats.MCANVIL;
                    default:
                        return LevelFormats.INVALID;
                }
            }
            catch (Exception e) {
                //System.err.println("Error reading level.dat!");
                e.printStackTrace();
                return LevelFormats.INVALID;
            }
        }
        else {
            //System.out.println("[DEBUG] file is normal file");
            String extension = Arrays.stream(f.getName().split("\\.")).reduce((a, b) -> b).orElse("");

            //System.out.println("[DEBUG] extension is "+extension);
            if (extension.equals("mclevel")) {
                return LevelFormats.INDEV;
            }
            if (extension.equals("dat")) {
                return LevelFormats.CLASSIC;
            }

            return LevelFormats.INVALID;
        }
    }

    public static boolean askYesNo(BufferedReader reader) throws IOException {
        while (true) {
            String s = reader.readLine().toLowerCase(Locale.ROOT);
            if (s.equals("y")) {
                return true;
            }
            if (s.equals("n")) {
                return false;
            }
            System.out.println("Unrecognized option!");
        }
    }
}
