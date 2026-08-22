package copper.core.util;

import arc.*;
import arc.files.*;
import copper.loader.func.*;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;

/**
 * A forwarded {@link Fi} that delegates all operations to a target provided by a function.
 *
 * <p>Used to create virtual file handles that resolve dynamically (e.g., loading
 * resources from inside a mod's jar via {@link copper.core.Copper#getModAsset}).</p>
 */
public class ForwardedFi extends Fi {
    protected Func<String, Fi> provider;
    protected Fi target;

    /**
     * @param path     the virtual path
     * @param provider function that maps a path to the actual backing {@link Fi}
     */
    public ForwardedFi(String path, Func<String, Fi> provider) {
        super(path, Files.FileType.absolute);
        this.provider = provider;
        target = provider.get(path);
    }

    @Override
    public Files.FileType type() {
        return target.type();
    }

    @Override
    public File file() {
        return target.file();
    }

    // keep the virtual path here; the real file may not exist on disk
    @Override
    public String absolutePath() {
        return path();
    }

    @Override
    public InputStream read() {
        return target.read();
    }

    @Override
    public ByteBuffer map(FileChannel.MapMode mode) {
        return target.map(mode);
    }

    @Override
    public OutputStream write(boolean append) {
        return target.write(append);
    }

    @Override
    public Writer writer(boolean append, String charset) {
        return target.writer(append, charset);
    }

    @Override
    public Fi[] list() {
        return target.list();
    }

    @Override
    public Fi[] list(FileFilter filter) {
        return target.list(filter);
    }

    @Override
    public Fi[] list(FilenameFilter filter) {
        return target.list(filter);
    }

    @Override
    public Fi[] list(String suffix) {
        return target.list(suffix);
    }

    @Override
    public boolean isDirectory() {
        return target.isDirectory();
    }

    @Override
    public Fi child(String name) {
        return new ForwardedFi(super.child(name).path(), provider);
    }

    @Override
    public Fi sibling(String name) {
        return new ForwardedFi(super.sibling(name).path(), provider);
    }

    @Override
    public Fi parent() {
        return new ForwardedFi(super.parent().path(), provider);
    }

    @Override
    public boolean mkdirs() {
        return target.mkdirs();
    }

    @Override
    public boolean exists() {
        return target.exists();
    }

    @Override
    public boolean delete() {
        return target.delete();
    }

    @Override
    public boolean deleteDirectory() {
        return target.deleteDirectory();
    }

    @Override
    public void emptyDirectory(boolean preserveTree) {
        target.emptyDirectory(preserveTree);
    }

    @Override
    public void moveTo(Fi dest) {
        target.moveTo(dest);
    }

    @Override
    public long length() {
        return target.length();
    }

    @Override
    public long lastModified() {
        return target.lastModified();
    }

    @Override
    public boolean equals(Object obj) {
        return target.equals(obj);
    }

    @Override
    public int hashCode() {
        return target.hashCode();
    }

    @Override
    public int compareTo(Fi fi) {
        return target.compareTo(fi);
    }
}
