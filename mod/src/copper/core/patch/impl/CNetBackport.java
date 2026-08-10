package copper.core.patch.impl;

import arc.func.*;
import copper.core.net.*;
import mindustry.net.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

/**
 * Mixin patch: registers the {@link ExtendedPacket} wrapper during {@link Net} class initialization.
 */
@Mixin(Net.class)
public abstract class CNetBackport {
    @Inject(method = "<clinit>", at = @At("RETURN"))
    private static void cRegisterExtendedPacket(CallbackInfo ci) {
        registerPacket(ExtendedPacket::new);
    }

    // return value is changed to int in v159
    @Shadow
    private static <T extends Packet> void registerPacket(Prov<T> cons) {}
}
