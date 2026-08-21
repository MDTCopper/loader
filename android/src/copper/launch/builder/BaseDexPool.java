package copper.launch.builder;

import copper.loader.func.*;
import copper.loader.util.*;

import java.io.*;
import java.util.zip.*;

public class BaseDexPool extends CodePool {
    public BaseDexPool() {
        super();
    }

    public BaseDexPool(File zip) {
        try (var zis = new ZipInputStream(new FileInputStream(zip))) {
            ZipEntry e;
            while ((e = zis.getNextEntry()) != null) {
                String name = e.getName();
                if (name.endsWith(".dex")) {
                    name = name.substring(0, name.length() - 4);
                    putCode(name, Streams.readAllBytes(zis));
                }
                zis.closeEntry();
            }
        } catch (Throwable e) {
            throw new RuntimeException("failed to read base dex pool", e);
        }
    }

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
