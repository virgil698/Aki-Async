package org.virgil.akiasync.mixin.mixins.gameevent;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.gameevent.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.virgil.akiasync.mixin.bridge.Bridge;
import org.virgil.akiasync.mixin.bridge.BridgeManager;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(ServerLevel.class)
public class GameEventOptimizationMixin {
    
    @Unique
    private static volatile boolean initialized = false;
    @Unique
    private static volatile boolean enabled = true;
    @Unique
    private static volatile boolean earlyFilter = true;
    @Unique
    private static volatile boolean throttleLowPriority = true;
    @Unique
    private static volatile long throttleIntervalMs = 50;
    @Unique
    private static volatile boolean distanceFilter = true;
    @Unique
    private static volatile double maxDetectionDistance = 64.0;
    
    @Unique
    private static final Set<String> LOW_PRIORITY_EVENTS = ConcurrentHashMap.newKeySet();
    
    @Unique
    private static final ConcurrentHashMap<Long, Long> EVENT_THROTTLE_CACHE = new ConcurrentHashMap<>();
    
    @Unique
    private static final long THROTTLE_INTERVAL_MS = 50;
    
    @Unique
    private static final double MAX_DETECTION_DISTANCE = 64.0;
    
    @Inject(
        method = "gameEvent(Lnet/minecraft/core/Holder;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/level/gameevent/GameEvent$Context;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void aki$optimizeGameEvent(
        Holder<GameEvent> eventHolder,
        net.minecraft.world.phys.Vec3 pos,
        GameEvent.Context context,
        CallbackInfo ci
    ) {
        if (!initialized) {
            aki$initConfig();
        }
        
        if (!enabled || !earlyFilter) {
            return;
        }
        
        try {
            ServerLevel level = (ServerLevel) (Object) this;
            
            if (throttleLowPriority && aki$isLowPriorityEvent(eventHolder)) {
                
                if (aki$shouldThrottleEvent(pos)) {
                    ci.cancel();
                    return;
                }
            }
            
            if (distanceFilter && aki$isTooFarFromPlayers(level, pos)) {
                ci.cancel();
                return;
            }
            
            GameEvent event = eventHolder.value();
            if (event.notificationRadius() <= 8) {
                
                int chunkX = Mth.floor(pos.x) >> 4;
                int chunkZ = Mth.floor(pos.z) >> 4;
                if (level.getChunkIfLoadedImmediately(chunkX, chunkZ) == null) {
                    ci.cancel();
                    return;
                }
            }
            
        } catch (Exception e) {
            
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "GameEventOptimizationMixin", "optimizeGameEvent", e);
        }
    }
    
    @Unique
    private boolean aki$isLowPriorityEvent(Holder<GameEvent> eventHolder) {
        try {
            GameEvent event = eventHolder.value();
            String eventKey = event.toString();
            return LOW_PRIORITY_EVENTS.contains(eventKey);
        } catch (Exception e) {
            return false;
        }
    }
    
    @Unique
    private static boolean aki$shouldThrottleEvent(net.minecraft.world.phys.Vec3 pos) {
        long now = System.currentTimeMillis();
        
        long gridKey = aki$getGridKey(pos);
        
        Long lastTime = EVENT_THROTTLE_CACHE.get(gridKey);
        if (lastTime != null && (now - lastTime) < throttleIntervalMs) {
            return true; 
        }
        
        EVENT_THROTTLE_CACHE.put(gridKey, now);
        
        if (EVENT_THROTTLE_CACHE.size() > 1000) {
            aki$cleanupThrottleCache(now);
        }
        
        return false;
    }
    
    @Unique
    private static long aki$getGridKey(net.minecraft.world.phys.Vec3 pos) {
        int gridX = ((int) pos.x) >> 1; 
        int gridY = ((int) pos.y) >> 1;
        int gridZ = ((int) pos.z) >> 1;
        
        return ((long) gridX << 42) | ((long) gridY << 21) | (long) gridZ;
    }
    
    @Unique
    private static void aki$cleanupThrottleCache(long now) {
        EVENT_THROTTLE_CACHE.entrySet().removeIf(entry -> 
            (now - entry.getValue()) > throttleIntervalMs * 10
        );
    }
    
    @Unique
    private static boolean aki$isTooFarFromPlayers(ServerLevel level, net.minecraft.world.phys.Vec3 pos) {
        try {
            
            if (level.players().isEmpty()) {
                return true;
            }
            
            double maxDistSq = maxDetectionDistance * maxDetectionDistance;
            
            for (net.minecraft.server.level.ServerPlayer player : level.players()) {
                double distSq = player.distanceToSqr(pos);
                if (distSq <= maxDistSq) {
                    return false; 
                }
            }
            
            return true; 
        } catch (Exception e) {
            return false; 
        }
    }
    
    @Unique
    private static synchronized void aki$initConfig() {
        if (initialized) {
            return;
        }
        
        try {
            
            LOW_PRIORITY_EVENTS.add("minecraft:step");           
            LOW_PRIORITY_EVENTS.add("minecraft:swim");           
            LOW_PRIORITY_EVENTS.add("minecraft:flap");           
            LOW_PRIORITY_EVENTS.add("minecraft:splash");         
            LOW_PRIORITY_EVENTS.add("minecraft:entity_place");   
            LOW_PRIORITY_EVENTS.add("minecraft:hit_ground");     
            
            Bridge bridge = BridgeManager.getBridge();
            if (bridge != null) {
                enabled = bridge.isGameEventOptimizationEnabled();
                earlyFilter = bridge.isGameEventEarlyFilter();
                throttleLowPriority = bridge.isGameEventThrottleLowPriority();
                throttleIntervalMs = bridge.getGameEventThrottleIntervalMs();
                distanceFilter = bridge.isGameEventDistanceFilter();
                maxDetectionDistance = bridge.getGameEventMaxDetectionDistance();
                
                bridge.debugLog("[GameEventOptimization] Initialized: enabled=%s, earlyFilter=%s, " +
                    "throttleLowPriority=%s, throttleInterval=%dms, distanceFilter=%s, maxDistance=%.1f",
                    enabled, earlyFilter, throttleLowPriority, throttleIntervalMs, 
                    distanceFilter, maxDetectionDistance);
            }
        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "GameEventOptimizationMixin", "initConfig", e);
        }
        
        initialized = true;
    }
}
