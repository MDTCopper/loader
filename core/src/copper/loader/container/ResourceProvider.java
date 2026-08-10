package copper.loader.container;

import copper.loader.container.resource.*;
import java.util.*;

/**
 * Chains multiple {@link IResource} providers, returning data from the first one that succeeds.
 */
public class ResourceProvider {
    /** Ordered list of resource backends. */
    public ArrayList<IResource> resources;

    public ResourceProvider() {
        resources = new ArrayList<>();
    }

    /**
     * Reads a file from this provider's resource chain.
     *
     * @param path the resource path (backslashes normalized to forward slashes,
     *             leading slash stripped)
     * @return the raw bytes, or {@code null} if not found
     */
    public byte[] get(String path) {
        path = path.replace('\\', '/');
        if (path.startsWith("/"))
            path = path.substring(1);
        for (IResource resource : resources) {
            try {
                byte[] data = resource.read(path);
                if (data != null)
                    return data;
            } catch (Throwable ignored) {}
        }
        return null;
    }
}
