package org.virgil.akiasync.mixin.mixins.network;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

@SuppressWarnings("unused")
@Mixin(ServerCommonPacketListenerImpl.class)
public class BatchNetworkPacketMixin {
    
    @Unique
    private static volatile boolean enabled = true;
    @Unique
    private static volatile boolean initialized = false;
    
    @Unique
    private static volatile int batchSize = 20;
    
    @Unique
    private final ConcurrentLinkedQueue<Packet<?>> packetQueue = new ConcurrentLinkedQueue<>();
    
    @Unique
    private static volatile long totalPackets = 0;
    @Unique
    private static volatile long batchedPackets = 0;
    @Unique
    private static volatile long flushes = 0;
    
    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void batchPackets(Packet<?> packet, CallbackInfo ci) {
        if (!initialized) {
            akiasync$initBatchOptimization();
        }
        
        if (!enabled) {
            return;
        }
        
        totalPackets++;
        
        try {
            if (akiasync$shouldSendImmediately(packet)) {
                return;
            }
            
            if (!((Object)this instanceof ServerGamePacketListenerImpl)) {
                return;
            }
            
            packetQueue.offer(packet);
            batchedPackets++;
            
            if (packetQueue.size() >= batchSize) {
                akiasync$flushPacketQueue();
            }
            ci.cancel();
            
        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "BatchNetworkPacket", "batchPackets", e);
        }
    }
    
    @Unique
    private void akiasync$flushPacketQueue() {
        if (packetQueue.isEmpty()) {
            return;
        }
        
        flushes++;
        
        List<Packet<?>> packetsToSend = new ArrayList<>();
        Packet<?> packet;
        
        while ((packet = packetQueue.poll()) != null && packetsToSend.size() < batchSize) {
            packetsToSend.add(packet);
        }
        
        for (Packet<?> p : packetsToSend) {
            try {
                boolean wasEnabled = enabled;
                enabled = false;
                ((ServerCommonPacketListenerImpl)(Object)this).send(p);
                enabled = wasEnabled;
            } catch (Exception e) {
                org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                    "BatchNetworkPacket", "flushPacketQueue - sending packet", e);
            }
        }
    }
    
    @Unique
    private boolean akiasync$shouldSendImmediately(Packet<?> packet) {
        if (packet instanceof ClientboundPlayerChatPacket ||
            packet instanceof ClientboundSystemChatPacket ||
            packet instanceof ClientboundLoginPacket ||
            packet instanceof ClientboundPlayerAbilitiesPacket ||
            packet instanceof ClientboundRespawnPacket ||
            packet instanceof ClientboundSetHealthPacket ||
            packet instanceof ClientboundPlayerCombatKillPacket) {
            return true;
        }
        
        String simpleName = packet.getClass().getSimpleName();
        return simpleName.contains("KeepAlive") ||
               simpleName.contains("Configuration") ||
               simpleName.contains("Handshake") ||
               simpleName.contains("Status");
    }
    
    @Unique
    private static void akiasync$initBatchOptimization() {
        if (initialized) {
            return;
        }
        
        try {
            org.virgil.akiasync.mixin.bridge.Bridge bridge =
                org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
            
            if (bridge != null) {
                enabled = bridge.isVelocityCompressionEnabled();
                
                bridge.debugLog("[BatchNetworkPacket] Initialized: enabled=%s, batchSize=%d", enabled, batchSize);
            
                initialized = true;
            }
        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "BatchNetworkPacket", "init", e);
        }
    }
}
