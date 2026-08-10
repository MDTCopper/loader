package copper.loader.container;

import copper.loader.container.resource.*;

/**
 * A container that delegates class loading to an existing {@link ClassLoader}.
 *
 * <p>Used to wrap classloaders that were created outside of the Container system
 * (e.g., the loader's own classpath).</p>
 */
public class ForwardedContainer extends Container {
    public ClassLoader loader;

    /**
     * @param target the classloader to delegate to
     */
    public ForwardedContainer(ClassLoader target) {
        super();
        loader = target;
        resource.resources.add(new ClassPathResource(target));
    }

    @Override
    public Class<?> loadOwnClass(String name) {
        try {
            return loader.loadClass(name);
        } catch (Throwable e) {
            return null;
        }
    }

    @Override
    public ClassLoader getClassLoader() {
        return loader;
    }
}
