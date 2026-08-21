package copper.launch;

import copper.launch.util.*;
import copper.loader.*;
import copper.loader.func.*;
import copper.loader.util.*;
import java.io.*;
import java.nio.charset.*;
import java.util.*;

public class DexCache {
    private File root;
    private File baseDexFolder;
    private File packedBaseDexFolder;
    private File runtimeDexFolder;
    private File currentRuntimeDexFolder;

    public DexCache(File root) {
        this.root = root;
        baseDexFolder = new File(root, "base");
        packedBaseDexFolder = new File(root, "pack/base");
        runtimeDexFolder = new File(root, "pack/runtime");
        baseDexFolder.mkdirs();
        packedBaseDexFolder.mkdirs();
        runtimeDexFolder.mkdirs();
    }

    public void init() {
        currentRuntimeDexFolder = getCurrentRuntimeFolder();
        currentRuntimeDexFolder.mkdirs();
    }

    public File getBaseDexFile(String id, String version) {
        id = id.replace(':', '-');
        return new File(baseDexFolder, id + "-" + version + ".jar");
    }

    public File getPackedBaseDexFile(String id, String version) {
        id = id.replace(':', '-');
        return new File(packedBaseDexFolder, id + "-" + version + ".jar");
    }

    public File getRuntimeDexLink(String id) {
        id = id.replace(':', '-');
        return new File(currentRuntimeDexFolder, id + ".link");
    }

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

    public boolean isCurrentRuntimeExisted() {
        return getRuntimeDexFile("mindustry").exists();
    }

    public void clearCurrentRuntime() {
        currentRuntimeDexFolder.delete();
    }

    public void clear() {
        baseDexFolder.delete();
        runtimeDexFolder.delete();
    }

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
