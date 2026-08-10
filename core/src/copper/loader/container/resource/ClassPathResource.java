package copper.loader.container.resource;

public class ClassPathResource implements IResource {
    public ClassLoader loader;

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
