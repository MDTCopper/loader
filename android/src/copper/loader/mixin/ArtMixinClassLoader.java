package copper.loader.mixin;

import copper.launch.*;
import copper.loader.container.*;
import dalvik.system.*;
import java.io.*;
import java.net.*;
import java.util.*;

/**
 * The dex-based counterpart of {@link JvmMixinClassLoader}: loads the mixin
 * engine's isolated environment from the loader jar on Android.
 *
 * <p>Classes matching {@link MixinContainerClassFilter} are loaded from this
 * loader's own dex; everything else is delegated to the parent.</p>
 */
public class ArtMixinClassLoader extends DexClassLoader {
    private final ClassFilter filter;

    public ArtMixinClassLoader(File jarFile) {
        super(jarFile.getAbsolutePath(), ArtPlatform.optimizedDexCacehFolder.getAbsolutePath(), null, ArtPlatform.class.getClassLoader());
        filter = new MixinContainerClassFilter();
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (filter.check(name)) {
            // child-first: look in this loader before asking the parent
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

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        return getParent().getResources(name);
    }
}