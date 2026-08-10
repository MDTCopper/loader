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
public class JvmContainer extends Container {
    protected ContainerClassLoader loader;
    protected IMixinEngine mixinEngine;
    /** Cache of already-transformed bytecode. */
    protected Map<String, byte[]> transformedBytecode;
    private final boolean mixinEnabled;
    private boolean logEnabled;
    private ArrayList<String> flag;

    protected JvmContainer(boolean enableMixin) {
        super();
        loader = new ContainerClassLoader();
        transformedBytecode = new ConcurrentHashMap<>();
        logEnabled = false;
        mixinEnabled = enableMixin;
        flag = new ArrayList<>();
    }

    public JvmContainer() {
        this(true);
    }

    @Override
    public void init() {
        if (!mixin.isEmpty() && mixinEnabled) {
            mixinEngine = Loader.platform.createMixinEngine();
            mixinEngine.setBytecodeProvider(name -> {
                if (name.startsWith("/"))
                    name = name.substring(1);
                name = name.replace('/', '.');

                byte[] code = getRawOwnBytecode(name);
                if (code == null)
                    code = getAccessibleBytecode(name);
                return code;
            });
            mixinEngine.setEngineId(id);
            mixinEngine.setLogEnabled(logEnabled);
            mixinEngine.bootstrap();
            for (String f : flag)
                mixinEngine.setFlag(f);
            for (MixinInfo info : mixin)
                mixinEngine.addConfig(info.config, info.container.id.replace(':', '-'));
        }
    }

    /** Enables or disables mixin audit logging for this container. */
    public void setMixinLogEnabled(boolean v) {
        logEnabled = v;
        if (mixinEngine != null)
            mixinEngine.setLogEnabled(v);
    }

    /** Adds a mixin environment flag (e.g. {@code "EXPORT_FILTER"}). */
    public void addMixinFlag(String v) {
        flag.add(v);
        if (mixinEngine != null)
            mixinEngine.setFlag(v);
    }

    /**
     * Gets bytecode for a class from a dependency or mixin target,
     * returning the already-transformed version.
     */
    public byte[] getAccessibleBytecode(String name) {
        byte[] code = null;
        // Search dependency containers for transformed bytecode.
        for (DependencyInfo info : dependency) {
            if (info.container instanceof JvmContainer container) {
                if (info.extraImport.check(name))
                    code = container.getTransformedOwnBytecode(name);
                else
                    code = container.getPublicTransformedOwnBytecode(name);
                if (code != null)
                    break;
            }
        }
        // Fall back to mixin target containers.
        if (code == null) {
            for (MixinInfo info : mixin) {
                if (info.container instanceof JvmContainer container) {
                    code = container.getPublicTransformedOwnBytecode(name);
                    if (code != null)
                        break;
                }
            }
        }
        return code;
    }

    /**
     * Returns the raw (untransformed) bytecode of a class from this container.
     */
    public byte[] getRawOwnBytecode(String name) {
        return resource.get(name.replace('.', '/') + ".class");
    }

    /**
     * Returns raw bytecode of an own class, but only if it passes the export filter.
     */
    public byte[] getPublicRawBytecode(String name) {
        if (export.check(name))
            return getRawOwnBytecode(name);
        return null;
    }

    /**
     * Returns the transformed (mixin-processed) bytecode of an own class.
     * Results are cached.
     */
    public byte[] getTransformedOwnBytecode(String name) {
        byte[] code = transformedBytecode.get(name);
        if (code == null) {
            code = getRawOwnBytecode(name);
            if (mixinEngine != null) {
                byte[] transformed = mixinEngine.transform(name, code);
                if (transformed != null) {
                    transformedBytecode.put(name, transformed);
                    code = transformed;
                }
            }
        }
        return code;
    }

    /**
     * Returns transformed bytecode of an own class, but only if it passes the export filter.
     */
    public byte[] getPublicTransformedOwnBytecode(String name) {
        if (export.check(name))
            return getTransformedOwnBytecode(name);
        return null;
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
     * Custom classloader for a {@link JvmContainer}.
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

                // java.* classes from the platform system classloader.
                if (c == null && name.startsWith("java.")) {
                    if (onlyOwnClass)
                        throw new ClassNotFoundException(name);
                    c = Loader.platform.loadSystemClass(name);
                }

                // Search this container's own classes.
                if (c == null) {
                    try {
                        c = findClass(name);
                    } catch (ClassNotFoundException ignored) {
                    } catch (Throwable e) {
                        throw new RuntimeException("failed to define class in container " + JvmContainer.this.id + " : " + name, e);
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
            byte[] code = JvmContainer.this.getTransformedOwnBytecode(name);
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
            byte[] lib = JvmContainer.this.resource.get(libname);
            if (lib == null)
                return null;
            return Loader.platform.extractLibrary(lib, libname);
        }

        /**
         * Creates a {@code copper-memory:} URL for an in-memory resource.
         */
        @Override
        protected URL findResource(String name) {
            byte[] resource = JvmContainer.this.resource.get(name);
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
