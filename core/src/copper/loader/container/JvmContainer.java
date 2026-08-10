package copper.loader.container;

import copper.loader.*;
import copper.loader.container.info.*;
import copper.loader.mixin.*;
import copper.loader.util.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class JvmContainer extends Container {
    protected ContainerClassLoader loader;
    protected IMixinEngine mixinEngine;
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

    public void setMixinLogEnabled(boolean v) {
        logEnabled = v;
        if (mixinEngine != null)
            mixinEngine.setLogEnabled(v);
    }

    public void addMixinFlag(String v) {
        flag.add(v);
        if (mixinEngine != null)
            mixinEngine.setFlag(v);
    }

    public byte[] getAccessibleBytecode(String name) {
        byte[] code = null;
        // get transformed bytecode from dependency
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
        // get transformed bytecode from mixin
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

    public byte[] getRawOwnBytecode(String name) {
        return resource.get(name.replace('.', '/') + ".class");
    }

    public byte[] getPublicRawBytecode(String name) {
        if (export.check(name))
            return getRawOwnBytecode(name);
        return null;
    }

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

        protected Class<?> loadClass(String name, boolean resolve, boolean onlyOwnClass) throws ClassNotFoundException {
            synchronized (getClassLoadingLock(name)) {
                // find loaded class
                Class<?> c = findLoadedClass(name);
                // find java classes in bootstrap classloader
                if (c == null && name.startsWith("java.")) {
                    if (onlyOwnClass)
                        throw new ClassNotFoundException(name);
                    c = Loader.platform.loadSystemClass(name);
                }
                // find in this container
                if (c == null) {
                    try {
                        c = findClass(name);
                    } catch (ClassNotFoundException ignored) {
                    } catch (Throwable e) {
                        throw new RuntimeException("failed to define class in container " + JvmContainer.this.id + " : " + name, e);
                    }
                }
                // find accessible class
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

        @Override
        protected String findLibrary(String libname) {
            byte[] lib = JvmContainer.this.resource.get(libname);
            if (lib == null)
                return null;
            return Loader.platform.extractLibrary(lib, libname);
        }

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
