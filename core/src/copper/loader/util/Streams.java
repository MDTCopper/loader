package copper.loader.util;

import java.io.*;

public class Streams {
    public static byte[] readAllBytes(InputStream in) {
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int len;
            while ((len = in.read(chunk)) != -1) {
                buffer.write(chunk, 0, len);
            }
            return buffer.toByteArray();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static void pipeStream(InputStream in, OutputStream out, boolean closeInput) {
        try {
            byte[] buffer = new byte[8192];
            int length;
            while ((length = in.read(buffer)) != -1) {
                out.write(buffer, 0, length);
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (closeInput)
                    in.close();
            } catch (Throwable ignored) {}
        }
    }
}
