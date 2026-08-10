package copper.loader.container.info;

import copper.loader.container.*;

public class MixinInfo {
    public Container container;
    public String config;

    public MixinInfo() {
        config = "{}";
    }

    public MixinInfo(Container container, String config) {
        this.container = container;
        this.config = config;
    }
}
