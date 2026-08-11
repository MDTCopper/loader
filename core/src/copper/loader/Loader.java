package copper.loader;

import copper.loader.mod.*;
import copper.loader.util.*;

/**
 * Central entry point for CopperLoader.
 *
 * <p>Initialization sequence:
 * <ol>
 *   <li>{@link #init()} — create {@link Vars}, {@link Game}, {@link Mods}, then discover and read all mods.</li>
 *   <li>{@link #launch(String[])} — initialize the game, pre-init and init all mods, then launch the game.</li>
 * </ol>
 */
public class Loader {
    public static IPlatform platform;
    public static Game game;
    public static Mods mods;
    public static Vars vars;

    /**
     * Initializes the loader infrastructure: creates vars, game metadata, mod manager, and reads all mods.
     */
    public static void init() {
        vars = new Vars();
        game = new Game();
        mods = new Mods();
        Log.info("CopperLoader v" + vars.loaderVersion.toString());
        Log.info("Game info: " + game.variant.name() + " " + game.type.name() + " " + game.version.toString());
        mods.read();
    }

    /**
     * Launches the loaded game with the given arguments.
     *
     * <p>This first initializes the game container, then pre-inits and inits all mods,
     * and finally hands control to the game's main entry point.</p>
     *
     */
    public static Class<?> launch() {
        Log.info("Launching game.");
        game.init();
        mods.init();
        mods.bootstrap();
        return game.getMainClass();
    }
}
