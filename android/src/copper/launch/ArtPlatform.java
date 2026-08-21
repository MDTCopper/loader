package copper.launch;

import copper.loader.*;
import copper.loader.container.*;
import copper.loader.container.resource.*;
import copper.loader.mod.*;
import copper.loader.util.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;

public abstract class ArtPlatform implements IPlatform {
    public static File jarFile;
    public static File gameFile;
    public static File gameAssetFile;
    public static File gameAndroidCompFile;
    public static File gameLibFile;
    public static File gameDataFolder;
    public static File cacheFolder;
    public static File libCacheFolder;
    public static File optimizedDexCacehFolder;

    public static void init() {
        gameAssetFile = new File(cacheFolder, "asset.jar");
        gameAndroidCompFile = new File(cacheFolder, "android.jar");
        gameLibFile = new File(cacheFolder, "lib.jar");
        libCacheFolder = new File(cacheFolder, "lib");
        optimizedDexCacehFolder = new File(cacheFolder, "oat");

        libCacheFolder.mkdirs();
        optimizedDexCacehFolder.mkdirs();
    }

    @Override
    public String extractLibrary(byte[] library, String name) {
        try {
            File target = new File(libCacheFolder, System.mapLibraryName(name));
            extractFile(library, target);
            target.setReadOnly();
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
        extractFile(data, target);
    }

    @Override
    public MixinContainer createModContainer(File file) {
        return createJarContainer(file);
    }

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

    protected abstract MixinContainer createJarContainer(File file);

    protected abstract String getAbi();
}
