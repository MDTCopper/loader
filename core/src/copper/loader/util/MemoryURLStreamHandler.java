package copper.loader.util;

import java.io.*;
import java.net.*;

public class MemoryURLStreamHandler extends URLStreamHandler {
    private final byte[] data;

    public MemoryURLStreamHandler(byte[] data) {
        this.data = data;
    }

    @Override
    protected URLConnection openConnection(URL u) throws IOException {
        return new MemoryURLConnection(u, data);
    }

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
