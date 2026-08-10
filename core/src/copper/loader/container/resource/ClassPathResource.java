package copper.loader.container.resource;

/**
 * Reads resources from a {@link ClassLoader}'s classpath.
 */
public class ClassPathResource implements IResource {
    public ClassLoader loader;

    /**
     * @param target the classloader to read from
     */
    public ClassPathResource(ClassLoader target) {
        loader = target;
    }

    @Override
    public byte[] read(String path) {
        try (var stream = loader.getResourceAsStream(path)) {
            return stream == null ? null : stream.readAllBytes();
        } catch (Throwable e) {
            return null;
        }
    }
}
