package copper.launch.builder;

import copper.loader.func.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;

public class RuntimeDex extends CodePool {
    private BaseDexPool base;

    public RuntimeDex(BaseDexPool base) {
        this.base = base;
    }

    public void build(File jar) {
        try {
            DexMerger merger = new DexMerger();
            Set<String> names = new HashSet<>();
            ThrowableCons2<String, byte[]> process = (name, code) -> {
                if (names.add(name))
                    merger.addSource(code);
            };
            eachCode(process);
            base.eachCode(process);
            merger.merge();

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
