package copper.core.patch.impl;

import arc.func.*;
import arc.scene.style.*;
import copper.core.patch.*;
import copper.loader.*;
import copper.loader.mod.*;
import mindustry.Vars;
import mindustry.mod.Mods;
import mindustry.ui.dialogs.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(SettingsMenuDialog.SettingsCategory.class)
public abstract class CSettingsCategory implements ICopperSettingsCategory {
    @Unique
    private String modName;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void cDetectMod(String name, Drawable icon, Cons<SettingsMenuDialog.SettingsTable> builder, CallbackInfo ci) {
        if (Vars.mods == null)
            return;
        // stackwalk is added in android 14
        var mod = Loader.mods.getModByClassLoader(builder.getClass().getClassLoader());
        if (mod != null) {
            modName = mod instanceof MindustryMod ?
                    mod.id.substring("mindustry:".length()) :
                    "copper-" + mod.id.replace(':', '-');
        } else {
            var stacks = (new Throwable()).getStackTrace();
            for (var stack : stacks) {
                String fileName = stack.getFileName();
                if (fileName != null && fileName.endsWith(".js")) {
                    for (var m : Vars.mods.list()) {
                        if (fileName.startsWith(m.name + "/")) {
                            modName = m.name;
                            break;
                        }
                    }
                }
                if (modName != null)
                    break;
            }
        }
    }

    @Override
    public Mods.LoadedMod getCopperDetectedLoadedMod() {
        if (Vars.mods == null || modName == null)
            return null;
        return Vars.mods.getMod(modName);
    }

    @Override
    public String getCopperDetectedModName() {
        return modName;
    }

    @Override
    public void setCopperDetectedModName(String modName) {
        this.modName = modName;
    }
}
