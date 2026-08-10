package copper.loader.container;

import copper.loader.container.resource.*;

/**
 * A JVM container that delegates class loading to an existing {@link ClassLoader}.
 *
 * <p>Like {@link ForwardedContainer}, but extends {@link JvmContainer} to gain
 * bytecode-access methods ({@link #getTransformedOwnBytecode}, {@link #getAccessibleBytecode}, etc.).
 * Mixin transformation is <b>disabled</b> ({@code enableMixin = false}) —
 * this container is intended for wrapping classloaders that do not need
 * mixin processing (e.g. the loader's own classpath).</p>
 */
public class ForwardedJvmContainer extends JvmContainer {
    public ClassLoader loader;

    /**
     * @param target the classloader to delegate to
     */
    public ForwardedJvmContainer(ClassLoader target) {
        super(false);
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
