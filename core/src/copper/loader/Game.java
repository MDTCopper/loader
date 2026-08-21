package copper.loader;

import copper.loader.container.*;
import copper.loader.container.info.*;
import copper.loader.mod.*;
import java.io.*;
import java.nio.charset.*;
import java.util.*;

/**
 * Holds information about the game being loaded (Mindustry).
 *
 * <p>On construction, the game version is read from {@code version.properties}
 * and the platform variant (Desktop, Server, Android, iOS) is detected by
 * probing the game container for the variant's main class bytecode.
 * Version format varies by game release type (Release, Bleeding-Edge, or Custom).</p>
 */
public class Game {
    public MixinContainer container;
    public SemanticVersion version;
    public Type type;
    public Variant variant;

    /**
     * Creates the game descriptor by scanning the game jar for version and variant info.
     */
    public Game() {
        container = Loader.platform.createGameContainer();
        container.id = "mindustry";
        container.export.addRule("include *");
        container.dependency.add(new DependencyInfo(Loader.vars.loaderContainer));

        try {
            String ver = new String(container.resource.get("version.properties"), StandardCharsets.UTF_8);
            Properties properties = new Properties();
            properties.load(new StringReader(ver));
            String verType = properties.getProperty("type");
            String verNumber = properties.getProperty("number", "0");
            String verBuild = properties.getProperty("build", "0");
            if (verType.equals("official")) {
                version = new SemanticVersion(verNumber + "." + verBuild);
                type = Type.Release;
            } else if (verType.equals("bleeding-edge")) {
                version = new SemanticVersion(verNumber + ".0." + verBuild);
                type = Type.BleedingEdge;
            } else {
                version = new SemanticVersion(verNumber);
                type = Type.CustomBuild;
            }

            variant = Variant.Unknown;
            for (Variant v : Variant.values()) {
                if (v.mainClass.isEmpty())
                    continue;
                if (container.getOwnBytecode(v.mainClass) == null)
                    continue;
                variant = v;
                break;
            }
        } catch (Throwable e) {
            throw new RuntimeException("failed to read game", e);
        }
    }

    /**
     * Initializes the game container (prepares classloader, sets up mixin engine).
     */
    public void init() {
        try {
            container.init();
        } catch (Throwable e) {
            throw new RuntimeException("failed to init game", e);
        }
    }

    /**
     * Finds the game's main class for the detected variant.
     *
     * @return the main class
     * @throws RuntimeException if the variant is unknown or the main class is not found
     */
    public Class<?> getMainClass() {
        if (variant == Variant.Unknown)
            throw new RuntimeException("failed to get main class of unknown variant of game");
        Class<?> main = container.loadOwnClass(variant.mainClass);
        if (main == null)
            throw new RuntimeException("failed to find game main class: " + variant.mainClass);
        return main;
    }

    /** Game release type. */
    public enum Type {
        Release,
        BleedingEdge,
        CustomBuild
    }

    /** Known game platform variants, each identified by its main class name. */
    public enum Variant {
        Android("mindustry.android.AndroidLauncher"),
        IOS("mindustry.ios.IOSLauncher"),
        Desktop("mindustry.desktop.DesktopLauncher"),
        Server("mindustry.server.ServerLauncher"),
        Unknown("")
        ;

        /** The fully qualified main class name for this variant. */
        public final String mainClass;

        Variant(String mainClass) {
            this.mainClass = mainClass;
        }
    }
}
