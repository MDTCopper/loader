package copper.loader.container.resource;

import java.io.*;

public class FolderResource implements IResource {
    public File folder;

    public FolderResource(File folder) {
        this.folder = folder;
    }

    @Override
    public byte[] read(String path) {
        try {
            File file = new File(folder, path);
            if (file.exists() && file.isFile()) {
                try (var fis = new FileInputStream(file)) {
                    return fis.readAllBytes();
                }
            }
            return null;
        } catch (Throwable e) {
            return null;
        }
    }
}
