package copper.core.patch.impl;

import arc.files.*;
import copper.loader.*;
import copper.loader.mod.*;
import mindustry.core.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

/**
 * Mixin patch: intercepts {@code Platform.loadJar()} to use the Copper container
 * classloader for Mindustry Java mods that have a main class.
 *
 * <p>Mindustry's default {@code loadJar} creates a new {@code URLClassLoader} for
 * each Java mod jar. Copper replaces this with its own {@link copper.loader.container.Container}
 * classloader for Mindustry mods that declare a {@code main} class, so that
 * class loading, dependency visibility, and mixin transformation are all managed
 * through the Copper container system.</p>
 */
@Mixin(Platform.class)
public interface CPlatform {
    @Inject(method = "loadJar", at = @At("HEAD"), cancellable = true)
    default void cLoadMindustryJavaMod(Fi jar, ClassLoader parent, CallbackInfoReturnable<ClassLoader> ci) throws Exception {
        Mod mod = Loader.mods.getModByFile(jar.file());
        if (mod instanceof MindustryMod && !mod.main.isEmpty())
            ci.setReturnValue(mod.container.getClassLoader());
    }
}
