package copper.core.patch.impl;

import arc.func.*;
import mindustry.net.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

/**
 * Mixin patch: disables Mindustry's auto-update check and update dialog.
 */
@Mixin(BeControl.class)
public abstract class CBeControl {
    /** Forces {@code active()} to always return {@code false}. */
    @Inject(method = "active", at = @At("HEAD"), cancellable = true)
    private void cBanAutoCheckUpdate(CallbackInfoReturnable<Boolean> ci) {
        ci.setReturnValue(false);
    }

    /** Suppresses the update check by immediately calling the done callback. */
    @Inject(method = "checkUpdate", at = @At("HEAD"), cancellable = true)
    private void cBanCheckUpdate(Boolc done, CallbackInfo ci) {
        done.get(true);
        ci.cancel();
    }

    /** Prevents the update dialog from appearing. */
    @Inject(method = "showUpdateDialog", at = @At("HEAD"), cancellable = true)
    private void cBanUpdateDialog(CallbackInfo ci) {
        ci.cancel();
    }
}
