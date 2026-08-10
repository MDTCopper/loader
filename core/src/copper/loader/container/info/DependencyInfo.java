package copper.loader.container.info;

import copper.loader.container.*;

/**
 * Describes a dependency on another container, optionally with extra import rules.
 */
public class DependencyInfo {
    /** The dependency container. */
    public Container container;
    /** Extra class-visibility rules applied when importing from this dependency. */
    public ClassFilter extraImport;

    public DependencyInfo() {
        extraImport = new ClassFilter();
    }

    public DependencyInfo(Container container) {
        this.container = container;
        extraImport = new ClassFilter();
    }

    public DependencyInfo(Container container, String extraImportRule) {
        this.container = container;
        extraImport = new ClassFilter();
        extraImport.addRule(extraImportRule);
    }
}
