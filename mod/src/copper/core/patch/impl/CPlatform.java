package copper.core.patch.impl;

import arc.files.*;
import copper.loader.*;
import copper.loader.mod.*;
import mindustry.core.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(Platform.class)
public interface CPlatform {
    @Inject(method = "loadJar", at = @At("HEAD"), cancellable = true)
    default void cLoadMindustryJavaMod(Fi jar, ClassLoader parent, CallbackInfoReturnable<ClassLoader> ci) throws Exception {
        Mod mod = Loader.mods.getModByFile(jar.file());
        if (mod instanceof MindustryMod && !mod.main.isEmpty())
            ci.setReturnValue(mod.container.getClassLoader());
    }
}
