package copper.loader;

import copper.loader.util.*;

public class Loader {
    public static IPlatform platform;
    public static Game game;
    public static Mods mods;
    public static Vars vars;

    public static void init() {
        vars = new Vars();
        game = new Game();
        mods = new Mods();
        Log.info("CopperLoader v" + vars.loaderVersion.toString());
        Log.info("Game info: " + game.variant.name() + " " + game.type.name() + " " + game.version.toString());
        mods.read();
    }

    public static void launch(String[] args) {
        Log.info("Launching game.");
        game.init();
        mods.preInit();
        mods.init();
        game.launch(args);
    }
}
