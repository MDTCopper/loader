package copper.loader;

import copper.loader.container.*;
import copper.loader.mod.*;
import copper.loader.util.*;

import java.io.*;
import java.nio.charset.*;
import java.util.*;

/**
 * Holds all runtime paths and loader metadata.
 */
public class Vars {
    /** The game's data directory (e.g., {@code ~/.mindustry}). */
    public File gameDataFolder;
    /** The Copper loader's data directory ({@code {gameData}/copper}). */
    public File loaderDataFolder;
    /** Mindustry's standard mod folder ({@code {gameData}/mods}). */
    public File gameModFolder;
    /** Copper-native mod folder ({@code {gameData}/copper/mods}). */
    public File copperModFolder;
    /** Copper mod data folder ({@code {gameData}/copper/datas}). */
    public File copperModDataFolder;

    /** Container representing the loader's own classes. */
    public Container loaderContainer;
    /** The installed loader version, read from {@code version.properties}. */
    public SemanticVersion loaderVersion;

    /** When {@code true}, loads the vanilla game without the copper core mod. */
    public boolean vanillaMode = false;

    /** When {@code false}, skips writing logs to the file. */
    public boolean writeFileLog = true;

    /** Whether {@link #init()} has already run. */
    private boolean inited = false;

    /**
     * Sets up all runtime folders, the loader container, and the loader version.
     * Only does the work once: later calls are ignored.
     */
    public void init() {
        if (inited)
            return;

        gameDataFolder = Loader.platform.getGameDataFolder();
        loaderDataFolder = new File(gameDataFolder, "copper");
        gameModFolder = new File(gameDataFolder, "mods");
        copperModFolder = new File(loaderDataFolder, "mods");
        copperModDataFolder = new File(loaderDataFolder, "datas");

        gameModFolder.mkdirs();
        copperModFolder.mkdirs();
        copperModDataFolder.mkdirs();

        loaderContainer = Loader.platform.createLoaderContainer();
        loaderContainer.id = "loader";
        loaderContainer.export.addRule("include *");
        loaderContainer.init();

        try {
            String ver = new String(loaderContainer.resource.get("version.properties"), StandardCharsets.UTF_8);
            Properties properties = new Properties();
            properties.load(new StringReader(ver));
            loaderVersion = new SemanticVersion(properties.getProperty("version", "0.0.0"));
        } catch (Throwable e) {
            throw new RuntimeException("failed to read loader version", e);
        }
        inited = true;
    }

    /** Starts the file logger, unless it has been disabled. */
    public void setupFileLogger() {
        if (writeFileLog)
            Log.setOutputFile(new File(gameDataFolder, "last_log.txt"));
    }
}
