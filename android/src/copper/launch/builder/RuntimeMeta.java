package copper.launch.builder;

import java.io.*;
import java.util.*;

public class RuntimeMeta {
    public List<ModDescriptor> mods;
    public Map<String, String> jarLinks;

    public RuntimeMeta() {
        mods = new ArrayList<>();
        jarLinks = new HashMap<>();
    }

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
