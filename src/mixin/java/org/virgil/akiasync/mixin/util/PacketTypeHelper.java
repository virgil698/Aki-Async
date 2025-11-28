package org.virgil.akiasync.mixin.util;

import net.minecraft.network.protocol.Packet;

public final class PacketTypeHelper {

    private PacketTypeHelper() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static boolean isPacketType(Packet<?> packet, Class<? extends Packet<?>> packetClass) {
        if (packet == null || packetClass == null) {
            return false;
        }
        
        return packetClass.isInstance(packet);
    }
}
