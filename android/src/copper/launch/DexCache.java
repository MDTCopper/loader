package copper.launch;

import copper.launch.builder.*;
import copper.launch.util.*;
import copper.loader.*;

import java.io.*;
import java.nio.charset.*;
import java.util.*;

/**
 * Manages the on-disk dex cache layout.
 *
 * <p>Layout under the cache root:</p>
 * <ul>
 *   <li>{@code base/<id>-<version>.jar} — per-mod dex before any mixins</li>
 *   <li>{@code pack/base/<id>-<version>.jar} — final packed dex for vanilla (no-mixin) mods</li>
 *   <li>{@code pack/runtime/<hash>/...} — the dex set for the currently enabled mods,
 *       where the folder name is a hash of the enabled mod list</li>
 * </ul>
 */
public class DexCache {
    private File root;
    private File baseDexFolder;
    private File mixinDexFolder;
    private File runtimeMetaFolder;
    private File currentRuntimeFile;
    private RuntimeMeta currentRuntimeMeta;

    /** Creates the cache folders under the given root. */
    public DexCache(File root) {
        this.root = root;
        baseDexFolder = new File(root, "base");
        mixinDexFolder = new File(root, "mixin");
        runtimeMetaFolder = new File(root, "runtime");
        currentRuntimeMeta = new RuntimeMeta();
        baseDexFolder.mkdirs();
        mixinDexFolder.mkdirs();
        runtimeMetaFolder.mkdirs();
    }

    /** Picks the runtime folder for the currently enabled mods. */
    public void init() {
        currentRuntimeFile = getCurrentRuntimeFile();
        if (currentRuntimeFile.exists()) {
            currentRuntimeMeta = new RuntimeMeta(currentRuntimeFile);
        }
    }

    /** Returns the base dex file for a mod at a given version. */
    public File getBaseDexFile(String id, String version) {
        id = id.replace(':', '-');
        var folder = new File(baseDexFolder, id);
        folder.mkdirs();
        return new File(folder, Hash.sha256(version.getBytes(StandardCharsets.UTF_8)) + ".jar");
    }

    public File getMixinDexFile(MixinDexMeta meta) {
        return new File(getMixinDexFolder(meta), "rt.jar");
    }

    public File getMixinDexMetaFile(MixinDexMeta meta) {
        return new File(getMixinDexFolder(meta), "meta");
    }

    /**
     * Returns the dex file to actually load for {@code id}.
     * Follows the link file when present, otherwise the runtime dex file.
     */
    public File getRuntimeDexFile(String id) {
        String hash = currentRuntimeMeta.jarLinks.get(id);
        if (hash == null)
            return null;
        id = id.replace(':', '-');
        return new File(mixinDexFolder, id + "/" + hash + "/rt.jar");
    }

    public void updateCurrentRuntime(RuntimeMeta meta) {
        try {
            meta.write(currentRuntimeFile);
            currentRuntimeMeta = meta;
        } catch (Throwable e) {
            if (currentRuntimeFile != null)
                currentRuntimeFile.delete();
            throw new RuntimeException("failed to update current runtime", e);
        }
    }

    /** Whether a runtime dex for the current mod set already exists. */
    public boolean isCurrentRuntimeExisted() {
        return currentRuntimeFile.exists();
    }

    /** Deletes the runtime folder of the current mod set (used on a failed build). */
    public void clearCurrentRuntime() {
        currentRuntimeFile.delete();
    }

    /** Deletes all base and runtime dexes. */
    public void clear() {
        baseDexFolder.delete();
        mixinDexFolder.delete();
        runtimeMetaFolder.delete();
    }

    /** Deletes every cache entry related to one mod id. */
    public void remove(String id) {
        id = id.replace(':', '-');
        for (String name : baseDexFolder.list()) {
            if (name.equals(id))
                (new File(baseDexFolder, name)).delete();
        }
        for (String name : mixinDexFolder.list()) {
            if (name.equals(id))
                (new File(baseDexFolder, name)).delete();
        }
        for (String name : runtimeMetaFolder.list()) {
            File file = new File(runtimeMetaFolder, name);
            RuntimeMeta meta = new RuntimeMeta(file);
            if (meta.jarLinks.containsKey(id))
                file.delete();
        }
    }

    private File getMixinDexFolder(MixinDexMeta meta) {
        File folder = new File(mixinDexFolder, meta.id.replace(':', '-') + "/" + meta.sha256());
        folder.mkdirs();
        return folder;
    }

    /**
     * The runtime folder is named after a sha256 hash of the enabled mod list
     * (game version + every mod id/version), so a different mod set yields a
     * different folder.
     */
    private File getCurrentRuntimeFile() {
        try {
            ArrayList<String> list = new ArrayList<>();
            list.add("mindustry:" + Loader.game.version.toString());
            Loader.mods.eachMod(m -> list.add(m.id + ":" + m.version.toString()));

            String txt = "# Enabled Copper Mods\n";
            txt += String.join("\n", list);

            String hash = Hash.sha256(txt.getBytes(StandardCharsets.UTF_8));
            return new File(runtimeMetaFolder, hash);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}