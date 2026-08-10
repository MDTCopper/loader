package copper.core.net;

import arc.util.io.*;
import mindustry.net.*;

import java.io.*;

/**
 * A network packet wrapper that delegates to a Copper-registered custom packet.
 *
 * <p>Mindustry's built-in packet ids are limited to a single {@code byte}
 * (0–255). This wrapper encodes a Copper sub-packet id as an <b>unsigned short</b>
 * (2 bytes, 0–65535), expanding the total addressable packet space for Copper mods.</p>
 *
 * <p>On the wire, an {@code ExtendedPacket} is a standard Mindustry packet
 * whose payload starts with a 2-byte Copper sub-id followed by the
 * wrapped packet's own serialized data.</p>
 */
public class ExtendedPacket extends Packet {
    /** The wrapped Copper packet. */
    public Packet target;

    @Override
    public void read(Reads read, int length) {
        if (length < 2)
            return;
        int id = read.us();
        target = CopperNet.newPacket(id);
        target.read(read, length - 2);
    }

    @Override
    public void write(Writes write) {
        if (target != null) {
            write.s(CopperNet.getPacketId(target) & 0xffff);
            target.write(write);
        }
    }

    @Override
    public void handled() {
        if (target != null)
            target.handled();
    }

    @Override
    public void handleClient() {
        if (target != null)
            target.handleClient();
    }

    @Override
    public void handleServer(NetConnection con) {
        if (target != null)
            target.handleServer(con);
    }

    @Override
    public boolean allow(boolean server) {
        if (target == null)
            return false;
        return target.allow(server);
    }

    @Override
    public int getPriority() {
        if (target == null)
            return priorityNormal;
        return target.getPriority();
    }
}
