package copper.loader.mod;

/**
 * A dependency or conflict descriptor: a mod id paired with a version filter.
 */
public class ModDescriptor {
    /** Target mod id. */
    public String id;
    /** Version filter that must be satisfied. */
    public IVersionFilter version;
}
