package copper.core;

import arc.util.*;
import copper.core.mod.*;

/**
 * The Copper core mod's main class.
 */
public class CoreMod extends CopperMod {
    public static I18NBundle bundles;

    public CoreMod() {
        bundles = Copper.createBundle(CoreMod.class);
        Copper.translateModMeta(CoreMod.class, bundles);
    }
}
