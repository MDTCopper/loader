package copper.loader.container;

import copper.loader.container.resource.*;

public class ForwardedJvmContainer extends JvmContainer {
    public ClassLoader loader;

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
