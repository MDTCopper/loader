package copper.core.patch.impl;

import arc.func.*;
import copper.core.net.*;
import copper.loader.*;
import mindustry.net.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

/**
 * Mixin patch: registers the {@link ExtendedPacket} wrapper during {@link Net} class initialization.
 */
@Mixin(Net.class)
public abstract class CNet {
    @Inject(method = "<clinit>", at = @At("RETURN"))
    private static void cRegisterExtendedPacket(CallbackInfo ci) {
        // the extended packet type must stay out of the vanilla game
        if (!Loader.vars.vanillaMode)
            Net.registerPacket(ExtendedPacket::new);
    }
}
