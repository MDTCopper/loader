package copper.loader;

import copper.loader.container.*;
import copper.loader.mod.*;
import java.io.*;
import java.nio.charset.*;
import java.util.*;

public class Vars {
    public File gameDataFolder;
    public File loaderDataFolder;
    public File gameModFolder;
    public File copperModFolder;
    public File copperModDataFolder;

    public Container loaderContainer;
    public final SemanticVersion loaderVersion;

    public Vars() {
        gameDataFolder = Loader.platform.getGameDataFolder();
        loaderDataFolder = new File(gameDataFolder, "copper");
        gameModFolder = new File(gameDataFolder, "mods");
        copperModFolder = new File(loaderDataFolder, "mods");
        copperModDataFolder = new File(loaderDataFolder, "datas");

        loaderContainer = Loader.platform.createLoaderContainer();
        loaderContainer.id = "loader";
        loaderContainer.export.addRule("include *");

        try {
            String ver = new String(loaderContainer.resource.get("version.properties"), StandardCharsets.UTF_8);
            Properties properties = new Properties();
            properties.load(new StringReader(ver));
            loaderVersion = new SemanticVersion(properties.getProperty("version", "0.0.0"));
        } catch (Throwable e) {
            throw new RuntimeException("failed to read loader version", e);
        }
    }
}
