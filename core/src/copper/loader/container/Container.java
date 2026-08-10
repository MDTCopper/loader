package copper.loader.container;

import copper.loader.container.info.*;
import java.util.*;

public abstract class Container {
    // for debug
    public String id;
    public ArrayList<DependencyInfo> dependency;
    public ArrayList<MixinInfo> mixin;
    public ResourceProvider resource;
    public ClassFilter export;

    public Container() {
        id = "unnamed";
        dependency = new ArrayList<>();
        mixin = new ArrayList<>();
        resource = new ResourceProvider();
        export = new ClassFilter();
    }

    public void init() {}

    public Class<?> getAccessibleClass(String name) {
        Class<?> c = null;
        // find in mixin containers
        for (MixinInfo info : mixin) {
            c = info.container.loadPublicOwnClass(name);
            if (c != null)
                break;
        }
        // find in dependency containers
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

    public abstract Class<?> loadOwnClass(String name);

    public Class<?> loadPublicOwnClass(String name) {
        if (export.check(name))
            return loadOwnClass(name);
        return null;
    }

    public abstract ClassLoader getClassLoader();

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
