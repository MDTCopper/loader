package copper.launch;

import copper.loader.*;
import copper.loader.container.*;
import copper.loader.container.resource.*;
import copper.loader.mixin.*;
import copper.loader.util.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.zip.*;

/**
 * JVM desktop platform implementation.
 *
 * <p>Provides jar/zip-based containers, native library extraction,
 * and a classloader-isolated mixin engine.</p>
 */
public class JvmPlatform implements IPlatform {
    /** Path to the game jar file. */
    public static File gameJar;
    /** Game data directory (default: {@code .mindustry}). */
    public static File gameData = new File(".mindustry");

    @Override
    public String extractLibrary(byte[] library, String name) {
        try {
            File target = new File(Loader.vars.loaderDataFolder, "lib/" + System.mapLibraryName(name));
            extractFile(library, target);
            return target.getAbsolutePath();
        } catch (Throwable e) {
            Log.error("Failed to extract library: " + name);
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void extractCoreMod() {
        File target = new File(Loader.vars.copperModFolder, "copper-core.jar");
        byte[] data = Loader.vars.loaderContainer.resource.get("core-mod.jar");
        if (data == null)
            throw new RuntimeException("no core mod found in package");
        JvmPlatform.extractFile(data, target);
    }

    /**
     * Creates a mixin engine inside an isolated classloader.
     *
     * <p>The isolated classloader only exposes SpongePowered ASM classes and
     * Copper's mixin internals, as defined by {@link MixinContainerClassFilter}.
     * All other classes are delegated to the parent classloader.</p>
     */
    @Override
    public IMixinEngine createMixinEngine() {
        ClassLoader cl = new JvmMixinClassLoader();
        try {
            Class<?> c = cl.loadClass("copper.loader.mixin.MixinEngine");
            return (IMixinEngine) c.getDeclaredMethod("getInstance").invoke(null);
        } catch (Throwable e) {
            throw new RuntimeException("failed to create mixin engine", e);
        }

    }

    @Override
    public MixinContainer createModContainer(File file) {
        return createJarContainer(file);
    }

    @Override
    public MixinContainer createGameContainer() {
        return createJarContainer(gameJar);
    }

    @Override
    public Container createLoaderContainer() {
        return new JvmDelegatedContainer(Loader.class.getClassLoader());
    }

    @Override
    public File getGameDataFolder() {
        return gameData;
    }

    /** Creates a {@link JvmMixinContainer} backed by a zip/jar file. */
    private MixinContainer createJarContainer(File file) {
        try {
            MixinContainer container = new JvmMixinContainer();
            ZipFile zip = new ZipFile(file);
            container.resource.resources.add(new ZipResource(zip));
            return container;
        } catch (Throwable e) {
            throw new RuntimeException("failed to create container", e);
        }
    }

    /**
     * Extracts a byte array to a file, but only if the file doesn't
     * already exist with the same content.
     */
    private static void extractFile(byte[] data, File file) {
        try (var fis = new FileInputStream(file)) {
            byte[] curr = fis.readAllBytes();
            if (Arrays.equals(data, curr))
                return;
        } catch (Throwable ignored) {}

        try {
            file.getParentFile().mkdirs();
            try (var fos = new FileOutputStream(file, false)) {
                fos.write(data);
            }
        } catch (Throwable e) {
            throw new RuntimeException("failed to extract file", e);
        }
    }
}
