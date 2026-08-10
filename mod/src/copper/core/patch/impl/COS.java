package copper.core.patch.impl;

import arc.util.*;
import copper.loader.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

/**
 * Mixin patch: injects the Copper game data directory into Mindustry's
 * {@code OS.env()} and {@code OS.hasEnv()} methods.
 */
@Mixin(OS.class)
public abstract class COS {
    @Inject(method = "env", at = @At("HEAD"), cancellable = true)
    private static void cInjectDataFolderEnv(String name, CallbackInfoReturnable<String> ci) {
        if (name.equals("MINDUSTRY_DATA_DIR"))
            ci.setReturnValue(Loader.vars.gameDataFolder.getAbsolutePath());
    }

    @Inject(method = "hasEnv", at = @At("HEAD"), cancellable = true)
    private static void cInjectDataFolderHasEnv(String name, CallbackInfoReturnable<Boolean> ci) {
        if (name.equals("MINDUSTRY_DATA_DIR"))
            ci.setReturnValue(true);
    }
}
