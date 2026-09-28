package copper.loader;

import copper.loader.container.*;
import copper.loader.util.*;
import java.io.*;
import java.nio.charset.*;

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
    /**
     * The installed loader version and what identifies this build, read from {@code version.properties}.
     * Always numeric for mod dependency checks; {@link LoaderVersion#versionLabel()} is the readable form.
     */
    public LoaderVersion loaderVersion;

    /** When {@code true}, loads the vanilla game without the non-hidden copper core mod. */
    public boolean vanillaMode = false;
    /** When {@code false}, skips writing logs to the file. */
    public boolean writeFileLog = true;
    /** When non {@code null}, uses it as the main class of the game (e.g., {@code com.example.MyClass}). */
    public String customGameMainClass = null;

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

        if (gameDataFolder != null) {
            gameModFolder.mkdirs();
            copperModFolder.mkdirs();
            copperModDataFolder.mkdirs();
        }

        loaderContainer = Loader.platform.createLoaderContainer();
        loaderContainer.id = "loader";
        loaderContainer.export.addRule("include *");
        loaderContainer.init();

        try {
            String ver = new String(loaderContainer.resource.get("version.properties"), StandardCharsets.UTF_8);
            loaderVersion = new LoaderVersion(ver);
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
