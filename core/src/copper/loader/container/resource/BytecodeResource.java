package copper.loader.container.resource;

import copper.loader.container.*;
import copper.loader.util.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;

/**
 * Holds class files in memory, read from a jar in the constructor.
 *
 * <p>Unlike {@link ZipResource}, the bytecode is loaded eagerly into a map
 * so it can be read many times without keeping the jar open.</p>
 */
public class BytecodeResource implements IResource {
    /** Map of resource path (with slashes) to raw bytes. */
    public Map<String, byte[]> content;

    /**
     * Reads all {@code .class} entries of a jar into memory.
     *
     * @param file   the jar bytes
     * @param filter optional class filter; {@code null} keeps everything
     */
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