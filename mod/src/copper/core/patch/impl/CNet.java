package copper.core.patch.impl;

import copper.core.net.*;
import mindustry.net.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(Net.class)
public abstract class CNet {
    @Inject(method = "<clinit>", at = @At("RETURN"))
    private static void cRegisterExtendedPacket(CallbackInfo ci) {
        Net.registerPacket(ExtendedPacket::new);
    }
}
