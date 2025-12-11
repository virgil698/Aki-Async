package org.virgil.akiasync.mixin.mixins.datapack;

import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.virgil.akiasync.mixin.bridge.Bridge;
import org.virgil.akiasync.mixin.bridge.BridgeManager;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

@Mixin(EntitySelector.class)
public class EntitySelectorInactiveSkipMixin {
    
    @Unique
    private static volatile boolean initialized = false;
    
    @Unique
    private static volatile boolean enabled = false;
    
    @Unique
    private static volatile int skipLevel = 2;
    
    @Unique
    private static volatile double simulationDistanceMultiplier = 1.0;
    
    @Unique
    private static volatile long cacheDurationMs = 1000L;
    
    @Unique
    private static volatile Set<String> whitelistTypes = null;
    
    @Unique
    private static final Map<UUID, Long> activityCache = new ConcurrentHashMap<>();
    
    @Unique
    private static long lastCacheCleanup = 0L;
    
    @Unique
    private static final long CACHE_CLEANUP_INTERVAL_MS = 10000L; 
    
    @Inject(method = "getPredicate", at = @At("RETURN"), cancellable = true)
    private void skipInactiveEntities(Vec3 pos, AABB box, FeatureFlagSet enabledFeatures, 
                                     CallbackInfoReturnable<Predicate<Entity>> cir) {
        
        if (!initialized) {
            Bridge bridge = BridgeManager.getBridge();
            if (bridge != null) {
                enabled = bridge.isExecuteCommandInactiveSkipEnabled();
                
                if (enabled) {
                    skipLevel = bridge.getExecuteCommandSkipLevel();
                    simulationDistanceMultiplier = bridge.getExecuteCommandSimulationDistanceMultiplier();
                    cacheDurationMs = bridge.getExecuteCommandCacheDurationMs();
                    whitelistTypes = bridge.getExecuteCommandWhitelistTypes();
                    
                    if (bridge.isDebugLoggingEnabled() && bridge.isExecuteCommandDebugEnabled()) {
                        bridge.debugLog("[AkiAsync] Execute command inactive skip enabled");
                        bridge.debugLog("  - Skip level: %d", skipLevel);
                        bridge.debugLog("  - Simulation distance multiplier: %.2f", simulationDistanceMultiplier);
                        bridge.debugLog("  - Cache duration: %d ms", cacheDurationMs);
                        bridge.debugLog("  - Whitelist types: %d", whitelistTypes.size());
                    }
                }
                
                initialized = true;
            }
        }
        
        if (!enabled || skipLevel == 0) return;
        
        Predicate<Entity> original = cir.getReturnValue();
        
        Predicate<Entity> enhanced = entity -> {
            
            if (!original.test(entity)) return false;
            
            if (isWhitelisted(entity)) return true;
            
            long currentTime = System.currentTimeMillis();
            
            if (currentTime - lastCacheCleanup > CACHE_CLEANUP_INTERVAL_MS) {
                cleanExpiredCache(currentTime);
                lastCacheCleanup = currentTime;
            }
            
            if (isCached(entity, currentTime)) return true;
            
            boolean active = isEntityActive(entity, skipLevel, simulationDistanceMultiplier);
            
            if (active) {
                
                activityCache.put(entity.getUUID(), currentTime);
            } else {
                
                Bridge bridge = BridgeManager.getBridge();
                if (bridge != null && bridge.isDebugLoggingEnabled() && bridge.isExecuteCommandDebugEnabled()) {
                    logSkippedEntity(entity);
                }
            }
            
            return active;
        };
        
        cir.setReturnValue(enhanced);
    }
    
    @Unique
    private static boolean isWhitelisted(Entity entity) {
        if (whitelistTypes == null || whitelistTypes.isEmpty()) return false;
        
        String entityType = entity.getType().toString();
        return whitelistTypes.contains(entityType);
    }
    
    @Unique
    private static boolean isCached(Entity entity, long currentTime) {
        Long lastCheck = activityCache.get(entity.getUUID());
        return lastCheck != null && (currentTime - lastCheck) < cacheDurationMs;
    }
    
    @Unique
    private static boolean isEntityActive(Entity entity, int level, double simDistMultiplier) {
        
        if (entity.isRemoved()) return false;
        if (entity instanceof LivingEntity living && !living.isAlive()) return false;
        
        if (!(entity.level() instanceof ServerLevel serverLevel)) return false;
        
        if (level >= 1) {
            if (!isChunkEntityTicking(entity, serverLevel)) return false;
        }
        
        if (level >= 2) {
            if (!isWithinSimulationDistance(entity, serverLevel, simDistMultiplier)) return false;
        }
        
        if (level >= 3) {
            
        }
        
        return true;
    }
    
    @Unique
    private static boolean isChunkEntityTicking(Entity entity, ServerLevel serverLevel) {
        BlockPos blockPos = entity.blockPosition();
        ChunkPos chunkPos = new ChunkPos(blockPos);
        
        LevelChunk chunk = serverLevel.getChunkSource().getChunkNow(chunkPos.x, chunkPos.z);
        if (chunk == null) return false;
        
        ChunkHolder holder = serverLevel.getChunkSource().chunkMap.getVisibleChunkIfPresent(chunkPos.toLong());
        if (holder == null) return false;
        
        FullChunkStatus status = holder.getFullStatus();
        return status == FullChunkStatus.ENTITY_TICKING;
    }
    
    @Unique
    private static boolean isWithinSimulationDistance(Entity entity, ServerLevel serverLevel, double multiplier) {
        
        int simDistance = serverLevel.getServer().getPlayerList().getSimulationDistance();
        double maxDistanceBlocks = simDistance * 16 * multiplier;
        double maxDistanceSq = maxDistanceBlocks * maxDistanceBlocks;
        
        for (ServerPlayer player : serverLevel.players()) {
            double distSq = player.distanceToSqr(entity);
            if (distSq < maxDistanceSq) {
                return true;
            }
        }
        
        return false;
    }
    
    @Unique
    private static void cleanExpiredCache(long currentTime) {
        activityCache.entrySet().removeIf(entry -> 
            (currentTime - entry.getValue()) > cacheDurationMs * 2
        );
    }
    
    @Unique
    private static void logSkippedEntity(Entity entity) {
        Bridge bridge = BridgeManager.getBridge();
        if (bridge != null && bridge.isDebugLoggingEnabled()) {
            BlockPos pos = entity.blockPosition();
            bridge.debugLog("[AkiAsync] Skipped entity: %s at (%d, %d, %d)", 
                entity.getType().toString(), pos.getX(), pos.getY(), pos.getZ());
        }
    }
}
