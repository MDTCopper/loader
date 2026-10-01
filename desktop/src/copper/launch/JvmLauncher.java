package copper.launch;

import copper.loader.*;
import copper.loader.container.*;
import copper.loader.mod.*;
import copper.loader.util.*;
import java.io.*;

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
            parser.addOption("G", "game-jar", "Game jar or folder class path", "path", path -> JvmPlatform.gameJars.add(new File(path)));
            parser.addOption("D", "game-data", "Game data folder path", "path", path -> JvmPlatform.gameData = new File(path));
            parser.addOption(null, "main", "Custom game main class", "class name");
            parser.addFlag("d", "debug", "Enable debug log output", () -> Log.setLevel(Log.Level.DEBUG));
            parser.addFlag(null, "verbose", "Enable verbose log output", () -> Log.setLevel(Log.Level.VERBOSE));
            parser.addFlag("v", "version", "Display loader version then exit", JvmLauncher::displayVersion);
            parser.addFlag(null, "vanilla", "Load the vanilla game", () -> Loader.vars.vanillaMode = true);
            parser.addFlag(null, "init", "Create data folders then exit");
            parser.addOption(null, "mixin-log", "Enable mixin log for mod", "modId");
            parser.addOption(null, "mixin-flag", "Add mixin flag for mod", "modId,flag1,flag2,...");
            parser.addOption(null, "mod-debug-classpath", "Classpath of the mod to debug", "classpath");
            parser.addOption(null, "mod-debug-jar", "Jar path of the mod to debug", "path");
            parser.parse(args);

            if (JvmPlatform.gameJars.isEmpty())
                throw new RuntimeException("no game jar provided");
            if (JvmPlatform.gameData == null)
                JvmPlatform.gameData = new File(".mindustry");

            Loader.platform = new JvmPlatform();
            if (parser.hasOption("init")) {
                Loader.vars.init();
                System.exit(0);
            }

            if (parser.hasOption("main"))
                Loader.vars.customGameMainClass = parser.getOptionValue("main");
            Loader.init();

            setupMixinOptions(parser);
            setupDebug(parser);

            Loader.launch();

            if (JvmAgent.isAttached())
                JvmAgent.launchDebug();

            Log.info("Launching game.");
            Loader.mods.bootstrap();
            Loader.game.getMainClass()
                    .getDeclaredMethod("main", String[].class)
                    .invoke(null, (Object) parser.getPositionalArgs().toArray(String[]::new));
        } catch (Throwable e) {
            Log.error(e);
            System.exit(1);
        }
    }

    private static void setupMixinOptions(ArgParser parser) {
        for (String id : parser.getOptionValues("mixin-log")) {
            MixinContainer c = findContainer(id);
            if (c instanceof JvmMixinContainer container)
                container.setMixinLogEnabled(true);
        }

        for (String desc : parser.getOptionValues("mixin-flag")) {
            String[] parts = desc.split(",");
            MixinContainer c = findContainer(parts[0]);
            if (c instanceof JvmMixinContainer container) {
                for (int i = 1; i < parts.length; i++)
                    container.addMixinFlag(parts[i].trim());
            }
        }
    }

    private static void setupDebug(ArgParser parser) {
        if (parser.hasOption("mod-debug-jar")) {
            if (parser.hasOption("mod-debug-classpath")) {
                File jar = new File(parser.getOptionValue("mod-debug-jar"));
                File cp = new File(parser.getOptionValue("mod-debug-classpath"));
                Mod mod = Loader.mods.getModByFile(jar);
                if (mod == null) {
                    JvmAgent.dispose();
                    Log.warn("the mod requested to debug is not loaded by loader, ignoring mod debug request");
                } else {
                    JvmAgent.attachDebug(mod, cp);
                }
            } else {
                JvmAgent.dispose();
                Log.warn("no mod debug classpath is provided, ignoring mod debug request");
            }
        } else if (parser.hasOption("mod-debug-classpath")) {
            JvmAgent.dispose();
            Log.warn("no mod debug jar is provided, ignoring mod debug request");
        } else {
            JvmAgent.dispose();
        }
    }

    /**
     * Finds a container by id. Returns the game container for {@code "mindustry"},
     * otherwise looks up a Copper mod by id.
     */
    private static MixinContainer findContainer(String id) {
        if (id.equals("mindustry"))
            return Loader.game.container;
        Mod mod = Loader.mods.getModById(id);
        if (mod == null)
            throw new RuntimeException("mod not found: " + id);
        return mod.container;
    }

    /** Displays the loader version by initializing a minimal platform. */
    private static void displayVersion() {
        Loader.platform = new JvmPlatform();
        Loader.vars = new Vars();
        Loader.vars.init();
        Log.info("CopperLoader " + Loader.vars.loaderVersion.versionLabel());
        System.exit(0);
    }
}
