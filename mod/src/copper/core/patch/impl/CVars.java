package copper.core.patch.impl;

import mindustry.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

/**
 * Mixin patch: when running without a console, replaces Mindustry's color-coded log tags
 * with plain-text equivalents for readability in non-interactive terminals.
 */
@Mixin(Vars.class)
public class CVars {
    @ModifyVariable(method = "loadLogger", at = @At("STORE"), ordinal = 1)
    private static String[] cApplyNonConsoleTags(String[] stags) {
        if (System.console() != null)
            return stags;
        return new String[]{"[D]", "[I]", "[W]", "[E]", ""};
    }
}
