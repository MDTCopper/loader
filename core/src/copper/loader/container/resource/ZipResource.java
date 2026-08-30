package copper.loader.container.resource;

import copper.loader.container.*;
import copper.loader.util.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;

/**
 * Reads resources from a zip/jar file.
 */
public class ZipResource implements IResource {
    /** The opened zip file. */
    public ZipFile file;

    /**
     * @param file the zip file to read from
     */
    public ZipResource(ZipFile file) {
        this.file = file;
    }

    @Override
    public byte[] read(String path) {
        try {
            ZipEntry entry = file.getEntry(path);
            if (entry == null || entry.isDirectory())
                return null;
            try (var is = file.getInputStream(entry)) {
                return is == null ? null : Streams.readAllBytes(is);
            }
        } catch (Throwable e) {
            return null;
        }
    }
}
