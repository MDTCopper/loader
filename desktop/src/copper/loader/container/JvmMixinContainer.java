package copper.loader.container;

import copper.loader.*;
import copper.loader.container.info.*;
import copper.loader.mixin.*;
import copper.loader.util.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * A JVM container backed by a custom {@link ContainerClassLoader} with optional
 * bytecode transformation via the Mixin framework.
 *
 * <p>This is the primary container implementation for the JVM desktop platform.
 * Mixin transformation, in-memory resource URLs, and native library extraction
 * are handled here; see individual methods for specifics.</p>
 */
public class JvmMixinContainer extends MixinContainer {
    protected ContainerClassLoader loader;

    public JvmMixinContainer() {
        loader = new ContainerClassLoader();
        transformedBytecode = new ConcurrentHashMap<>();
    }

    @Override
    public Class<?> loadOwnClass(String name) {
        try {
            return loader.loadOwnClass(name);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    @Override
    public ClassLoader getClassLoader() {
        return loader;
    }

    /**
     * Custom classloader for a {@link JvmMixinContainer}.
     *
     * <p>Class loading order:
     * <ol>
     *   <li>Check already-loaded classes</li>
     *   <li>For {@code java.*} classes: delegate to platform system classloader</li>
     *   <li>Find in this container's own resources (with mixin transformation)</li>
     *   <li>Find in accessible dependency/mixin containers</li>
     * </ol>
     */
    public class ContainerClassLoader extends ClassLoader {
        public ContainerClassLoader() {
            super(null);
        }

        public Class<?> loadOwnClass(String name) throws ClassNotFoundException {
            return loadClass(name, false, true);
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            return loadClass(name, resolve, false);
        }

        /**
         * @param name         fully qualified class name
         * @param resolve      whether to resolve the class after loading
         * @param onlyOwnClass if {@code true}, skip the dependency/mixin fallback
         */
        protected Class<?> loadClass(String name, boolean resolve, boolean onlyOwnClass) throws ClassNotFoundException {
            synchronized (getClassLoadingLock(name)) {
                // Already loaded?
                Class<?> c = findLoadedClass(name);

                // java classes from the platform system classloader.
                if (c == null) {
                    if (name.startsWith("java.") ||
                            name.startsWith("javax.")) {
                        if (onlyOwnClass)
                            throw new ClassNotFoundException(name);
                        try {
                            c = Object.class.getClassLoader().loadClass(name);
                        } catch (Throwable ignored) {}
                    }
                }

                // Search this container's own classes.
                if (c == null) {
                    try {
                        c = findClass(name);
                    } catch (ClassNotFoundException ignored) {
                    } catch (Throwable e) {
                        throw new RuntimeException("failed to define class in container " + JvmMixinContainer.this.id + " : " + name, e);
                    }
                }

                // Fall back to accessible classes from dependencies/mixins.
                if (c == null && !onlyOwnClass)
                    c = getAccessibleClass(name);

                if (c != null) {
                    if (resolve)
                        resolveClass(c);
                    return c;
                } else {
                    throw new ClassNotFoundException(name);
                }
            }
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            byte[] code = JvmMixinContainer.this.getOwnBytecode(name);
            if (code == null)
                throw new ClassNotFoundException(name);
            return defineClass(name, code, 0, code.length);
        }

        /**
         * Finds and extracts a native library from this container's resources.
         *
         * @return the absolute path to the extracted library, or {@code null}
         */
        @Override
        protected String findLibrary(String libname) {
            byte[] lib = JvmMixinContainer.this.resource.get(libname);
            if (lib == null)
                lib = JvmMixinContainer.this.resource.get(System.mapLibraryName(libname));
            if (lib == null)
                return null;
            return Loader.platform.extractLibrary(lib, libname);
        }

        /**
         * Creates a {@code copper-memory:} URL for an in-memory resource.
         */
        @Override
        protected URL findResource(String name) {
            byte[] resource = JvmMixinContainer.this.resource.get(name);
            if (resource == null)
                return null;
            try {
                return new URL("copper-memory", null, -1, name, new MemoryURLStreamHandler(resource));
            } catch (Throwable e) {
                return null;
            }
        }

        @Override
        protected Enumeration<URL> findResources(String name) throws IOException {
            URL url = findResource(name);
            if (url == null)
                return Collections.emptyEnumeration();
            return Collections.enumeration(List.of(url));
        }
    }
}
