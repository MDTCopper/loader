package copper.launch;

import copper.loader.container.*;
import copper.loader.container.resource.*;
import copper.loader.mixin.*;
import java.io.*;
import java.util.zip.*;

/**
 * The {@link ArtPlatform} used by {@link ArtBuilder} while building the dex cache.
 *
 * <p>The builder can run on a desktop JVM ({@code desktopMode}) or directly on
 * the device (the launcher app's "build" action). Either way a real mixin engine
 * is available and containers only expose raw bytecode — classes are never
 * loaded ({@link ArtMixinOnlyContainer}).</p>
 */
public class ArtBuilderPlatform extends ArtPlatform {
    /** When {@code true}, the builder runs directly on the desktop JVM. */
    public static boolean desktopMode = false;

    /**
     * Creates a real mixin engine through an isolated classloader:
     * {@link JvmMixinClassLoader} on the desktop, {@code ArtMixinClassLoader}
     * when the builder itself is running as dex on Android.
     */
    @Override
    public IMixinEngine createMixinEngine() {
        try {
            ClassLoader cl = desktopMode ?
                    new JvmMixinClassLoader() :
                    // avoid ClassNotFound on desktop
                    (ClassLoader) Class.forName("copper.loader.mixin.ArtMixinClassLoader")
                            .getDeclaredConstructor(File.class).newInstance(jarFile);
            Class<?> c = cl.loadClass("copper.loader.mixin.MixinEngine");
            return (IMixinEngine) c.getDeclaredMethod("getInstance").invoke(null);
        } catch (Throwable e) {
            throw new RuntimeException("failed to create mixin engine", e);
        }
    }

    /** Containers in the builder only expose resources; classes are never loaded. */
    @Override
    protected MixinContainer createJarContainer(File file) {
        try {
            MixinContainer container = new ArtMixinOnlyContainer();
            container.resource.resources.add(new ZipResource(new ZipFile(file)));
            return container;
        } catch (Throwable e) {
            throw new RuntimeException("failed to create container for: " + file.getAbsolutePath(), e);
        }
    }

    /**
     * Builder-side ABI, only used when the game container reads the packed
     * native libs; the launcher picks the real device ABI at runtime.
     */
    @Override
    protected String getAbi() {
        return "arm64-v8a";
    }
}