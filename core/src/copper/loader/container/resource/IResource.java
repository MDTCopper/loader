package copper.loader.container.resource;

/**
 * A generic resource reader that returns raw bytes for a given path.
 */
public interface IResource {
    /**
     * @param path the resource path
     * @return the raw bytes, or {@code null} if not found
     */
    byte[] read(String path);
}
