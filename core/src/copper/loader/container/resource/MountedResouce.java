package copper.loader.container.resource;

/**
 * Wraps another {@link IResource} and prefixes every read path with a mount point.
 *
 * <p>For example, a {@code MountedResouce(resource, "assets")} turns the read of
 * {@code "sprites/logo.png"} into {@code "assets/sprites/logo.png"}.</p>
 */
public class MountedResouce implements IResource {
    /** The wrapped resource to read from. */
    public IResource target;
    /** The path prefix (always ends with {@code "/"}). */
    public String path;

    /**
     * @param target     the wrapped resource
     * @param targetPath the mount point prefix
     */
    public MountedResouce(IResource target, String targetPath) {
        if (!targetPath.endsWith("/"))
            targetPath += "/";
        this.target = target;
        this.path = targetPath;
    }

    @Override
    public byte[] read(String path) {
        return target.read(this.path + path);
    }
}