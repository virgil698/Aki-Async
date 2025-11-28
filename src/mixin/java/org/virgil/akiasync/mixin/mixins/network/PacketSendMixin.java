package org.virgil.akiasync.mixin.mixins.network;

import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.virgil.akiasync.mixin.bridge.Bridge;
import org.virgil.akiasync.mixin.bridge.BridgeManager;
import org.virgil.akiasync.mixin.util.PacketTypeHelper;

@Mixin(value = Connection.class, priority = 900)
public abstract class PacketSendMixin {

    private static final ThreadLocal<Boolean> SENDING_FROM_WORKER = ThreadLocal.withInitial(() -> false);

    @Shadow
    public abstract net.minecraft.server.level.ServerPlayer getPlayer();

    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void aki$onPacketSend(Packet<?> packet, CallbackInfo ci) {
        try {

            if (SENDING_FROM_WORKER.get()) {
                return;
            }

            Bridge bridge = BridgeManager.getBridge();
            if (bridge == null || !bridge.isNetworkOptimizationEnabled()) {
                return;
            }

            ServerPlayer player = this.getPlayer();
            if (player == null) {
                return;
            }

            if (bridge.isTeleportOptimizationEnabled() && bridge.isTeleportPacketBypassEnabled()) {
                if (bridge.isTeleportPacket(packet)) {
                    
                    bridge.markPlayerTeleportStart(player.getUUID());
                    
                    bridge.recordTeleportBypassedPacket();
                    
                    if (bridge.isTeleportDebugEnabled()) {
                        bridge.debugLog("[TeleportOptimization] Teleport packet detected for player %s: %s",
                            player.getUUID().toString(),
                            packet.getClass().getSimpleName()
                        );
                    }
                    
                    return;
                }
                
                if (bridge.isPlayerTeleporting(player.getUUID())) {
                    
                    if (!bridge.shouldSendPacketDuringTeleport(packet, player.getUUID())) {
                        
                        if (bridge.isTeleportDebugEnabled()) {
                            bridge.debugLog("[TeleportOptimization] Filtered non-essential packet during teleport for player %s: %s",
                                player.getUUID().toString(),
                                packet.getClass().getSimpleName()
                            );
                        }
                        ci.cancel();
                        return;
                    }
                    
                    if (bridge.isTeleportDebugEnabled()) {
                        bridge.debugLog("[TeleportOptimization] Sending essential packet during teleport for player %s: %s",
                            player.getUUID().toString(),
                            packet.getClass().getSimpleName()
                        );
                    }
                    return;
                }
            }

            if (!bridge.isPacketPriorityEnabled()) {
                return;
            }

            if (bridge.isPlayerUsingViaVersion(player.getUUID())) {
                
                if (PacketTypeHelper.isPacketType(packet, ClientboundLevelChunkWithLightPacket.class)) {
                    return;
                }
                
                if (PacketTypeHelper.isPacketType(packet, net.minecraft.network.protocol.game.ClientboundSetHealthPacket.class) ||
                    PacketTypeHelper.isPacketType(packet, net.minecraft.network.protocol.game.ClientboundSetExperiencePacket.class) ||
                    PacketTypeHelper.isPacketType(packet, net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket.class) ||
                    PacketTypeHelper.isPacketType(packet, net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket.class) ||
                    PacketTypeHelper.isPacketType(packet, net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket.class) ||
                    PacketTypeHelper.isPacketType(packet, net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket.class)) {
                    return;
                }
                
                if (!bridge.isViaConnectionInPlayState(player.getUUID())) {
                    return;
                }
            }

            if (!bridge.shouldPacketUseQueue(packet)) {

                return;
            }

            int priority = bridge.classifyPacketPriority(packet);

            boolean enqueued = bridge.enqueuePacket(player, packet, priority);

            if (enqueued) {
                ci.cancel();
            }

            if (bridge.isDebugLoggingEnabled()) {
                int queueSize = bridge.getPlayerPacketQueueSize(player.getUUID());

                if (!enqueued) {
                    String priorityName = switch (priority) {
                        case 0 -> "HIGH";
                        case 2 -> "LOW";
                        default -> "NORMAL";
                    };

                    bridge.debugLog("[PacketSend] DROPPED packet for %s: queue full, priority: %s, packet: %s",
                        player.getUUID().toString(),
                        priorityName,
                        packet.getClass().getSimpleName()
                    );
                }

                if (queueSize > 100) {
                    String priorityName = switch (priority) {
                        case 0 -> "HIGH";
                        case 2 -> "LOW";
                        default -> "NORMAL";
                    };

                    bridge.debugLog("[PacketSend] Player %s queue size: %d, priority: %s, packet: %s",
                        player.getUUID().toString(),
                        queueSize,
                        priorityName,
                        packet.getClass().getSimpleName()
                    );
                }
            }

            if (bridge.isCongestionDetectionEnabled()) {

                int estimatedSize = 100;
                bridge.recordPacketSent(player.getUUID(), estimatedSize);
            }

        } catch (Exception e) {

            Bridge bridge = BridgeManager.getBridge();
            if (bridge != null && bridge.isDebugLoggingEnabled()) {
                bridge.errorLog("[PacketSend] Error in packet priority scheduling: %s", e.getMessage());
            }
        }
    }
}
