package copper.loader.container.resource;

public class MountedResouce implements IResource {
    public IResource target;
    public String path;

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
