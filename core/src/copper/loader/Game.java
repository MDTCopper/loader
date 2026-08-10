package copper.loader;

import copper.loader.container.*;
import copper.loader.container.info.*;
import copper.loader.mod.*;
import java.io.*;
import java.nio.charset.*;
import java.util.*;

public class Game {
    public Container container;
    // number.build.revision
    // number.0.beNum
    // number.0.0
    public SemanticVersion version;
    public Type type;
    public Variant variant;

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
            byte[] manifestBytes = container.resource.get("META-INF/MANIFEST.MF");
            if (manifestBytes != null) {
                String manifest = new String(manifestBytes, StandardCharsets.UTF_8);
                for (Variant v : Variant.values()) {
                    if (v.mainClass.isEmpty())
                        continue;
                    if (manifest.contains(v.mainClass))
                        variant = v;
                }
            }
        } catch (Throwable e) {
            throw new RuntimeException("failed to read game", e);
        }
    }

    public void init() {
        try {
            container.init();
        } catch (Throwable e) {
            throw new RuntimeException("failed to init game", e);
        }
    }

    public void launch(String[] args) {
        if (variant == Variant.Unknown)
            throw new RuntimeException("failed to launch unknown variant of game");
        Class<?> main = container.loadOwnClass(variant.mainClass);
        try {
            main.getDeclaredMethod("main", String[].class).invoke(null, (Object) args);
        } catch (Throwable e) {
            throw new RuntimeException("failed to launch game", e);
        }
    }

    public enum Type {
        Release,
        BleedingEdge,
        CustomBuild
    }

    public enum Variant {
        Desktop("mindustry.desktop.DesktopLauncher"),
        Server("mindustry.server.ServerLauncher"),
        Android("mindustry.android.AndroidLauncher"),
        IOS("mindustry.ios.IOSLauncher"),
        Unknown("")
        ;

        public final String mainClass;

        Variant(String mainClass) {
            this.mainClass = mainClass;
        }
    }
}
