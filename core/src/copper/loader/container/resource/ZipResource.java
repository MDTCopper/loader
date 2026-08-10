package copper.loader.container.resource;

import java.util.zip.*;

public class ZipResource implements IResource {
    public ZipFile file;

    public ZipResource(ZipFile file) {
        this.file = file;
    }

    @Override
    public byte[] read(String path) {
        try {
            ZipEntry entry = file.getEntry(path);
            if (entry == null)
                return null;
            try (var is = file.getInputStream(entry)) {
                return is == null ? null : is.readAllBytes();
            }
        } catch (Throwable e) {
            return null;
        }
    }
}
