package copper.core.patch.impl;

import copper.core.util.*;
import mindustry.mod.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.gen.*;

@Internal
@Pseudo
@Mixin(targets = "mindustry.mod.Scripts")
public interface ICScriptsAccessor {
    @Accessor
    Mods.LoadedMod getCurrentMod();
}
