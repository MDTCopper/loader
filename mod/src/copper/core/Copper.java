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

/**
 * Utility methods for Copper mods running inside Mindustry.
 */
public class Copper {
    /** The Copper loader's own data directory. */
    public static Fi getDataFolder() {
        return new Fi(Loader.vars.loaderDataFolder);
    }

    /** Directory where Copper mods store persistent data. */
    public static Fi getModsDataFolder() {
        return new Fi(Loader.vars.copperModDataFolder);
    }

    /** Directory containing Copper mod jars. */
    public static Fi getModsFolder() {
        return new Fi(Loader.vars.copperModFolder);
    }

    /**
     * Looks up a Copper mod by its main class.
     *
     * <p>The mod id is derived from the first two segments of the class package name.
     * For example, {@code author.mod.SomeClass} maps to mod id {@code "author:mod"}.</p>
     *
     * @param clazz the main class of the Copper mod
     * @return the mod, or {@code null} if not found
     */
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

    /** Like {@link #getMod}, but throws if the mod is not found. */
    public static Mod getModNonNull(Class<? extends CopperMod> clazz) {
        Mod mod = getMod(clazz);
        if (mod == null)
            throw new ArcRuntimeException("copper mod not found: " + clazz.getName());
        return mod;
    }

    /** Returns the root {@link Fi} of a mod's jar or directory. */
    public static Fi getModRoot(Class<? extends CopperMod> clazz) {
        Mod mod = getModNonNull(clazz);
        Fi file = new Fi(mod.file);
        return file.isDirectory() ? file : new ZipFi(file);
    }

    /** Returns a file inside a mod's jar or directory. */
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

    /** Returns the {@code assets} folder inside a mod. */
    public static Fi getModAssetFolder(Class<? extends CopperMod> clazz) {
        return getModFile(clazz, "assets");
    }

    /** Returns a file inside the mod's {@code assets} folder. */
    public static Fi getModAsset(Class<? extends CopperMod> clazz, String path) {
        return getModFile(clazz, "assets/" + path);
    }

    /** Creates a {@link Settings} instance backed by a Copper mod's data directory. */
    public static Settings createSettings(Class<? extends CopperMod> clazz) {
        Mod mod = getModNonNull(clazz);
        CopperSettings settings = new CopperSettings();
        settings.setDataDirectory(getModsDataFolder().child(mod.id.replace(':', '-')));
        settings.load();
        return settings;
    }

    /** Creates an i18n bundle from a mod's {@code bundles/bundle} asset. */
    public static I18NBundle createBundle(Class<? extends CopperMod> clazz) {
        Fi handle = new ForwardedFi("copper/bundles/bundle", path -> getModAsset(clazz, path));
        return I18NBundle.createBundle(handle, Locale.getDefault());
    }

    /**
     * Applies an i18n bundle to translate mod metadata fields
     * (name, author, description, subtitle) for both Copper and Mindustry mod registries.
     */
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
