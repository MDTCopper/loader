package copper.loader.container;

import copper.loader.container.resource.*;
import java.util.*;

public class ResourceProvider {
    public ArrayList<IResource> resources;

    public ResourceProvider() {
        resources = new ArrayList<>();
    }

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
