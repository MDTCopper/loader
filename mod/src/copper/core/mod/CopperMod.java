package copper.core.mod;

import mindustry.mod.*;

/**
 * Base class for Copper mod main classes.
 */
public abstract class CopperMod extends Mod {
    /** Called during mod loading to register custom network packets to {@link copper.core.net.CopperNet}. */
    public void registerPackets() {}
}
