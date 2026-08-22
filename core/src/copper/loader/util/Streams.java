package copper.loader.util;

import java.io.*;

/**
 * Stream helpers that do not rely on {@code InputStream.readAllBytes()},
 * which is missing on older Android API levels.
 */
public class Streams {
    /** Reads the whole stream into a byte array. */
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

    /**
     * Copies everything from one stream to another.
     *
     * @param closeInput whether to close the input stream when done
     */
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