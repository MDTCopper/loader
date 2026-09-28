package copper.core.patch.impl;

import copper.core.*;
import mindustry.*;
import mindustry.mod.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

/**
 * Mixin patch: keeps the Copper core mod from being reported as the cause of launcher crashes.
 */
@Pseudo
@Mixin(targets = "mindustry.net.CrashHandler")
public abstract class CCrashHandler {
    @Inject(method = "getModCause", at = @At("RETURN"), cancellable = true)
    private static void filterModCause(Throwable e, CallbackInfoReturnable<Mods.LoadedMod> ci) {
        if (Vars.mods == null)
            return;
        // the game is launched by copper loader, so copper.launch.* will appear in stacktrace
        // with `copper` in package name, the game's `CrashHandler.getMatches(...)` will match the copper core mod
        // but most of time, it isn't the true cause of the crash
        if (ci.getReturnValue() != null) {
            if (ci.getReturnValue().main instanceof CoreMod)
                ci.setReturnValue(null);
        }
    }
}
