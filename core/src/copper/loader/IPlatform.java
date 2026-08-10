package copper.loader;

import copper.loader.container.*;
import copper.loader.mixin.*;
import java.io.*;

public interface IPlatform {
    String extractLibrary(byte[] library, String name);
    void extractCoreMod();
    IMixinEngine createMixinEngine();
    Class<?> loadSystemClass(String name);
    Container createModContainer(File file);
    Container createGameContainer();
    Container createLoaderContainer();
    File getGameDataFolder();
}
