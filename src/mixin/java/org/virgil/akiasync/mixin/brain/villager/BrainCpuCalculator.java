package org.virgil.akiasync.mixin.brain.villager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.ExpirableValue;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;

/**
 * Brain CPU-intensive calculator.
 * @author Virgil
 */
public final class BrainCpuCalculator {
    
    private static class ScoredPoi {
        final BlockPos pos;
        final double score;
        
        ScoredPoi(BlockPos pos, double score) {
            this.pos = pos;
            this.score = score;
        }
        
        double score() { return score; }
        BlockPos pos() { return pos; }
    }
    
    @SuppressWarnings("unchecked")
    public static <E extends LivingEntity> BrainDiff runCpuOnly(
            Brain<E> brain,
            ServerLevel level,
            Map<BlockPos, PoiRecord> poiSnapshot
    ) {
        try {
            Map<MemoryModuleType<?>, Object> memorySnapshot = brain.getMemories().entrySet()
                .stream()
                .collect(Collectors.toMap(
                    e -> e.getKey(),
                    e -> {
                        Optional<?> opt = e.getValue();
                        if (opt != null && opt.isPresent()) {
                            Object val = opt.get();
                            if (val instanceof ExpirableValue) {
                                return ((ExpirableValue<?>) val).getValue();
                            }
                            return val;
                        }
                        return null;
                    }
                ));
            
            List<BlockPos> pois = new ArrayList<>();
            if (poiSnapshot != null) {
                pois.addAll(poiSnapshot.keySet());
                
                while (pois.size() < 64 && !poiSnapshot.isEmpty()) {
                    pois.addAll(poiSnapshot.keySet());
                }
            }
            
            List<ScoredPoi> scoredPois = pois.stream()
                .map(poi -> new ScoredPoi(poi, score(poi, memorySnapshot)))
                .collect(Collectors.toList());
            
            scoredPois.sort(Comparator.comparingDouble(ScoredPoi::score).reversed());
            
            BrainDiff diff = new BrainDiff();
            
            if (!scoredPois.isEmpty()) {
                BlockPos topPoi = scoredPois.get(0).pos();
                diff.setTopPoi(topPoi);
            }
            
            Object likedPlayer = memorySnapshot.get(MemoryModuleType.LIKED_PLAYER);
            if (likedPlayer != null) {
                diff.setLikedPlayer(likedPlayer);
            }
            
            return diff;
            
        } catch (Exception e) {
            return new BrainDiff();
        }
    }
    
    private static double score(BlockPos poi, Map<MemoryModuleType<?>, Object> memory) {
        double dist = Math.sqrt(
            poi.getX() * poi.getX() + 
            poi.getY() * poi.getY() + 
            poi.getZ() * poi.getZ()
        );
        
        double match = 0.0;
        Object walkTarget = memory.get(MemoryModuleType.WALK_TARGET);
        if (walkTarget != null) {
            match = poi.getX() % 10 == 0 ? 1.0 : 0.3;
        }
        
        double price = 1.0 - (poi.getY() % 100) / 100.0;
        
        double noise = (poi.hashCode() % 1000) / 1000.0;
        
        return (match * 1000 - dist * 10 - price * 100) * noise;
    }
}

