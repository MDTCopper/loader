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
    /** The package that holds the mixin classes. */
    public String packageName;
    /** Names of the mixin classes listed in the config. */
    public List<String> mixinName;

    public MixinInfo() {
        config = "{}";
        mixinName = new ArrayList<>();
    }
}
