package copper.loader.container;

import copper.loader.container.info.*;
import java.util.*;

/**
 * A pluggable classpath container providing class loading, resource access,
 * and dependency traversal between containers.
 *
 * <p>Each container owns a set of {@link copper.loader.container.info.DependencyInfo dependencies}
 * (other containers it can search for classes), a {@link ResourceProvider}, and a
 * {@link ClassFilter} controlling export visibility.</p>
 */
public abstract class Container {
    /** Debug identifier (e.g. mod id). */
    public String id;
    /** Dependencies of this container. */
    public List<DependencyInfo> dependency;
    /** Resource provider for reading files. */
    public ResourceProvider resource;
    /** Filter controlling public visibility of this container's own classes. */
    public ClassFilter export;

    public Container() {
        id = "unnamed";
        dependency = new ArrayList<>();
        resource = new ResourceProvider();
        export = new ClassFilter();
    }

    /** Called after all runtime required info is resolved. */
    public void init() {}

    /**
     * Finds bytecode accessible from this container by searching dependency containers.
     *
     * @param name fully qualified class name (dots or slashes)
     * @return the bytecode, or {@code null} if not found
     */
    public byte[] getAccessibleBytecode(String name) {
        byte[] code = null;
        // Search dependency containers for transformed bytecode.
        for (DependencyInfo info : dependency) {
            if (info.extraImport.check(name))
                code = info.container.getOwnBytecode(name);
            else
                code = info.container.getPublicOwnBytecode(name);
            if (code != null)
                break;
        }
        return code;
    }

    /**
     * Reads raw bytecode from this container's own resources.
     *
     * @param name fully qualified class name (dots or slashes)
     * @return the bytecode, or {@code null} if not found
     */
    public byte[] getOwnBytecode(String name) {
        return resource.get(name.replace('.', '/') + ".class");
    }

    /**
     * Reads bytecode from this container's own resources, but only if the class passes the export filter.
     */
    public byte[] getPublicOwnBytecode(String name) {
        if (export.check(name))
            return getOwnBytecode(name);
        return null;
    }

    /**
     * Finds a class accessible from this container by searching dependency containers.
     *
     * @param name fully qualified class name
     * @return the class, or {@code null} if not found
     */
    public Class<?> getAccessibleClass(String name) {
        Class<?> c = null;
        for (DependencyInfo info : dependency) {
            if (info.extraImport.check(name))
                c = info.container.loadOwnClass(name);
            else
                c = info.container.loadPublicOwnClass(name);
            if (c != null)
                break;
        }
        return c;
    }

    /**
     * Loads a class from this container's own resources.
     *
     * @param name fully qualified class name
     * @return the class, or {@code null} if not found
     */
    public abstract Class<?> loadOwnClass(String name);

    /**
     * Loads a class from this container's own resources, but only if it passes the export filter.
     */
    public Class<?> loadPublicOwnClass(String name) {
        if (export.check(name))
            return loadOwnClass(name);
        return null;
    }

    /**
     * Returns the raw classloader for this container.
     */
    public abstract ClassLoader getClassLoader();

    /**
     * Returns a classloader that only exposes publicly-exported classes.
     */
    public ClassLoader getPublicClassLoader() {
        return new ClassLoader() {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                Class<?> c = loadPublicOwnClass(name);
                if (c == null)
                    throw new ClassNotFoundException(name);
                return c;
            }
        };
    }
}
