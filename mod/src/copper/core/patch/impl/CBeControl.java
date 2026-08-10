package copper.core.patch.impl;

import arc.func.*;
import mindustry.net.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(BeControl.class)
public abstract class CBeControl {
    @Inject(method = "active", at = @At("HEAD"), cancellable = true)
    void cBanAutoCheckUpdate(CallbackInfoReturnable<Boolean> ci) {
        ci.setReturnValue(false);
    }

    @Inject(method = "checkUpdate", at = @At("HEAD"), cancellable = true)
    void cBanCheckUpdate(Boolc done, CallbackInfo ci) {
        done.get(true);
        ci.cancel();
    }

    @Inject(method = "showUpdateDialog", at = @At("HEAD"), cancellable = true)
    void cBanUpdateDialog(CallbackInfo ci) {
        ci.cancel();
    }
}
