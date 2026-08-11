package copper.loader.container;

/**
 * An abstract container that delegates class loading to an existing {@link ClassLoader}.
 *
 * <p>Resource access must be configured separately by subclasses (e.g. by adding
 * a {@link copper.loader.container.resource.ClassPathResource} to {@code resource.resources}).</p>
 */
public abstract class DelegatedContainer extends Container {
    protected ClassLoader loader;

    public DelegatedContainer(ClassLoader target) {
        loader = target;
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
