package copper.launch;

import copper.launch.util.*;
import copper.loader.*;
import copper.loader.func.*;
import copper.loader.util.*;
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
    private File packedBaseDexFolder;
    private File runtimeDexFolder;
    private File currentRuntimeDexFolder;

    /** Creates the cache folders under the given root. */
    public DexCache(File root) {
        this.root = root;
        baseDexFolder = new File(root, "base");
        packedBaseDexFolder = new File(root, "pack/base");
        runtimeDexFolder = new File(root, "pack/runtime");
        baseDexFolder.mkdirs();
        packedBaseDexFolder.mkdirs();
        runtimeDexFolder.mkdirs();
    }

    /** Picks the runtime folder for the currently enabled mods. */
    public void init() {
        currentRuntimeDexFolder = getCurrentRuntimeFolder();
        currentRuntimeDexFolder.mkdirs();
    }

    /** Returns the base dex file for a mod at a given version. */
    public File getBaseDexFile(String id, String version) {
        id = id.replace(':', '-');
        return new File(baseDexFolder, id + "-" + version + ".jar");
    }

    /** Returns the packed base dex file for a mod at a given version. */
    public File getPackedBaseDexFile(String id, String version) {
        id = id.replace(':', '-');
        return new File(packedBaseDexFolder, id + "-" + version + ".jar");
    }

    /** Returns the link file that points a runtime entry to a packed base dex. */
    public File getRuntimeDexLink(String id) {
        id = id.replace(':', '-');
        return new File(currentRuntimeDexFolder, id + ".link");
    }

    /**
     * Returns the dex file to actually load for {@code id}.
     * Follows the link file when present, otherwise the runtime dex file.
     */
    public File getRuntimeDexFile(String id) {
        File link = getRuntimeDexLink(id);
        if (link.exists()) {
            try (var fis = new FileInputStream(link)) {
                String name = new String(Streams.readAllBytes(fis), StandardCharsets.UTF_8);
                return new File(packedBaseDexFolder, name);
            } catch (Throwable ignored) {}
        }
        id = id.replace(':', '-');
        return new File(currentRuntimeDexFolder, id + ".jar");
    }

    /** Whether a runtime dex for the current mod set already exists. */
    public boolean isCurrentRuntimeExisted() {
        return getRuntimeDexFile("mindustry").exists();
    }

    /** Deletes the runtime folder of the current mod set (used on a failed build). */
    public void clearCurrentRuntime() {
        currentRuntimeDexFolder.delete();
    }

    /** Deletes all base and runtime dexes. */
    public void clear() {
        baseDexFolder.delete();
        packedBaseDexFolder.delete();
        runtimeDexFolder.delete();
    }

    /** Deletes every cache entry related to one mod id. */
    public void remove(String id) {
        id = id.replace(':', '-');
        for (String name : baseDexFolder.list()) {
            if (name.startsWith(id + "-"))
                (new File(baseDexFolder, name)).delete();
        }
        for (String name : packedBaseDexFolder.list()) {
            if (name.startsWith(id + "-"))
                (new File(baseDexFolder, name)).delete();
        }
        for (String name : runtimeDexFolder.list()) {
            if ((new File(runtimeDexFolder, name + "/" + id + ".jar")).exists())
                (new File(runtimeDexFolder, name)).delete();
            if ((new File(runtimeDexFolder, name + "/" + id + ".link")).exists())
                (new File(runtimeDexFolder, name)).delete();
        }
    }

    /**
     * The runtime folder is named after a sha256 hash of the enabled mod list
     * (game version + every mod id/version), so a different mod set yields a
     * different folder.
     */
    private File getCurrentRuntimeFolder() {
        try {
            ArrayList<String> list = new ArrayList<>();
            list.add("mindustry:" + Loader.game.version.toString());
            Loader.mods.eachMod(m -> list.add(m.id + ":" + m.version.toString()));

            String txt = "# Enabled Copper Mods\n";
            txt += String.join("\n", list);

            String hash = Hash.sha256(txt.getBytes(StandardCharsets.UTF_8));
            File folder = new File(runtimeDexFolder, hash);
            folder.mkdirs();
            return folder;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}