package net.minecraft.server;

import java.io.IOException;

public interface Packet<T extends PacketListener> {

    void a(PacketDataSerializer packetdataserializer) throws IOException;

    void b(PacketDataSerializer packetdataserializer) throws IOException;

    // Paper start
    default java.util.List<Packet> getExtraPackets() { return null; }
    default boolean packetTooLarge(NetworkManager manager) {
        return false;
    }
    // Paper end
    void a(T t0);
}
