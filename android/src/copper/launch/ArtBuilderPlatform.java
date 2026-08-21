package copper.launch;

import copper.loader.container.*;
import copper.loader.container.resource.*;
import copper.loader.mixin.*;
import java.io.*;
import java.util.zip.*;

public class ArtBuilderPlatform extends ArtPlatform {
    public static boolean desktopMode = false;

    @Override
    public IMixinEngine createMixinEngine() {
        try {
            ClassLoader cl = desktopMode ?
                    new JvmMixinClassLoader() :
                    (ClassLoader) Class.forName("copper.loader.mixin.ArtMixinClassLoader")
                            .getDeclaredConstructor(File.class).newInstance(jarFile);
            Class<?> c = cl.loadClass("copper.loader.mixin.MixinEngine");
            return (IMixinEngine) c.getDeclaredMethod("getInstance").invoke(null);
        } catch (Throwable e) {
            throw new RuntimeException("failed to create mixin engine", e);
        }
    }

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

    @Override
    protected String getAbi() {
        return "arm64-v8a";
    }
}
