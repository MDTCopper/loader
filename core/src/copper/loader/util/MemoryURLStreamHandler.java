package copper.loader.util;

import java.io.*;
import java.net.*;

/**
 * URL stream handler backed by an in-memory byte array.
 *
 * <p>Used to serve resources from containers as {@code copper-memory:} URLs.</p>
 */
public class MemoryURLStreamHandler extends URLStreamHandler {
    private final byte[] data;

    /**
     * @param data the byte content to serve
     */
    public MemoryURLStreamHandler(byte[] data) {
        this.data = data;
    }

    @Override
    protected URLConnection openConnection(URL u) throws IOException {
        return new MemoryURLConnection(u, data);
    }

    /** A {@link URLConnection} that reads from an in-memory byte array. */
    public static class MemoryURLConnection extends URLConnection {
        private final byte[] data;

        protected MemoryURLConnection(URL url, byte[] data) {
            super(url);
            this.data = data;
        }

        @Override
        public void connect() {
            connected = true;
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(data);
        }
    }
}
