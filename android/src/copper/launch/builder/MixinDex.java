package copper.launch.builder;

import copper.loader.func.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;

/**
 * Merges the base dex pool of a mod with its mixin delta into the final mixin dex jar.
 *
 * <p>The {@link DexMerger} re-packages all dex entries (base + delta) into one jar
 * with the usual {@code classes.dex}, {@code classes2.dex}, ... naming.</p>
 */
public class MixinDex extends CodePool {
    private BaseDexPool base;

    public MixinDex(BaseDexPool base) {
        this.base = base;
    }

    /** Builds the mixin dex jar, deduplicating classes by name. */
    public void build(File jar) {
        try {
            DexMerger merger = new DexMerger();
            Set<String> names = new HashSet<>();
            ThrowableCons2<String, byte[]> process = (name, code) -> {
                // only feed one copy of each class to the merger
                if (names.add(name))
                    merger.addSource(code);
            };
            eachCode(process);
            base.eachCode(process);
            merger.merge();

            // write classes.dex, classes2.dex, ... into the jar
            try (var zos = new ZipOutputStream(new FileOutputStream(jar))) {
                var codes = merger.getBytecodes();
                for (int i = 0; i < codes.size(); i++) {
                    String name = "classes" + (i > 0 ? i : "") + ".dex";
                    ZipEntry entry = new ZipEntry(name);
                    zos.putNextEntry(entry);
                    zos.write(codes.get(i));
                    zos.closeEntry();
                }
            }
        } catch (Throwable e) {
            jar.delete();
            throw new RuntimeException("failed to build runtime dex", e);
        }
    }
}