package copper.launch;

import copper.loader.*;
import copper.loader.container.*;
import copper.loader.container.resource.*;
import copper.loader.mod.*;
import copper.loader.util.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;

/**
 * Shared {@link IPlatform} implementation for the ART (Android) side.
 *
 * <p>Holds the fixed cache file locations and the shared logic needed both when
 * {@link ArtBuilder building} the dex cache and when {@link ArtLauncher running}
 * it on the device. Subclasses only need to provide the concrete jar container
 * type and the device ABI.</p>
 */
public abstract class ArtPlatform implements IPlatform {
    /** The loader jar file. */
    public static File jarFile;
    /** The desktop game jar used as the bytecode source. */
    public static File gameFile;
    /** Packed game assets (what would be {@code assets/} in an apk). */
    public static File gameAssetFile;
    /**
     * Compiled android components: the game's android sources and the arc
     * android backend (its base activity is rewritten to extend the loader's
     * own {@link LoaderActivity}).
     */
    public static File gameAndroidCompFile;
    /**
     * Packed native libraries (.so files) of the arc natives, one folder per
     * ABI; the runtime picks the one matching the device.
     */
    public static File gameLibFile;
    /** The game data folder on the device. */
    public static File gameDataFolder;
    /** The cache folder that holds everything built by {@link ArtBuilder}. */
    public static File cacheFolder;
    /** Folder where extracted native libraries are stored. */
    public static File libCacheFolder;
    /** Folder where optimized dex files (oat) are written by Android. */
    public static File optimizedDexCacehFolder;

    /** Fills in the cache file locations and creates the extra folders. */
    public static void init() {
        gameAssetFile = new File(cacheFolder, "asset.jar");
        gameAndroidCompFile = new File(cacheFolder, "android.jar");
        gameLibFile = new File(cacheFolder, "lib.jar");
        libCacheFolder = new File(cacheFolder, "lib");
        optimizedDexCacehFolder = new File(cacheFolder, "oat");

        libCacheFolder.mkdirs();
        optimizedDexCacehFolder.mkdirs();
    }

    /**
     * Writes a native library into the lib cache and returns its path,
     * so it can be loaded even though the original jar is gone.
     */
    @Override
    public String extractLibrary(byte[] library, String name) {
        try {
            File target = new File(libCacheFolder, System.mapLibraryName(name));
            extractFile(library, target);
            target.setReadOnly();
            return target.getAbsolutePath();
        } catch (Throwable e) {
            Log.error("Failed to extract library: " + name);
            Log.error(e);
            return null;
        }
    }

    /** Writes the bundled copper core mod into the copper mod folder. */
    @Override
    public void extractCoreMod() {
        File target = new File(Loader.vars.copperModFolder, "copper-core.jar");
        byte[] data = Loader.vars.loaderContainer.resource.get("core-mod.jar");
        if (data == null)
            throw new RuntimeException("no core mod found in package");
        extractFile(data, target);
    }

    @Override
    public MixinContainer createModContainer(File file) {
        return createJarContainer(file);
    }

    /**
     * Creates the game container and injects the android pieces into it:
     * native libs, packed assets, and the compiled android components.
     */
    @Override
    public MixinContainer createGameContainer() {
        try {
            var container = createJarContainer(gameFile);
            var originalRes = container.resource;
            container.resource = new ResourceProvider();

            var res = new ZipResource(new ZipFile(gameLibFile));
            var mres = new MountedResouce(res, getAbi());
            var asset = new ZipResource(new ZipFile(gameAssetFile));
            var masset = new MountedResouce(asset, "assets");

            // inject android libs, assets and components
            container.resource.resources.add(mres);
            container.resource.resources.add(masset);
            container.resource.resources.add(new ZipResource(new ZipFile(gameAndroidCompFile)));
            // restore resources
            container.resource.resources.addAll(originalRes.resources);
            return container;
        } catch (Throwable e) {
            throw new RuntimeException("failed to create game container", e);
        }
    }

    @Override
    public Container createLoaderContainer() {
        return new ArtDelegatedContainer(Loader.class.getClassLoader(), jarFile);
    }

    @Override
    public File getGameDataFolder() {
        return gameDataFolder;
    }

    /**
     * Extracts a byte array to a file, but only if the file doesn't
     * already exist with the same content.
     */
    private void extractFile(byte[] data, File file) {
        try (var fis = new FileInputStream(file)) {
            byte[] curr = Streams.readAllBytes(fis);
            if (Arrays.equals(data, curr))
                return;
        } catch (Throwable ignored) {}

        try {
            file.getParentFile().mkdirs();
            file.delete();
            try (var fos = new FileOutputStream(file, false)) {
                fos.write(data);
            }
        } catch (Throwable e) {
            throw new RuntimeException("failed to extract file", e);
        }
    }

    /** Creates a mixin container for one jar file. */
    protected abstract MixinContainer createJarContainer(File file);

    /** Returns the ABI (e.g. {@code arm64-v8a}) the packed native libs target. */
    protected abstract String getAbi();
}