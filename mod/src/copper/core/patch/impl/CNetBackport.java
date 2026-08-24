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
@Pseudo  // shut the ap up
@Mixin(targets = "mindustry.net.Net")
public abstract class CNetBackport {
    @Inject(method = "<clinit>", at = @At("RETURN"))
    private static void cRegisterExtendedPacket(CallbackInfo ci) {
        // the extended packet type must stay out of the vanilla game
        if (!Loader.vars.vanillaMode)
            registerPacket(ExtendedPacket::new);
    }

    // return value is changed to int in v159
    @Shadow
    private static <T extends Packet> void registerPacket(Prov<T> cons) {}
}
