package copper.loader.container;

import copper.loader.container.resource.*;

public class JvmDelegatedContainer extends DelegatedContainer {
    public JvmDelegatedContainer(ClassLoader target) {
        super(target);
        resource.resources.add(new ClassPathResource(target));
    }
}
