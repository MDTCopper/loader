package copper.loader.container;

import copper.loader.container.resource.*;
import java.io.*;
import java.util.zip.*;

/**
 * A {@link DelegatedContainer} that also exposes the loader jar as a resource,
 * so bytecode (and embedded files) of the loader itself can be read on the ART side.
 */
public class ArtDelegatedContainer extends DelegatedContainer {
    public ArtDelegatedContainer(ClassLoader target, File resource) {
        super(target);
        try {
            IResource source = resource.isDirectory() ?
                    new FolderResource(resource) : new ZipResource(new ZipFile(resource));
            this.resource.resources.add(source);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}