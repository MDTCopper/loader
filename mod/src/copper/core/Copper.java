package copper.core;

import arc.*;
import arc.files.*;
import arc.util.*;
import arc.util.serialization.*;
import copper.core.mod.*;
import copper.core.util.*;
import copper.loader.*;
import copper.loader.mod.*;
import mindustry.Vars;
import java.util.*;

public class Copper {
    public static Fi getDataFolder() {
        return new Fi(Loader.vars.loaderDataFolder);
    }

    public static Fi getModsDataFolder() {
        return new Fi(Loader.vars.copperModDataFolder);
    }

    public static Fi getModsFolder() {
        return new Fi(Loader.vars.copperModFolder);
    }

    // clazz -> main class of mod
    public static @Nullable Mod getMod(Class<? extends CopperMod> clazz) {
        String id = null;
        String name = clazz.getName();
        int i = name.indexOf('.');
        if (i != -1)
            i = name.indexOf('.', i + 1);
        if (i != -1)
            id = name.substring(0, i);
        return id == null ? null : Loader.mods.getModById(id.replace('.', ':'));
    }

    public static Mod getModNonNull(Class<? extends CopperMod> clazz) {
        Mod mod = getMod(clazz);
        if (mod == null)
            throw new ArcRuntimeException("copper mod not found: " + clazz.getName());
        return mod;
    }

    public static Fi getModRoot(Class<? extends CopperMod> clazz) {
        Mod mod = getModNonNull(clazz);
        Fi file = new Fi(mod.file);
        return file.isDirectory() ? file : new ZipFi(file);
    }

    public static Fi getModFile(Class<? extends CopperMod> clazz, String path) {
        if (path.startsWith("/"))
            path = path.substring(1);
        if (path.endsWith("/"))
            path = path.substring(0, path.length() - 1);
        Mod mod = getModNonNull(clazz);
        Fi file = new Fi(mod.file);
        if (file.isDirectory()) {
            return file.child(path);
        } else {
            file = new ZipFi(file);
            for (String name : path.split("/"))
                file = file.child(name);
            return file;
        }
    }

    public static Fi getModAssetFolder(Class<? extends CopperMod> clazz) {
        return getModFile(clazz, "assets");
    }

    public static Fi getModAsset(Class<? extends CopperMod> clazz, String path) {
        return getModFile(clazz, "assets/" + path);
    }

    public static Settings createSettings(Class<? extends CopperMod> clazz) {
        Mod mod = getModNonNull(clazz);
        CopperSettings settings = new CopperSettings();
        settings.setDataDirectory(getModsDataFolder().child(mod.id.replace(':', '-')));
        settings.load();
        return settings;
    }

    public static I18NBundle createBundle(Class<? extends CopperMod> clazz) {
        Fi handle = new ForwardedFi("copper/bundles/bundle", path -> getModAsset(clazz, path));
        return I18NBundle.createBundle(handle, Locale.getDefault());
    }

    public static void translateModMeta(Class<? extends CopperMod> clazz, I18NBundle bundle) {
        var copperMod = getModNonNull(clazz);
        copperMod.name = bundle.get("mod.name", copperMod.name);
        copperMod.author = bundle.get("mod.author", copperMod.author);
        copperMod.description = bundle.get("mod.description", copperMod.description);
        Jval extra = Jval.read(copperMod.extraMeta);
        if (extra.has("subtitle"))
            extra.put("subtitle", bundle.get("subtitle", extra.getString("subtitle")));
        copperMod.extraMeta = extra.toString(Jval.Jformat.plain);

        if (Vars.mods == null)
            return;
        var mod = Vars.mods.getMod(clazz);
        if (mod == null)
            return;
        mod.meta.displayName = bundle.get("mod.name", mod.meta.name);
        mod.meta.author = bundle.get("mod.name", mod.meta.author);
        mod.meta.description = bundle.get("mod.name", mod.meta.description);
        mod.meta.subtitle = bundle.get("mod.name", mod.meta.subtitle);
    }
}