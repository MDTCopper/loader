package copper.loader.container;

// resources is missing, so it's abstract
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
