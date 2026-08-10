package copper.core.net;

import arc.func.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.net.*;

/**
 * Manages Copper-specific network packet types.
 *
 * <p>Mindustry's {@link Net} has a hard limit of <b>256 packet types</b>
 * (the packet id is serialized as a single {@code byte} on the wire).
 * Copper expands this capacity to <b>65536</b> (unsigned short) by introducing
 * an {@link ExtendedPacket} wrapper: Copper mods register their custom packets
 * here with {@code int} ids, and the wrapper encodes them as a 2-byte
 * sub-id inside the outer packet.</p>
 *
 * <p>Each Copper mod can register its own packet types by overriding
 * {@link copper.core.mod.CopperMod#registerPackets()}.</p>
 */
public class CopperNet {
    private static Seq<Prov<? extends Packet>> packetProvs = new Seq<>();
    private static ObjectIntMap<Class<? extends Packet>> packetToId = new ObjectIntMap<>();

    /**
     * Registers a packet type and returns its assigned id.
     */
    public static <T extends Packet> int registerPacket(Prov<T> cons){
        int id = packetProvs.size;
        packetProvs.add(cons);
        var t = cons.get();
        packetToId.put(t.getClass(), id);
        return id;
    }

    /**
     * Returns the id for a registered packet type.
     *
     * @throws ArcRuntimeException if the packet type is unknown
     */
    public static int getPacketId(Packet packet){
        int id = packetToId.get(packet.getClass(), -1);
        if(id == -1) throw new ArcRuntimeException("Unknown packet type: " + packet.getClass());
        return id;
    }

    /** Creates a new packet instance by id. */
    @SuppressWarnings("unchecked")
    public static <T extends Packet> T newPacket(int id){
        return ((Prov<T>)packetProvs.get(id & 0xffff)).get();
    }

    /**
     * Sends a custom packet wrapped in an {@link ExtendedPacket}.
     */
    public void send(Packet packet, boolean reliable) {
        ExtendedPacket wrap = new ExtendedPacket();
        wrap.target = packet;
        Vars.net.send(wrap, reliable);
    }
}
