package copper.core.net;

import arc.util.io.*;
import mindustry.net.*;

import java.io.*;

public class ExtendedPacket extends Packet {
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
