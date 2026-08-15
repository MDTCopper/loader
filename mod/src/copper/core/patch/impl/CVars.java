package copper.core.patch.impl;

import arc.files.*;
import copper.loader.*;
import copper.loader.util.*;
import mindustry.Vars;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import java.io.*;

/**
 * Mixin patch: when running without a console, replaces Mindustry's color-coded log tags
 * with plain-text equivalents for readability in non-interactive terminals. Redirects
 * Mindustry logs to Copper logs.
 */
@Mixin(Vars.class)
public class CVars {
    @ModifyVariable(method = "loadLogger", at = @At("STORE"), ordinal = 1)
    private static String[] cApplyNonConsoleTags(String[] stags) {
        if (System.console() != null)
            return stags;
        return new String[]{"[D]", "[I]", "[W]", "[E]", ""};
    }

    @ModifyVariable(method = "loadFileLogger(Larc/files/Fi;)V", at = @At("STORE"), ordinal = 0)
    private static Writer cReplaceLogFileWritter(Writer writer) {
        Writer replaced = Log.getLogFileWriter();
        if (replaced != null) {
            return replaced;
        } else {
            return (new Fi(Loader.vars.gameDataFolder))
                    .child("last_log.txt")
                    .writer(false);
        }
    }
}
