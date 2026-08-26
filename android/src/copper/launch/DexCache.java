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
 *   <li>{@code base/<id>/<sha256(version)>.jar} — per-mod dex compiled before any
 *       mixins, keyed by the mod version</li>
 *   <li>{@code mixin/<id>/<hash>/rt.jar} (+ {@code meta}) — the mixin dex of one mod,
 *       keyed by a hash of its version and the versions of its mixin sources</li>
 *   <li>{@code runtime/<hash>} — the {@link RuntimeMeta} file of the currently enabled
 *       mods, keyed by a hash of the enabled mod list</li>
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

    /** Loads the runtime meta of the currently enabled mods, if it exists. */
    public void init() {
        currentRuntimeFile = getCurrentRuntimeFile();
        if (currentRuntimeFile.exists()) {
            currentRuntimeMeta = new RuntimeMeta(currentRuntimeFile);
        }
    }

    /**
     * Returns the base dex file for a mod at a given version,
     * named by a hash of the version under {@code base/<id>}.
     */
    public File getBaseDexFile(String id, String version) {
        id = id.replace(':', '-');
        var folder = new File(baseDexFolder, id);
        folder.mkdirs();
        return new File(folder, Hash.sha256(version.getBytes(StandardCharsets.UTF_8)) + ".jar");
    }

    /** Returns the mixin dex jar of a mod at its {@code mixin/<id>/<hash>/rt.jar} path. */
    public File getMixinDexFile(MixinDexMeta meta) {
        return new File(getMixinDexFolder(meta), "rt.jar");
    }

    /** Returns the meta file stored next to a mod's mixin dex jar. */
    public File getMixinDexMetaFile(MixinDexMeta meta) {
        return new File(getMixinDexFolder(meta), "meta");
    }

    /**
     * Returns the dex jar to load for {@code id} at runtime, resolving the mixin
     * dex hash recorded in the current runtime meta, or {@code null} if the mod
     * has no entry.
     */
    public File getRuntimeDexFile(String id) {
        String hash = currentRuntimeMeta.jarLinks.get(id);
        if (hash == null)
            return null;
        id = id.replace(':', '-');
        return new File(mixinDexFolder, id + "/" + hash + "/rt.jar");
    }

    /**
     * Persists {@code meta} as the current runtime meta file and replaces the
     * in-memory copy; deletes the file again if writing fails.
     */
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

    /** Whether the runtime meta for the current mod set already exists. */
    public boolean isCurrentRuntimeExisted() {
        return currentRuntimeFile.exists();
    }

    /** Deletes the runtime meta of the current mod set (used on a failed build). */
    public void clearCurrentRuntime() {
        currentRuntimeFile.delete();
    }

    /** Deletes every cached base dex, mixin dex, and runtime meta. */
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
            File folder = new File(baseDexFolder, name);
            if (name.equals(id)) {
                folder.delete();
            } else {
                // also remove caches mixin by this mod
                for (String hash : folder.list()) {
                    File mixinFolder = new File(folder, hash);
                    File metaFile = new File(mixinFolder, "meta");
                    if (metaFile.exists()) {
                        MixinDexMeta meta = new MixinDexMeta(metaFile);
                        for (var src : meta.sources) {
                            if (src.id.equals(id)) {
                                mixinFolder.delete();
                                break;
                            }
                        }

                    }
                }
            }
        }
        for (String name : runtimeMetaFolder.list()) {
            File file = new File(runtimeMetaFolder, name);
            RuntimeMeta meta = new RuntimeMeta(file);
            if (meta.jarLinks.containsKey(id))
                file.delete();
        }
    }

    /** Returns the per-mod mixin dex folder for {@code meta}, creating it if needed. */
    private File getMixinDexFolder(MixinDexMeta meta) {
        File folder = new File(mixinDexFolder, meta.id.replace(':', '-') + "/" + meta.sha256());
        folder.mkdirs();
        return folder;
    }

    /**
     * The runtime meta file is named after a sha256 hash of the enabled mod list
     * (game version + every mod id/version), so a different mod set yields a
     * different file.
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