package copper.loader.container;

import copper.launch.*;
import copper.loader.*;
import copper.loader.util.*;
import dalvik.system.*;

import java.io.*;
import java.net.*;
import java.util.*;

public class ArtPreMixinContainer extends MixinContainer {
    protected ContainerClassLoader loader;

    @Override
    public void init() {
        File dexFile = ArtRuntimePlatform.dexCache.getRuntimeDexFile(id);
        dexFile.setReadOnly();
        loader = new ContainerClassLoader(dexFile);
        super.init();
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

    public class ContainerClassLoader extends DexClassLoader {
        public ContainerClassLoader(File dexFile) {
            super(dexFile.getAbsolutePath(), ArtPlatform.optimizedDexCacehFolder.getAbsolutePath(), null, null);
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
                    throw new RuntimeException("failed to define class in container " + ArtPreMixinContainer.this.id + " : " + name, e);
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

        /**
         * Finds and extracts a native library from this container's resources.
         *
         * @return the absolute path to the extracted library, or {@code null}
         */
        @Override
        public String findLibrary(String libname) {
            byte[] lib = ArtPreMixinContainer.this.resource.get(libname);
            if (lib == null)
                lib = ArtPreMixinContainer.this.resource.get(System.mapLibraryName(libname));
            if (lib == null)
                return null;
            return Loader.platform.extractLibrary(lib, libname);
        }

        /**
         * Creates a {@code copper-memory:} URL for an in-memory resource.
         */
        @Override
        protected URL findResource(String name) {
            byte[] resource = ArtPreMixinContainer.this.resource.get(name);
            if (resource == null)
                return null;
            try {
                return new URL("copper-memory", null, -1, name, new MemoryURLStreamHandler(resource));
            } catch (Throwable e) {
                return null;
            }
        }

        @Override
        protected Enumeration<URL> findResources(String name) {
            URL url = findResource(name);
            if (url == null)
                return Collections.emptyEnumeration();
            return Collections.enumeration(List.of(url));
        }
    }
}
