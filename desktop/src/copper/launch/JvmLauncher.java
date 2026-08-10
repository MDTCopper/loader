package copper.launch;

import copper.loader.*;
import copper.loader.container.*;
import copper.loader.mod.*;
import copper.loader.util.*;
import java.io.*;
import java.util.*;

/**
 * JVM (desktop) launcher entry point for CopperLoader.
 *
 * <p>Parses command-line arguments, initializes the loader platform and mod
 * system, then hands control to the game. Supports debug/version flags and
 * per-mod mixin configuration options. Arguments after {@code --} are forwarded
 * to the game.</p>
 */
public class JvmLauncher {
    public static void main(String[] args) {
        if (System.console() == null)
            Log.setBackend(Log.Backend::colorless);

        try {
            ArgParser parser = new ArgParser("CopperLoader", "A mindustry loader to load copper mods.");
            parser.setPositionalDescription("mindustry args");
            parser.addOption("G", "game-jar", "Game jar file path", "path", path -> JvmPlatform.gameJar = new File(path));
            parser.addOption("D", "game-data", "Game data folder path", "path", path -> JvmPlatform.gameData = new File(path));
            parser.addFlag("d", "debug", "Enable debug log output", () -> Log.setLevel(Log.Level.DEBUG));
            parser.addFlag("v", "version", "Display loader version", JvmLauncher::displayVersion);
            parser.addOption(null, "mixin-log", "Enable mixin log for mod", "modId");
            parser.addOption(null, "mixin-flag", "Add mixin flag for mod", "modId,flag1,flag2,...");
            parser.parse(args);

            if (JvmPlatform.gameJar == null)
                throw new RuntimeException("no game jar provided");

            Loader.platform = new JvmPlatform();
            Loader.init();

            for (String id : parser.getOptionValues("mixin-log")) {
                Container c = findContainer(id);
                if (c instanceof JvmContainer container)
                    container.setMixinLogEnabled(true);
            }

            for (String desc : parser.getOptionValues("mixin-flag")) {
                String[] parts = desc.split(",");
                Container c = findContainer(parts[0]);
                if (c instanceof JvmContainer container) {
                    for (int i = 1; i < parts.length; i++)
                        container.addMixinFlag(parts[i].trim());
                }
            }

            Loader.launch(parser.getPositionalArgs().toArray(String[]::new));
        } catch (Throwable e) {
            Log.error(e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Finds a container by id. Returns the game container for {@code "mindustry"},
     * otherwise looks up a Copper mod by id.
     */
    private static Container findContainer(String id) {
        if (id.equals("mindustry"))
            return Loader.game.container;
        Mod mod = Loader.mods.getModById(id);
        if (mod == null)
            throw new RuntimeException("mod not found: " + id);
        return mod.container;
    }

    /** Displays the loader version by initializing a minimal platform. */
    private static void displayVersion() {
        JvmPlatform.gameJar = new File("");
        Loader.platform = new JvmPlatform();
        Loader.vars = new Vars();
        Log.info("CopperLoader v" + Loader.vars.loaderVersion.toString());
        System.exit(0);
    }
}
