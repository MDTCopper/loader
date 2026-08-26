package copper.launch.builder;

import copper.launch.util.*;

import java.io.*;
import java.nio.charset.*;
import java.util.*;

public class MixinDexMeta {
    public String id;
    public String version;
    public List<ModDescriptor> sources;

    public MixinDexMeta() {
        sources = new ArrayList<>();
    }

    public MixinDexMeta(File file) {
        this();
        try (var dis = new DataInputStream(new FileInputStream(file))) {
            id = dis.readUTF();
            version = dis.readUTF();
            int len = dis.readInt();
            for (int i = 0; i < len; i++) {
                String id = dis.readUTF();
                String version = dis.readUTF();
                sources.add(new ModDescriptor(id, version));
            }
        } catch (Throwable e) {
            throw new RuntimeException("failed to read mixin dex meta", e);
        }
    }

    public void write(File file) {
        try (var dos = new DataOutputStream(new FileOutputStream(file))) {
            dos.writeUTF(id);
            dos.writeUTF(version);
            dos.writeInt(sources.size());
            for (var source : sources) {
                dos.writeUTF(source.id);
                dos.writeUTF(source.version);
            }
        } catch (Throwable e) {
            throw new RuntimeException("failed to write mixin dex meta", e);
        }
    }

    public String sha256() {
        StringBuilder builder = new StringBuilder();
        builder.append("# Version\n");
        builder.append(version).append("\n");
        builder.append("# Mixin by Mods\n");
        for (var source : sources)
            builder.append(source.id).append(":").append(source.version).append("\n");
        return Hash.sha256(builder.toString().getBytes(StandardCharsets.UTF_8));
    }
}
