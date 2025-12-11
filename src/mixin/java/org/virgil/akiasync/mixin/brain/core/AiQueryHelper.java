package org.virgil.akiasync.mixin.brain.core;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.player.Player;
import org.virgil.akiasync.mixin.poi.PoiSpatialIndex;
import org.virgil.akiasync.mixin.poi.PoiSpatialIndexManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

public class AiQueryHelper {
    
    private static final AtomicLong totalQueries = new AtomicLong(0);
    private static final AtomicLong spatialIndexHits = new AtomicLong(0);
    private static final AtomicLong fallbackQueries = new AtomicLong(0);
    
    public static List<Player> getNearbyPlayers(Mob mob, double radius) {
        totalQueries.incrementAndGet();
        
        if (mob == null || mob.level().isClientSide) {
            return Collections.emptyList();
        }
        
        ServerLevel level = (ServerLevel) mob.level();
        AiSpatialIndex index = AiSpatialIndexManager.getIndex(level);
        
        if (index != null && index.isEnabled()) {
            
            List<Player> result = index.queryPlayers(mob.blockPosition(), (int)radius);
            spatialIndexHits.incrementAndGet();
            return result;
        }
        
        fallbackQueries.incrementAndGet();
        return level.getEntitiesOfClass(
            Player.class,
            mob.getBoundingBox().inflate(radius)
        );
    }
    
    public static <T extends LivingEntity> List<T> getNearbyEntities(
        Mob mob,
        Class<T> entityClass,
        double radius
    ) {
        return getNearbyEntities(mob, entityClass, radius, null);
    }
    
    public static <T extends LivingEntity> List<T> getNearbyEntities(
        Mob mob,
        Class<T> entityClass,
        double radius,
        Predicate<T> filter
    ) {
        totalQueries.incrementAndGet();
        
        if (mob == null || mob.level().isClientSide) {
            return Collections.emptyList();
        }
        
        ServerLevel level = (ServerLevel) mob.level();
        AiSpatialIndex index = AiSpatialIndexManager.getIndex(level);
        
        if (index != null && index.isEnabled()) {
            
            List<LivingEntity> candidates = index.queryEntities(mob.blockPosition(), (int)radius);
            spatialIndexHits.incrementAndGet();
            
            List<T> result = new ArrayList<>();
            for (LivingEntity entity : candidates) {
                if (entityClass.isInstance(entity)) {
                    T typed = entityClass.cast(entity);
                    if (filter == null || filter.test(typed)) {
                        result.add(typed);
                    }
                }
            }
            
            return result;
        }
        
        fallbackQueries.incrementAndGet();
        if (filter != null) {
            return level.getEntitiesOfClass(
                entityClass,
                mob.getBoundingBox().inflate(radius),
                filter
            );
        } else {
            return level.getEntitiesOfClass(
                entityClass,
                mob.getBoundingBox().inflate(radius)
            );
        }
    }
    
    public static List<Entity> getNearbyAnyEntities(
        Mob mob,
        double radius,
        Predicate<Entity> filter
    ) {
        totalQueries.incrementAndGet();
        
        if (mob == null || mob.level().isClientSide) {
            return Collections.emptyList();
        }
        
        ServerLevel level = (ServerLevel) mob.level();
        
        fallbackQueries.incrementAndGet();
        return level.getEntities(
            mob,
            mob.getBoundingBox().inflate(radius),
            filter
        );
    }
    
    public static List<PoiRecord> getNearbyPoi(Mob mob, int radius) {
        totalQueries.incrementAndGet();
        
        if (mob == null || mob.level().isClientSide) {
            return Collections.emptyList();
        }
        
        ServerLevel level = (ServerLevel) mob.level();
        PoiSpatialIndex index = PoiSpatialIndexManager.getIndex(level);
        
        if (index != null) {
            
            List<PoiRecord> result = index.queryRange(mob.blockPosition(), radius);
            spatialIndexHits.incrementAndGet();
            return result;
        }
        
        fallbackQueries.incrementAndGet();
        return level.getPoiManager()
            .getInRange(
                type -> true,
                mob.blockPosition(),
                radius,
                net.minecraft.world.entity.ai.village.poi.PoiManager.Occupancy.ANY
            )
            .toList();
    }
    
    public static List<PoiRecord> getNearbyPoiByType(
        Mob mob,
        PoiType type,
        int radius
    ) {
        totalQueries.incrementAndGet();
        
        if (mob == null || mob.level().isClientSide || type == null) {
            return Collections.emptyList();
        }
        
        ServerLevel level = (ServerLevel) mob.level();
        PoiSpatialIndex index = PoiSpatialIndexManager.getIndex(level);
        
        if (index != null) {
            
            List<PoiRecord> result = index.queryByType(mob.blockPosition(), type, radius);
            spatialIndexHits.incrementAndGet();
            return result;
        }
        
        fallbackQueries.incrementAndGet();
        return level.getPoiManager()
            .getInRange(
                t -> t.value().equals(type),
                mob.blockPosition(),
                radius,
                net.minecraft.world.entity.ai.village.poi.PoiManager.Occupancy.ANY
            )
            .toList();
    }
    
    public static List<Player> getNearbyPlayersFromPos(ServerLevel level, BlockPos pos, int radius) {
        totalQueries.incrementAndGet();
        
        if (level == null || pos == null) {
            return Collections.emptyList();
        }
        
        AiSpatialIndex index = AiSpatialIndexManager.getIndex(level);
        
        if (index != null && index.isEnabled()) {
            spatialIndexHits.incrementAndGet();
            return index.queryPlayers(pos, radius);
        }
        
        fallbackQueries.incrementAndGet();
        return level.getEntitiesOfClass(
            Player.class,
            new net.minecraft.world.phys.AABB(pos).inflate(radius)
        );
    }
    
    public static String getStatistics() {
        long queries = totalQueries.get();
        long hits = spatialIndexHits.get();
        long fallback = fallbackQueries.get();
        double spatialHitRate = queries > 0 ? 
            (hits * 100.0 / queries) : 0.0;
        double fallbackRate = queries > 0 ? 
            (fallback * 100.0 / queries) : 0.0;
        
        return String.format(
            "AI Query Helper: %d queries | %.2f%% spatial index | %.2f%% fallback",
            queries, spatialHitRate, fallbackRate
        );
    }
    
    public static void resetStatistics() {
        totalQueries.set(0);
        spatialIndexHits.set(0);
        fallbackQueries.set(0);
    }
    
    public static long getTotalQueries() {
        return totalQueries.get();
    }
    
    public static long getSpatialIndexHits() {
        return spatialIndexHits.get();
    }
    
    public static long getFallbackQueries() {
        return fallbackQueries.get();
    }
}
