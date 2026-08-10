package copper.loader.container.info;

import copper.loader.container.*;

/**
 * Holds a mixin configuration string and the container that owns it.
 */
public class MixinInfo {
    /** The container that provides this mixin config. */
    public Container container;
    /** The raw mixin config JSON string. */
    public String config;

    public MixinInfo() {
        config = "{}";
    }

    public MixinInfo(Container container, String config) {
        this.container = container;
        this.config = config;
    }
}
