package copper.launch.builder;

import copper.launch.util.*;
import copper.loader.util.*;
import java.io.*;
import java.nio.charset.*;
import java.util.*;

/**
 * Describes one mixin dex jar: the id/version of the owning mod and the versions
 * of every mod whose mixins were applied. The {@link #sha256()} hash keys the jar
 * folder {@code mixin/<id>/<hash>/rt.jar}; this meta is stored next to it as
 * {@code meta}.
 */
public class MixinDexMeta {
    /** Id of the mod owning the mixin dex. */
    public String id;
    /** Version of the owning mod at build time. */
    public String version;
    /** Mods whose mixins were applied, paired with their versions. */
    public List<ModDescriptor> sources;

    /** Creates an empty mixin dex meta. */
    public MixinDexMeta() {
        sources = new ArrayList<>();
    }

    /** Loads a mixin dex meta from the given file. */
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

    /** Serializes this mixin dex meta to the given file. */
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

    /**
     * Returns the sha256 hash keying the mixin dex folder, derived from the mod
     * version and the versions of the mixin source mods.
     */
    public String sha256() {
        sources.sort(Structs.comparing(m -> m.id));
        StringBuilder builder = new StringBuilder();
        builder.append("# Version\n");
        builder.append(version).append("\n");
        builder.append("# Mixin by Mods\n");
        for (var source : sources)
            builder.append(source.id).append(":").append(source.version).append("\n");
        return Hash.sha256(builder.toString().getBytes(StandardCharsets.UTF_8));
    }
}
