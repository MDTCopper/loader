package copper.loader.container.resource;

import copper.loader.container.*;
import copper.loader.util.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;

public class BytecodeResource implements IResource {
    public Map<String, byte[]> content;

    public BytecodeResource(byte[] file, ClassFilter filter) {
        content = new HashMap<>();
        try (var zis = new ZipInputStream(new ByteArrayInputStream(file))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName().replace('\\', '/');
                if (name.endsWith(".class")) {
                    if (name.startsWith("/"))
                        name = name.substring(1);
                    if (filter != null) {
                        // remove .class
                        String className = name.substring(0, name.length() - 6).replace('/', '.');
                        if (!filter.check(className)) {
                            zis.closeEntry();
                            continue;
                        }
                    }
                    content.put(name, Streams.readAllBytes(zis));
                }
                zis.closeEntry();
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] read(String path) {
        return content.get(path);
    }
}
