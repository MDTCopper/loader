package copper.launch.builder;

import java.io.*;
import java.util.*;

/**
 * Describes the dex set of the currently enabled mods: the ordered mod list and,
 * for each mod, the sha256 hash of the mixin dex jar to load at runtime. Stored
 * as the {@code runtime/<hash>} file of the dex cache.
 */
public class RuntimeMeta {
    /** Loaded mods, in load order. */
    public List<ModDescriptor> mods;
    /** Maps each mod id to the hash of its mixin dex jar ({@code mixin/<id>/<hash>/rt.jar}). */
    public Map<String, String> jarLinks;

    /** Creates an empty runtime meta. */
    public RuntimeMeta() {
        mods = new ArrayList<>();
        jarLinks = new HashMap<>();
    }

    /** Loads a runtime meta from the given file. */
    public RuntimeMeta(File file) {
        this();
        try (var dis = new DataInputStream(new FileInputStream(file))) {
            int len = dis.readInt();
            for (int i = 0; i < len; i++) {
                String id = dis.readUTF();
                String ver = dis.readUTF();
                mods.add(new ModDescriptor(id, ver));
            }
            len = dis.readInt();
            for (int i = 0; i < len; i++) {
                String id = dis.readUTF();
                String link = dis.readUTF();
                jarLinks.put(id, link);
            }
        } catch (Throwable e) {
            throw new RuntimeException("failed to read runtime meta", e);
        }
    }

    /** Serializes this runtime meta to the given file. */
    public void write(File file) {
        try (var dos = new DataOutputStream(new FileOutputStream(file))) {
            dos.writeInt(mods.size());
            for (var mod : mods) {
                dos.writeUTF(mod.id);
                dos.writeUTF(mod.version);
            }
            dos.writeInt(jarLinks.size());
            for (var link : jarLinks.entrySet()) {
                dos.writeUTF(link.getKey());
                dos.writeUTF(link.getValue());
            }
        } catch (Throwable e) {
            throw new RuntimeException("failed to write runtime meta", e);
        }
    }
}
