package copper.loader.container.resource;

import java.io.*;

/**
 * Reads resources from a directory on the filesystem.
 */
public class FolderResource implements IResource {
    /** The root folder. */
    public File folder;

    /**
     * @param folder the root directory to read from
     */
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
