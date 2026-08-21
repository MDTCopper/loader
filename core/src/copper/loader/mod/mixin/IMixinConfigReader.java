package copper.loader.mod.mixin;

import copper.loader.container.*;
import copper.loader.container.info.*;
import copper.loader.mod.*;
import copper.loader.util.*;

/**
 * Reads Copper mixin config JSON, merging version-filtered mixin entries into a single list.
 *
 * <p>Each entry key in the config is a version filter expression (e.g. {@code "*"} or
 * {@code ">=8.0.27179"}). The corresponding value is either a single mixin class name
 * or an array of class names. The reader evaluates all entries against the target
 * {@link Version} and merges matching mixins into a flat array.</p>
 */
public interface IMixinConfigReader {
    /**
     * @param version the target version to filter mixin entries against
     * @param obj     the parsed mixin config JSON object, modified in place
     * @return the processed JSON config string with version-filtering resolved to a flat list
     */
    MixinInfo read(Container container, Version version, Jval obj);
}
