package copper.loader.container.info;

import copper.loader.container.*;

import java.util.*;

/**
 * Holds a mixin configuration string and the container that owns it.
 */
public class MixinInfo {
    /** The container that provides this mixin config. */
    public Container container;
    /** The raw mixin config JSON string. */
    public String config;

    public String packageName;

    public List<String> mixinName;

    public MixinInfo() {
        config = "{}";
        mixinName = new ArrayList<>();
    }
}
