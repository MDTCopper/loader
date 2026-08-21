package copper.launch;

import copper.loader.*;
import copper.loader.util.*;
import java.io.*;
import java.nio.charset.*;

public class ArtLauncher {
    public static void main(String[] args) {
        Log.setBackend(Log.Backend::colorless);

        try {
            ArgParser parser = new ArgParser("CopperLoader", "A mindustry loader to load copper mods.");
            parser.setPositionalDescription("mindustry args");
            parser.addOption("D", "game-data", "Game data folder path", "path", path -> ArtPlatform.gameDataFolder = new File(path));
            parser.addOption("L", "loader-jar", "Loader jar path", "path", path -> ArtPlatform.jarFile = new File(path));
            parser.addOption("C", "cache-path", "Cache path", "path", path -> ArtPlatform.cacheFolder = new File(path));
            parser.addFlag("d", "debug", "Enable debug log output", () -> Log.setLevel(Log.Level.DEBUG));
            parser.addFlag(null, "verbose", "Enable verbose log output", () -> Log.setLevel(Log.Level.VERBOSE));
            parser.addFlag(null, "vanilla", "Load the vanilla game", () -> Loader.vars.vanillaMode = true);
            parser.parse(args);

            checkFileProvided(ArtPlatform.gameDataFolder, "Game data folder");
            checkFileExists(ArtPlatform.jarFile, "Loader jar file");
            checkFileProvided(ArtPlatform.cacheFolder, "Cache folder");

            ArtPlatform.init();
            Loader.platform = new ArtRuntimePlatform();
            ArtRuntimePlatform.dexCache = new DexCache(new File(ArtPlatform.cacheFolder, "dex"));

            Loader.init();
            ArtRuntimePlatform.dexCache.init();
            if (!ArtRuntimePlatform.dexCache.isCurrentRuntimeExisted())
                throw new RuntimeException("runtime cache is not existed, build it first");

            Loader.launch();
            Loader.mods.bootstrap();

            writeFile(new File(Loader.vars.gameDataFolder, "files_moved"), "files moved");
            writeFile(new File(Loader.vars.gameDataFolder, "files_moved_103"), "files moved again");
        } catch (Throwable e) {
            Log.error(e);
            throw new RuntimeException(e);
        }
    }

    private static void checkFileProvided(File file, String desc) {
        if (file == null)
            throw new RuntimeException(desc + " is not provided");
    }

    private static void checkFileExists(File file, String desc) {
        checkFileProvided(file, desc);
        if (!file.exists())
            throw new RuntimeException(desc + " is not existed: " + file.getAbsolutePath());
    }

    private static void writeFile(File file, String content) throws IOException {
        try (var fos = new FileOutputStream(file)) {
            fos.write(content.getBytes(StandardCharsets.UTF_8));
        }
    }
}
