package copper.core.mod;

import copper.loader.mod.Mod;
import mindustry.mod.*;

/**
 * Mindustry {@link Mods.ModMeta} subclass that holds a back-reference to the originating
 * Copper {@link copper.loader.mod.Mod}. Used to wire Copper mod metadata into Mindustry's
 * mod registry.
 */
public class CopperModMeta extends Mods.ModMeta {
    /** The originating Copper mod. */
    public Mod copperMod;

    @Override
    public boolean isBlacklisted() {
        return false;
    }
}
