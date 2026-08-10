package copper.core.patch.impl;

import copper.loader.*;
import mindustry.core.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

/**
 * Mixin patch: appends the Copper loader version to Mindustry's combined version string.
 */
@Mixin(Version.class)
public abstract class CVersion {
    @Inject(method = "combined", at = @At("RETURN"), cancellable = true)
    private static void cAddCopperVersion(CallbackInfoReturnable<String> ci) {
        ci.setReturnValue(ci.getReturnValue() + " + " + "copper v" + Loader.vars.loaderVersion.toString());
    }
}
