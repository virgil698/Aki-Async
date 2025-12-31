package org.virgil.akiasync.mixin.mixins.network;

import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;


@SuppressWarnings("unused")
@Mixin(value = ServerEntity.class, priority = 1100) 
public class EntityDataPacketThrottleMixin {
    
    @Shadow @Final private Entity entity;
    
    @Unique
    private static volatile boolean enabled = true;
    @Unique
    private static volatile boolean initialized = false;

    @Unique
    private static volatile int throttleIntervalTicks = 3;
    @Unique
    private static volatile int maxDataPacketsPerSecond = 20;
    @Unique
    private static volatile boolean throttleNonCritical = true;
    
    @Unique
    private static final Map<Integer, Long> lastDataSendTime = new ConcurrentHashMap<>();
    @Unique
    private static final Map<Integer, AtomicLong> dataPacketCount = new ConcurrentHashMap<>();
    @Unique
    private static volatile long lastSecondReset = System.currentTimeMillis();
    
    @Unique
    private static final AtomicLong totalDataPackets = new AtomicLong(0);
    @Unique
    private static final AtomicLong throttledDataPackets = new AtomicLong(0);
    
    @Unique
    private int akiasync$ticksSinceLastDataSend = 0;
    
    
    @Inject(method = "sendDirtyEntityData", at = @At("HEAD"), cancellable = true, require = 0)
    private void throttleEntityData(CallbackInfo ci) {
        if (!initialized) {
            akiasync$init();
        }
        
        if (!enabled) {
            return;
        }
        
        try {
            
            
            if (!entity.getEntityData().isDirty()) {
                return;
            }
            
            totalDataPackets.incrementAndGet();
            
            int entityId = entity.getId();
            long now = System.currentTimeMillis();
            
            
            if (now - lastSecondReset >= 1000) {
                lastSecondReset = now;
                dataPacketCount.values().forEach(c -> c.set(0));
            }
            
            
            AtomicLong count = dataPacketCount.computeIfAbsent(entityId, k -> new AtomicLong(0));
            if (count.incrementAndGet() > maxDataPacketsPerSecond) {
                throttledDataPackets.incrementAndGet();
                ci.cancel();
                return;
            }
            
            
            Long lastSend = lastDataSendTime.get(entityId);
            if (lastSend != null) {
                long elapsed = now - lastSend;
                
                if (elapsed < ((long) throttleIntervalTicks * 50L)) {
                    
                    if (throttleNonCritical && !akiasync$isCriticalData()) {
                        throttledDataPackets.incrementAndGet();
                        ci.cancel();
                        return;
                    }
                }
            }
            
            lastDataSendTime.put(entityId, now);
            akiasync$ticksSinceLastDataSend = 0;
            
        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "EntityDataPacketThrottle", "sendDirtyEntityData", e);
        }
    }
    
    @Unique
    private boolean akiasync$isCriticalData() {
        
        if (entity instanceof net.minecraft.world.entity.player.Player) {
            return true;
        }
        
        
        if (entity instanceof net.minecraft.world.entity.LivingEntity living) {
            if (living.hurtTime > 0 || living.deathTime > 0) {
                return true;
            }
        }
        
        return false;
    }
    
    @Unique
    private static void akiasync$onEntityRemoved(int entityId) {
        lastDataSendTime.remove(entityId);
        dataPacketCount.remove(entityId);
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
                enabled = bridge.isEntityDataPacketThrottleEnabled();
                throttleIntervalTicks = bridge.getEntityDataThrottleInterval();
                maxDataPacketsPerSecond = bridge.getMaxEntityDataPacketsPerSecond();
                
                bridge.debugLog("[EntityDataPacketThrottle] Initialized: enabled=%s, interval=%d, max=%d/s",
                    enabled, throttleIntervalTicks, maxDataPacketsPerSecond);
            }
        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "EntityDataPacketThrottle", "init", e);
        }
        
        initialized = true;
    }
}
