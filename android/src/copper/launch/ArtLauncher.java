package copper.launch;

import copper.loader.*;
import copper.loader.util.*;
import java.io.*;
import java.nio.charset.*;

/**
 * Boots the loader on a device using the prebuilt dex cache.
 *
 * <p>Called by the launcher app (through its {@code LoaderComponentFactory},
 * before the game activity is instantiated). It validates the cache, boots the
 * loader, and prepares the game data folder. The caller then creates the game
 * activity from {@code Loader.game.getMainClass()}.</p>
 */
public class ArtLauncher {
    public static void main(String[] args) {
        Log.setBackend(Log.Backend::colorless);

        try {
            ArgParser parser = new ArgParser("CopperLoader", "A mindustry loader to load copper mods.");
            parser.addOption("D", "game-data", "Game data folder path", "path", path -> ArtPlatform.gameDataFolder = new File(path));
            parser.addOption("L", "loader-jar", "Loader jar path", "path", path -> ArtPlatform.jarFile = new File(path));
            parser.addOption("C", "cache-path", "Cache path", "path", path -> ArtPlatform.cacheFolder = new File(path));
            parser.addFlag("d", "debug", "Enable debug log output", () -> Log.setLevel(Log.Level.DEBUG));
            parser.addFlag(null, "verbose", "Enable verbose log output", () -> Log.setLevel(Log.Level.VERBOSE));
            parser.addFlag(null, "vanilla", "Load the vanilla game", () -> Loader.vars.vanillaMode = true);
            parser.parse(args);

            checkFileProvided(ArtPlatform.gameDataFolder, "Game data folder");
            checkFileExists(ArtPlatform.jarFile, "Loader jar file");
            checkFileExists(ArtPlatform.cacheFolder, "Cache folder");

            ArtPlatform.init();
            checkFileExists(ArtPlatform.gameAssetFile, "Game assets jar file");
            checkFileExists(ArtPlatform.gameLibFile, "Game libs jar file");

            Loader.platform = new ArtRuntimePlatform();
            ArtRuntimePlatform.dexCache = new DexCache(new File(ArtPlatform.cacheFolder, "dex"));

            Loader.init();
            ArtRuntimePlatform.dexCache.init();
            // the dex for this exact set of mods must have been built before launching
            if (!ArtRuntimePlatform.dexCache.isCurrentRuntimeExisted())
                throw new RuntimeException("runtime cache is not existed, build it first");

            Loader.launch();
            Loader.mods.bootstrap();

            // write the "files moved" markers so the android backend skips its
            // file-moving step (the data folder is already in place)
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
        if (file.exists())
            return;
        try (var fos = new FileOutputStream(file)) {
            fos.write(content.getBytes(StandardCharsets.UTF_8));
        }
    }
}