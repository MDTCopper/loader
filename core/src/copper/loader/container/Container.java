package copper.loader.container;

import copper.loader.container.info.*;
import java.util.*;

/**
 * A pluggable classpath container providing class loading, resource access,
 * and dependency traversal between containers.
 *
 * <p>Each container owns a set of {@link copper.loader.container.info.DependencyInfo dependencies}
 * (other containers it can search for classes), a set of
 * {@link copper.loader.container.info.MixinInfo mixin configs} (bytecode transformations
 * to apply), a {@link ResourceProvider}, and a {@link ClassFilter} controlling export
 * visibility.</p>
 */
public abstract class Container {
    /** Debug identifier (e.g. mod id). */
    public String id;
    /** Dependencies of this container. */
    public ArrayList<DependencyInfo> dependency;
    /** Mixin configurations targeting this container. */
    public ArrayList<MixinInfo> mixin;
    /** Resource provider for reading files. */
    public ResourceProvider resource;
    /** Filter controlling public visibility of this container's own classes. */
    public ClassFilter export;

    public Container() {
        id = "unnamed";
        dependency = new ArrayList<>();
        mixin = new ArrayList<>();
        resource = new ResourceProvider();
        export = new ClassFilter();
    }

    /** Called after construction to initialize this container (e.g., bootstrap mixin engine). */
    public void init() {}

    /**
     * Finds a class accessible from this container.
     * Searches mixin targets first, then dependencies.
     *
     * @param name fully qualified class name
     * @return the class, or {@code null} if not found
     */
    public Class<?> getAccessibleClass(String name) {
        Class<?> c = null;
        // Search mixin target containers first.
        for (MixinInfo info : mixin) {
            c = info.container.loadPublicOwnClass(name);
            if (c != null)
                break;
        }
        // Fall back to dependency containers.
        if (c == null) {
            for (DependencyInfo info : dependency) {
                if (info.extraImport.check(name))
                    c = info.container.loadOwnClass(name);
                else
                    c = info.container.loadPublicOwnClass(name);
                if (c != null)
                    break;
            }
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
