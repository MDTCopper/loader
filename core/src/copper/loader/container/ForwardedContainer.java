package copper.loader.container;

import copper.loader.container.resource.*;

public class ForwardedContainer extends Container {
    public ClassLoader loader;

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
