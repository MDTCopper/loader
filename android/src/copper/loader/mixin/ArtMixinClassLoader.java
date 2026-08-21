package copper.loader.mixin;

import copper.launch.*;
import copper.loader.container.*;
import dalvik.system.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class ArtMixinClassLoader extends DexClassLoader {
    private final ClassFilter filter;

    public ArtMixinClassLoader(File jarFile) {
        super(jarFile.getAbsolutePath(), ArtPlatform.optimizedDexCacehFolder.getAbsolutePath(), null, ArtPlatform.class.getClassLoader());
        filter = new MixinContainerClassFilter();
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
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

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        return getParent().getResources(name);
    }
}
