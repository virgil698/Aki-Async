package org.virgil.akiasync.mixin.mixins.network.connection;

import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.virgil.akiasync.mixin.network.PacketStatistics;

@SuppressWarnings("unused")
@Mixin(Connection.class)
public class PacketStatisticsMixin {

    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"), require = 0)
    private void trackOutgoingPacket(Packet<?> packet, CallbackInfo ci) {
        if (PacketStatistics.isEnabled() && packet != null) {
            String packetName = getSimplePacketName(packet);
            int estimatedSize = estimatePacketSize(packet);
            PacketStatistics.recordOutgoing(packetName, estimatedSize);
        }
    }

    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketSendListener;Z)V", at = @At("HEAD"), require = 0)
    private void trackOutgoingPacketWithListener(Packet<?> packet, Object listener, boolean flush, CallbackInfo ci) {
        if (PacketStatistics.isEnabled() && packet != null) {
            String packetName = getSimplePacketName(packet);
            int estimatedSize = estimatePacketSize(packet);
            PacketStatistics.recordOutgoing(packetName, estimatedSize);
        }
    }

    @Inject(method = "genericsFtw", at = @At("HEAD"), require = 0)
    private static void trackIncomingPacket(Packet<?> packet, PacketListener listener, CallbackInfo ci) {
        if (PacketStatistics.isEnabled() && packet != null) {
            String packetName = getSimplePacketName(packet);
            int estimatedSize = estimatePacketSize(packet);
            PacketStatistics.recordIncoming(packetName, estimatedSize);
        }
    }

    private static String getSimplePacketName(Packet<?> packet) {
        String fullName = packet.getClass().getName();
        int lastDot = fullName.lastIndexOf('.');
        if (lastDot >= 0) {
            return fullName.substring(lastDot + 1);
        }
        return fullName;
    }

    private static int estimatePacketSize(Packet<?> packet) {

        return 64;
    }
}
