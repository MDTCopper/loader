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

public class JvmPlatform implements IPlatform {
    public static File gameJar;
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

    @Override
    public IMixinEngine createMixinEngine() {
        ClassLoader cl = new ClassLoader(JvmLauncher.class.getClassLoader()) {
            private final ClassFilter filter = new MixinContainerClassFilter();

            @Override
            protected Class<?> findClass(String name) throws ClassNotFoundException {
                try (InputStream is = getResourceAsStream(name.replace('.', '/') + ".class")) {
                    if (is == null)
                        throw new ClassNotFoundException(name);
                    byte[] code = is.readAllBytes();
                    return defineClass(name, code, 0, code.length);
                } catch (Throwable e) {
                    throw new ClassNotFoundException(name);
                }
            }

            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                synchronized (getClassLoadingLock(name)) {
                    if (filter.check(name)) {
                        Class<?> c = findLoadedClass(name);
                        if (c == null) {
                            try {
                                c = findClass(name);
                            } catch (Throwable ignored) {}
                        }
                        if (c == null)
                            throw new ClassNotFoundException(name);
                        if (resolve)
                            resolveClass(c);
                        return c;
                    } else {
                        return getParent().loadClass(name);
                    }
                }
            }

            @Override
            public Enumeration<URL> getResources(String name) throws IOException {
                return getParent().getResources(name);
            }
        };
        try {
            Class<?> c = cl.loadClass("copper.loader.mixin.MixinEngine");
            return (IMixinEngine) c.getDeclaredMethod("getInstance").invoke(null);
        } catch (Throwable e) {
            throw new RuntimeException("failed to create mixin engine", e);
        }

    }

    @Override
    public Class<?> loadSystemClass(String name) {
        try {
            return JvmLauncher.class.getClassLoader().loadClass(name);
        } catch (Throwable e) {
            return null;
        }
    }

    @Override
    public Container createModContainer(File file) {
        return createJarContainer(file);
    }

    @Override
    public Container createGameContainer() {
        return createJarContainer(gameJar);
    }

    @Override
    public Container createLoaderContainer() {
        return new ForwardedJvmContainer(Loader.class.getClassLoader());
    }

    @Override
    public File getGameDataFolder() {
        return gameData;
    }

    private Container createJarContainer(File file) {
        try {
            Container container = new JvmContainer();
            ZipFile zip = new ZipFile(file);
            container.resource.resources.add(new ZipResource(zip));
            return container;
        } catch (Throwable e) {
            throw new RuntimeException("failed to create container", e);
        }
    }

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
