package copper.core.patch.impl;

import mindustry.mod.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Pseudo
@Mixin(targets = "mindustry.mod.Scripts")
public abstract class CScriptsBackport {
    @Shadow
    private Mods.LoadedMod currentMod;

    @Redirect(method = "run(Lmindustry/mod/Mods$LoadedMod;Larc/files/Fi;)V",
        at = @At(value = "INVOKE", target = "Lmindustry/mod/Scripts;run(Ljava/lang/String;Ljava/lang/String;Z)Z"))
    private boolean cRunWithModNamePrefix(Scripts obj, String src, String file, boolean wrap) {
        return run(src, currentMod.name + "/" + file, wrap);
    }

    @ModifyVariable(method = "run(Ljava/lang/String;Ljava/lang/String;Z)Z", at = @At("STORE"), name = "file", argsOnly = true)
    private String cStripDuplicatedModNamePrefix(String file) {
        return file.substring(currentMod.name.length() + 1);
    }

    @Shadow
    abstract boolean run(String src, String file, boolean wrap);
}
