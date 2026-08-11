package copper.loader.container;

import copper.loader.container.resource.*;

/**
 * A JVM delegated container that reads resources from an existing classloader's classpath.
 */
public class JvmDelegatedContainer extends DelegatedContainer {
    public JvmDelegatedContainer(ClassLoader target) {
        super(target);
        resource.resources.add(new ClassPathResource(target));
    }
}
