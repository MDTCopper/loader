package copper.loader;

import copper.loader.container.*;
import copper.loader.mixin.*;
import java.io.*;

/**
 * Platform abstraction layer.
 *
 * <p>Each supported platform (JVM desktop, Android, etc.) implements this interface
 * to provide platform-specific behaviour such as library extraction, mixin engine creation,
 * and container construction.</p>
 */
public interface IPlatform {
    /**
     * Extracts a native library from an in-memory byte array to a temporary file.
     *
     * @param library the raw library bytes
     * @param name    the library name (without platform prefix/suffix)
     * @return the absolute path to the extracted file, or {@code null} on failure
     */
    String extractLibrary(byte[] library, String name);

    /**
     * Extracts the built-in core mod ({@code core-mod.jar}) to the Copper mods folder.
     */
    void extractCoreMod();

    /**
     * Creates a new mixin engine instance for this platform.
     */
    IMixinEngine createMixinEngine();

    /**
     * Creates a container for a mod file (jar/zip or directory).
     */
    MixinContainer createModContainer(File file);

    /**
     * Creates a container for the game jar.
     */
    MixinContainer createGameContainer();

    /**
     * Creates a container representing the loader's own classpath.
     */
    Container createLoaderContainer();

    /**
     * Returns the game data directory (e.g., {@code ~/.mindustry}).
     */
    File getGameDataFolder();
}
