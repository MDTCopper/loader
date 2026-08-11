package copper.loader.mixin;

import copper.launch.*;
import copper.loader.container.*;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * A child-first classloader for the mixin engine's isolated environment.
 *
 * <p>Classes matching {@link MixinContainerClassFilter} are loaded inside this
 * classloader; all others are delegated to the parent. This isolates the Mixin
 * framework and Copper's mixin integration classes from the rest of the system.</p>
 */
public class JvmMixinClassLoader extends ClassLoader {
    private final ClassFilter filter = new MixinContainerClassFilter();

    public JvmMixinClassLoader() {
        super(JvmPlatform.class.getClassLoader());
    }

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
}
