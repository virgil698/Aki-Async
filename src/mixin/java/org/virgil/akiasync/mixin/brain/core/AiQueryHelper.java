package org.virgil.akiasync.mixin.brain.core;

import net.minecraft.core.BlockPos;
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
import java.util.function.Predicate;

/**
 * AI查询助手
 * 
 * 统一的AI查询接口，所有AI优化都通过这个接口查询
 * 自动使用空间索引，提供一致的O(1)性能
 * 
 * 替代原有的：
 * - level.getEntitiesOfClass(...)
 * - level.getNearbyEntities(...)
 * - BatchPoiManager.getPoiInRange(...)
 * 
 * @author AkiAsync
 */
public class AiQueryHelper {
    
    private static volatile long totalQueries = 0;
    private static volatile long spatialIndexHits = 0;
    private static volatile long fallbackQueries = 0;
    
    /**
     * 查询附近的玩家 - O(1)
     * 
     * 替代: level.getEntitiesOfClass(Player.class, box)
     */
    public static List<Player> getNearbyPlayers(Mob mob, double radius) {
        totalQueries++;
        
        if (mob == null || mob.level().isClientSide) {
            return Collections.emptyList();
        }
        
        ServerLevel level = (ServerLevel) mob.level();
        AiSpatialIndex index = AiSpatialIndexManager.getIndex(level);
        
        if (index != null && index.isEnabled()) {
            
            List<Player> result = index.queryPlayers(mob.blockPosition(), (int)radius);
            spatialIndexHits++;
            return result;
        }
        
        fallbackQueries++;
        return level.getEntitiesOfClass(
            Player.class,
            mob.getBoundingBox().inflate(radius)
        );
    }
    
    /**
     * 查询附近的实体 - O(1)
     * 
     * 替代: level.getEntitiesOfClass(entityClass, box)
     */
    public static <T extends LivingEntity> List<T> getNearbyEntities(
        Mob mob,
        Class<T> entityClass,
        double radius
    ) {
        return getNearbyEntities(mob, entityClass, radius, null);
    }
    
    /**
     * 查询附近的实体（带过滤器） - O(1)
     * 
     * 替代: level.getEntitiesOfClass(entityClass, box, filter)
     */
    public static <T extends LivingEntity> List<T> getNearbyEntities(
        Mob mob,
        Class<T> entityClass,
        double radius,
        Predicate<T> filter
    ) {
        totalQueries++;
        
        if (mob == null || mob.level().isClientSide) {
            return Collections.emptyList();
        }
        
        ServerLevel level = (ServerLevel) mob.level();
        AiSpatialIndex index = AiSpatialIndexManager.getIndex(level);
        
        if (index != null && index.isEnabled()) {
            
            List<LivingEntity> candidates = index.queryEntities(mob.blockPosition(), (int)radius);
            spatialIndexHits++;
            
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
        
        fallbackQueries++;
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
    
    /**
     * 查询附近的任意实体（不限于LivingEntity） - O(1)
     * 
     * 替代: level.getEntities(mob, box, filter)
     */
    public static List<Entity> getNearbyAnyEntities(
        Mob mob,
        double radius,
        Predicate<Entity> filter
    ) {
        totalQueries++;
        
        if (mob == null || mob.level().isClientSide) {
            return Collections.emptyList();
        }
        
        ServerLevel level = (ServerLevel) mob.level();
        
        fallbackQueries++;
        return level.getEntities(
            mob,
            mob.getBoundingBox().inflate(radius),
            filter
        );
    }
    
    /**
     * 查询附近的POI - O(1)
     * 
     * 替代: BatchPoiManager.getPoiInRange(level, pos, radius)
     */
    public static List<PoiRecord> getNearbyPoi(Mob mob, int radius) {
        totalQueries++;
        
        if (mob == null || mob.level().isClientSide) {
            return Collections.emptyList();
        }
        
        ServerLevel level = (ServerLevel) mob.level();
        PoiSpatialIndex index = PoiSpatialIndexManager.getIndex(level);
        
        if (index != null) {
            
            List<PoiRecord> result = index.queryRange(mob.blockPosition(), radius);
            spatialIndexHits++;
            return result;
        }
        
        fallbackQueries++;
        return level.getPoiManager()
            .getInRange(
                type -> true,
                mob.blockPosition(),
                radius,
                net.minecraft.world.entity.ai.village.poi.PoiManager.Occupancy.ANY
            )
            .toList();
    }
    
    /**
     * 查询特定类型的POI - 更快
     * 
     * 使用类型索引，比通用查询更快
     */
    public static List<PoiRecord> getNearbyPoiByType(
        Mob mob,
        PoiType type,
        int radius
    ) {
        totalQueries++;
        
        if (mob == null || mob.level().isClientSide || type == null) {
            return Collections.emptyList();
        }
        
        ServerLevel level = (ServerLevel) mob.level();
        PoiSpatialIndex index = PoiSpatialIndexManager.getIndex(level);
        
        if (index != null) {
            
            List<PoiRecord> result = index.queryByType(mob.blockPosition(), type, radius);
            spatialIndexHits++;
            return result;
        }
        
        fallbackQueries++;
        return level.getPoiManager()
            .getInRange(
                t -> t.equals(type),
                mob.blockPosition(),
                radius,
                net.minecraft.world.entity.ai.village.poi.PoiManager.Occupancy.ANY
            )
            .toList();
    }
    
    /**
     * 查询附近的玩家（从BlockPos） - O(1)
     */
    public static List<Player> getNearbyPlayersFromPos(ServerLevel level, BlockPos pos, int radius) {
        totalQueries++;
        
        if (level == null || pos == null) {
            return Collections.emptyList();
        }
        
        AiSpatialIndex index = AiSpatialIndexManager.getIndex(level);
        
        if (index != null && index.isEnabled()) {
            spatialIndexHits++;
            return index.queryPlayers(pos, radius);
        }
        
        fallbackQueries++;
        return level.getEntitiesOfClass(
            Player.class,
            new net.minecraft.world.phys.AABB(pos).inflate(radius)
        );
    }
    
    /**
     * 获取统计信息
     */
    public static String getStatistics() {
        double spatialHitRate = totalQueries > 0 ? 
            (spatialIndexHits * 100.0 / totalQueries) : 0.0;
        double fallbackRate = totalQueries > 0 ? 
            (fallbackQueries * 100.0 / totalQueries) : 0.0;
        
        return String.format(
            "AI Query Helper: %d queries | %.2f%% spatial index | %.2f%% fallback",
            totalQueries, spatialHitRate, fallbackRate
        );
    }
    
    /**
     * 重置统计信息
     */
    public static void resetStatistics() {
        totalQueries = 0;
        spatialIndexHits = 0;
        fallbackQueries = 0;
    }
    
    /**
     * 获取总查询次数
     */
    public static long getTotalQueries() {
        return totalQueries;
    }
    
    /**
     * 获取空间索引命中次数
     */
    public static long getSpatialIndexHits() {
        return spatialIndexHits;
    }
    
    /**
     * 获取降级查询次数
     */
    public static long getFallbackQueries() {
        return fallbackQueries;
    }
}
