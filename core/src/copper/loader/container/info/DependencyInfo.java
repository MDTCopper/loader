package copper.loader.container.info;

import copper.loader.container.*;

public class DependencyInfo {
    public Container container;
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
