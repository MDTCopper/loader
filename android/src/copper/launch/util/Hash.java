package copper.launch.util;

import java.io.*;
import java.security.*;

/** Small sha256 helper used to derive the dex cache file and folder names. */
public class Hash {
    private static final char[] hexMap = "0123456789abcdef".toCharArray();

    private static String hexToString(byte[] data) {
        StringBuilder builder = new StringBuilder(data.length * 2);
        for (byte c : data) {
            builder.append(hexMap[(c & 0xff) >> 4]);
            builder.append(hexMap[c & 0xf]);
        }
        return builder.toString();
    }

    /** Returns the sha256 hex string of the given data. */
    public static String sha256(byte[] data) {
        try{
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream fis = new ByteArrayInputStream(data); DigestInputStream dis = new DigestInputStream(fis, digest)) {
                byte[] buffer = new byte[8192];
                while (dis.read(buffer) != -1);
            }
            return hexToString(digest.digest());
        }catch(IOException | NoSuchAlgorithmException e){
            throw new RuntimeException(e);
        }
    }
}