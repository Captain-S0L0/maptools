package xyz.terriblefriends.maptools;

import xyz.terriblefriends.maptools.formats.AlphaLevelFormat;
import xyz.terriblefriends.maptools.formats.IndevLevelFormat;
import xyz.terriblefriends.maptools.formats.LevelFormat;
import xyz.terriblefriends.maptools.formats.LevelFormats;
import xyz.terriblefriends.maptools.util.Operation;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Locale;

public class Main {
    public static void main(String[] args) {
        try {
            Arguments.INSTANCE.processArgs(args);

            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

            if (Arguments.INSTANCE.operation == Operation.NOOP) {
                System.out.println("No operation selected! Please choose one:");
                System.out.println("Recognized options are: convert, edit");

                while (true) {
                    String option = reader.readLine().toLowerCase(Locale.ROOT);
                    if (option.equals("convert")) {
                        Arguments.INSTANCE.operation = Operation.CONVERT;
                        break;
                    }
                    if (option.equals("edit")) {
                        Arguments.INSTANCE.operation = Operation.EDIT;
                        break;
                    }
                    System.out.println("Unrecognized option!");
                }
            }

            if (Arguments.INSTANCE.inFile == null || Arguments.INSTANCE.inFormat == LevelFormats.FILENOTFOUND) {
                System.out.println("No input world selected! Please enter the path:");
                System.out.println("(can be absolute or relative to current working directory)");

                while (true) {
                    Arguments.INSTANCE.inFile = new File(reader.readLine());

                    Arguments.INSTANCE.inFormat = Arguments.INSTANCE.determineFormat(Arguments.INSTANCE.inFile);

                    if (Arguments.INSTANCE.inFormat == LevelFormats.FILENOTFOUND) {
                        System.out.println("No file found!");
                    }
                    else {
                        break;
                    }
                }
            }

            if (Arguments.INSTANCE.inFormat == LevelFormats.INVALID) {
                System.out.println("WARNING: No valid format was detected for the input world!");
                System.out.println("You can manually input one here if you know what you're doing, but you should probably check the path you specified first.");
                System.out.println("Recognized options: classic, indev, alpha, mcregion, mcanvil");

                do {
                    Arguments.INSTANCE.inFormat = Arguments.INSTANCE.stringToFormat(reader.readLine().toLowerCase(Locale.ROOT));
                } while (Arguments.INSTANCE.inFormat == LevelFormats.INVALID);
            }

            if (Arguments.INSTANCE.operation == Operation.CONVERT) {
                if (Arguments.INSTANCE.outFile == null) {
                    System.out.println("No output world selected! Please enter the path:");
                    System.out.println("(can be absolute or relative to current working directory)");

                    Arguments.INSTANCE.outFile = new File(reader.readLine());

                    if (Arguments.INSTANCE.outFile.exists()) {
                        System.out.println("WARNING: Output world exists! Overwriting may occur!");
                        System.out.println("Proceed? (Y/N)");

                        if (!Arguments.askYesNo(reader)) {
                            return;
                        }
                    }

                    Arguments.INSTANCE.outFormat = Arguments.INSTANCE.determineFormat(Arguments.INSTANCE.outFile);
                }

                if (Arguments.INSTANCE.outFormat == LevelFormats.INVALID || Arguments.INSTANCE.outFormat == LevelFormats.FILENOTFOUND) {
                    System.out.println("Choose the format of the output world:");
                    System.out.println("Recognized options: classic, indev, alpha, mcregion, mcanvil");

                    while (true) {
                        Arguments.INSTANCE.outFormat = Arguments.INSTANCE.stringToFormat(reader.readLine().toLowerCase(Locale.ROOT));

                        if (Arguments.INSTANCE.outFormat != LevelFormats.INVALID) {
                            break;
                        }
                    }
                }
            }

            LevelFormat inputLevel = getLevelClass(Arguments.INSTANCE.inFormat, Arguments.INSTANCE.inFile);

            inputLevel.read();

            LevelFormat outputLevel = getLevelClass(Arguments.INSTANCE.outFormat, Arguments.INSTANCE.outFile);

            outputLevel.read();

            inputLevel.exportLevel(outputLevel);
        }
        catch (Exception e) {
            System.err.println("Uncaught error in main thread!");
            e.printStackTrace();
        }
    }

    private static LevelFormat getLevelClass(LevelFormats format, File f) {
        switch (format) {
            case INDEV:
                return new IndevLevelFormat(f);
            case ALPHA:
                return new AlphaLevelFormat(f);
            default:
                throw new UnsupportedOperationException("Unsupported level format!");
        }
    }
}
