package copper.launch.builder;

/**
 * An id/version pair identifying a mod in the dex cache metadata.
 */
public class ModDescriptor {
    public String id;
    public String version;

    public ModDescriptor(String id, String version) {
        this.id = id;
        this.version = version;
    }
}
