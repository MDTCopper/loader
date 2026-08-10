package copper.loader.mod.meta;

import copper.loader.mod.*;
import copper.loader.util.*;

/**
 * Reads Copper mod metadata from a parsed JSON/HJSON value into a {@link Mod} object.
 */
public interface IMetaReader {
    /**
     * @param mod the mod to populate with metadata
     * @param obj the parsed {@code "meta"} JSON object
     */
    void read(Mod mod, Jval obj);
}
