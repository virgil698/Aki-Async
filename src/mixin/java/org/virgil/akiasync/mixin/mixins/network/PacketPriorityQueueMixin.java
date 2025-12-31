package org.virgil.akiasync.mixin.mixins.network;

import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.atomic.AtomicLong;


@SuppressWarnings("unused")
@Mixin(value = Connection.class, priority = 1200) 
public class PacketPriorityQueueMixin {
    
    @Unique
    private static volatile boolean enabled = true;
    @Unique
    private static volatile boolean initialized = false;
    
    @Unique
    private static volatile boolean prioritizePlayerPackets = true;
    @Unique
    private static volatile boolean prioritizeChunkPackets = true;
    @Unique
    private static volatile boolean deprioritizeParticles = true;
    @Unique
    private static volatile boolean deprioritizeSounds = true;

    @Unique
    private static final AtomicLong highPriorityPackets = new AtomicLong(0);
    @Unique
    private static final AtomicLong normalPriorityPackets = new AtomicLong(0);
    @Unique
    private static final AtomicLong lowPriorityPackets = new AtomicLong(0);
    
    
    @Inject(
        method = "send(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketSendListener;Z)V",
        at = @At("HEAD"),
        require = 0
    )
    private void classifyPacketPriority(Packet<?> packet, Object listener, boolean flush, CallbackInfo ci) {
        if (!initialized) {
            akiasync$init();
        }
        
        if (!enabled) {
            return;
        }
        
        try {
            int priority = akiasync$getPacketPriority(packet);
            
            switch (priority) {
                case 2 -> highPriorityPackets.incrementAndGet();
                case 1 -> normalPriorityPackets.incrementAndGet();
                default -> lowPriorityPackets.incrementAndGet();
            }
            
        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "PacketPriorityQueue", "send", e);
        }
    }
    
    @Unique
    private int akiasync$getPacketPriority(Packet<?> packet) {
        
        if (prioritizePlayerPackets) {
            if (packet instanceof ClientboundPlayerPositionPacket ||
                packet instanceof ClientboundSetHealthPacket ||
                packet instanceof ClientboundRespawnPacket ||
                packet instanceof ClientboundLoginPacket ||
                packet instanceof ClientboundPlayerCombatKillPacket) {
                return 2;
            }
        }
        
        
        if (prioritizeChunkPackets) {
            if (packet instanceof ClientboundLevelChunkWithLightPacket ||
                packet instanceof ClientboundForgetLevelChunkPacket ||
                packet instanceof ClientboundChunkBatchStartPacket ||
                packet instanceof ClientboundChunkBatchFinishedPacket) {
                return 2;
            }
        }
        
        
        if (deprioritizeParticles && packet instanceof ClientboundLevelParticlesPacket) {
            return 0;
        }
        
        
        if (deprioritizeSounds) {
            if (packet instanceof ClientboundSoundPacket ||
                packet instanceof ClientboundSoundEntityPacket) {
                return 0;
            }
        }
        
        
        if (packet instanceof ClientboundMoveEntityPacket ||
            packet instanceof ClientboundRotateHeadPacket ||
            packet instanceof ClientboundTeleportEntityPacket) {
            return 1;
        }
        
        return 1;
    }
    
    @Unique
    private static void akiasync$init() {
        if (initialized) {
            return;
        }
        
        try {
            org.virgil.akiasync.mixin.bridge.Bridge bridge =
                org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
            
            if (bridge != null) {
                enabled = bridge.isPacketPriorityQueueEnabled();
                prioritizePlayerPackets = bridge.isPrioritizePlayerPacketsEnabled();
                prioritizeChunkPackets = bridge.isPrioritizeChunkPacketsEnabled();
                deprioritizeParticles = bridge.isDeprioritizeParticlesEnabled();
                deprioritizeSounds = bridge.isDeprioritizeSoundsEnabled();
                
                bridge.debugLog("[PacketPriorityQueue] Initialized: enabled=%s", enabled);
            }
        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "PacketPriorityQueue", "init", e);
        }
        
        initialized = true;
    }
    
    @Unique
    private static String akiasync$getStatistics() {
        long high = highPriorityPackets.get();
        long normal = normalPriorityPackets.get();
        long low = lowPriorityPackets.get();
        long total = high + normal + low;
        
        if (total == 0) {
            return "PacketPriorityQueue: No packets processed";
        }
        
        return String.format(
            "PacketPriorityQueue: high=%d (%.1f%%), normal=%d (%.1f%%), low=%d (%.1f%%)",
            high, (double) high / total * 100,
            normal, (double) normal / total * 100,
            low, (double) low / total * 100
        );
    }
}
