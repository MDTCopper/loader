package copper.launch.builder;

import copper.loader.func.*;
import copper.loader.util.*;

import java.io.*;
import java.util.zip.*;

/**
 * A {@link CodePool} that can be stored as a jar of dex entries.
 *
 * <p>This is the per-mod dex result before any mixin is applied. It is written
 * once per mod/version and later merged into the mixin dex files.</p>
 */
public class BaseDexPool extends CodePool {
    public BaseDexPool() {
        super();
    }

    /** Loads a pool from a jar of {@code .dex} entries. */
    public BaseDexPool(File zip) {
        try (var zis = new ZipInputStream(new FileInputStream(zip))) {
            ZipEntry e;
            while ((e = zis.getNextEntry()) != null) {
                String name = e.getName();
                if (name.endsWith(".dex")) {
                    // the entry path is the class path without the .dex suffix
                    name = name.substring(0, name.length() - 4);
                    putCode(name, Streams.readAllBytes(zis));
                }
                zis.closeEntry();
            }
        } catch (Throwable e) {
            throw new RuntimeException("failed to read base dex pool", e);
        }
    }

    /** Writes the pool as a jar of {@code .dex} entries. */
    public void write(File zip) {
        try (var zos = new ZipOutputStream(new FileOutputStream(zip))) {
            eachCode((name, code) -> {
                name = name.replace('.', '/') + ".dex";
                var entry = new ZipEntry(name);
                zos.putNextEntry(entry);
                zos.write(code);
                zos.closeEntry();
            });
        } catch (Throwable e) {
            zip.delete();
            throw new RuntimeException("failed to write base dex pool", e);
        }
    }
}