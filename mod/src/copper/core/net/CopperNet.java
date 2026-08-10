package copper.core.net;

import arc.func.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.net.*;

public class CopperNet {
    private static Seq<Prov<? extends Packet>> packetProvs = new Seq<>();
    private static ObjectIntMap<Class<? extends Packet>> packetToId = new ObjectIntMap<>();

    public static <T extends Packet> int registerPacket(Prov<T> cons){
        int id = packetProvs.size;
        packetProvs.add(cons);
        var t = cons.get();
        packetToId.put(t.getClass(), id);
        return id;
    }

    public static int getPacketId(Packet packet){
        int id = packetToId.get(packet.getClass(), -1);
        if(id == -1) throw new ArcRuntimeException("Unknown packet type: " + packet.getClass());
        return id;
    }

    @SuppressWarnings("unchecked")
    public static <T extends Packet> T newPacket(int id){
        return ((Prov<T>)packetProvs.get(id & 0xffff)).get();
    }

    public void send(Packet packet, boolean reliable) {
        ExtendedPacket wrap = new ExtendedPacket();
        wrap.target = packet;
        Vars.net.send(wrap, reliable);
    }
}
