package org.virgil.akiasync.mixin.mixins.network;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


@SuppressWarnings("unused")
@Mixin(ServerCommonPacketListenerImpl.class)
public class AfkPlayerPacketThrottleMixin {
    
    @Unique
    private static volatile boolean enabled = true;
    @Unique
    private static volatile boolean initialized = false;
    
    @Unique
    private static volatile long afkDurationMs = 120000;
    @Unique
    private static volatile double particleMaxDistance = 0.0;
    @Unique
    private static volatile double soundMaxDistance = 64.0;
    @Unique
    private static volatile double entitySoundMaxDistance = 100.0;
    @Unique
    private static volatile boolean filterTimeUpdate = true;
    @Unique
    private static volatile boolean filterPlayerInfoLatency = false;
    
    @Unique
    private static final Map<UUID, Long> lastMoveTime = new ConcurrentHashMap<>();
    @Unique
    private static final Map<UUID, Vec3> lastPosition = new ConcurrentHashMap<>();
    @Unique
    private static final Set<UUID> afkPlayers = ConcurrentHashMap.newKeySet();
    
    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void throttleAfkPackets(Packet<?> packet, CallbackInfo ci) {
        if (!initialized) {
            akiasync$init();
        }
        
        if (!enabled) {
            return;
        }
        
        if (!((Object)this instanceof ServerGamePacketListenerImpl gameListener)) {
            return;
        }
        
        try {
            ServerPlayer player = gameListener.player;
            if (player == null) {
                return;
            }
            
            UUID playerId = player.getUUID();
            
            akiasync$updateAfkStatus(player);
            
            if (!afkPlayers.contains(playerId)) {
                return;
            }
            
            if (akiasync$shouldFilterPacket(packet, player)) {
                ci.cancel();
            }
            
        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "AfkPlayerPacketThrottle", "throttleAfkPackets", e);
        }
    }
    
    @Unique
    private void akiasync$updateAfkStatus(ServerPlayer player) {
        UUID playerId = player.getUUID();
        Vec3 currentPos = player.position();
        long now = System.currentTimeMillis();
        
        Vec3 lastPos = lastPosition.get(playerId);
        if (lastPos == null || currentPos.distanceToSqr(lastPos) > 0.01) {
            lastPosition.put(playerId, currentPos);
            lastMoveTime.put(playerId, now);
            afkPlayers.remove(playerId);
            return;
        }
        
        Long lastMove = lastMoveTime.get(playerId);
        if (lastMove == null) {
            lastMoveTime.put(playerId, now);
            return;
        }
        
        if ((now - lastMove) >= afkDurationMs) {
            afkPlayers.add(playerId);
        }
    }
    
    @Unique
    private boolean akiasync$shouldFilterPacket(Packet<?> packet, ServerPlayer player) {
        Vec3 playerPos = player.position();
        
        if (packet instanceof ClientboundLevelParticlesPacket particlePacket) {
            if (particleMaxDistance <= 0) {
                return true;
            }
            double distSqr = akiasync$distanceSquared(playerPos, particlePacket.getX(), particlePacket.getY(), particlePacket.getZ());
            return distSqr > (particleMaxDistance * particleMaxDistance);
        }
        
        if (packet instanceof ClientboundSoundPacket soundPacket) {
            if (soundMaxDistance <= 0) {
                return true;
            }
            double distSqr = akiasync$distanceSquared(playerPos, soundPacket.getX(), soundPacket.getY(), soundPacket.getZ());
            return distSqr > (soundMaxDistance * soundMaxDistance);
        }
        
        if (packet instanceof ClientboundSoundEntityPacket) {
            if (entitySoundMaxDistance <= 0) {
                return true;
            }
            return false;
        }
        
        if (packet instanceof ClientboundHurtAnimationPacket) {
            return true;
        }
        
        if (packet instanceof ClientboundDamageEventPacket) {
            return true;
        }
        
        if (packet instanceof ClientboundRotateHeadPacket) {
            return true;
        }
        
        if (packet instanceof ClientboundSetTimePacket && !filterTimeUpdate) {
            return true;
        }
        
        return false;
    }
    
    @Unique
    private double akiasync$distanceSquared(Vec3 pos, double x, double y, double z) {
        double dx = pos.x - x;
        double dy = pos.y - y;
        double dz = pos.z - z;
        return dx * dx + dy * dy + dz * dz;
    }
    
    @Unique
    private static void akiasync$onPlayerQuit(UUID playerId) {
        lastMoveTime.remove(playerId);
        lastPosition.remove(playerId);
        afkPlayers.remove(playerId);
    }
    
    @Unique
    private static void akiasync$onPlayerMove(UUID playerId) {
        lastMoveTime.put(playerId, System.currentTimeMillis());
        afkPlayers.remove(playerId);
    }
    
    @Unique
    private static boolean akiasync$isPlayerAfk(UUID playerId) {
        return afkPlayers.contains(playerId);
    }
    
    @Unique
    private static int akiasync$getAfkPlayerCount() {
        return afkPlayers.size();
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
                enabled = bridge.isAfkPacketThrottleEnabled();
                afkDurationMs = bridge.getAfkDurationMs();
                particleMaxDistance = bridge.getAfkParticleMaxDistance();
                soundMaxDistance = bridge.getAfkSoundMaxDistance();
                
                bridge.debugLog("[AfkPlayerPacketThrottle] Initialized: enabled=%s, afkDuration=%dms",
                    enabled, afkDurationMs);
            }
        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "AfkPlayerPacketThrottle", "init", e);
        }
        
        initialized = true;
    }
}
