package org.virgil.akiasync.mixin.brain.universal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;

/**
 * 通用AI计算器 - 增强版（合并ExpensiveAI功能）
 * 
 * 支持：
 * - 环境分析（原有）
 * - Brain内存优化（从ExpensiveAI合并）
 * - POI优化（从ExpensiveAI合并）
 * 
 * @author AkiAsync
 */
public final class UniversalAiCpuCalculator {
    
    /**
     * CPU密集型计算（增强版）
     */
    public static UniversalAiDiff runCpuOnly(Mob mob, UniversalAiSnapshot snap) {
        UniversalAiDiff diff = new UniversalAiDiff();
        
        analyzeEnvironment(mob, snap, diff);
        
        if (snap.hasBrainMemories()) {
            optimizeBrainMemories(mob, snap, diff);
        }
        
        if (snap.hasPOIs()) {
            optimizePOI(mob, snap, diff);
        }
        
        return diff;
    }
    
    /**
     * 环境分析（原有逻辑）
     */
    private static void analyzeEnvironment(Mob mob, UniversalAiSnapshot snap, UniversalAiDiff diff) {
        if (!snap.players().isEmpty()) {
            UUID target = snap.players().stream()
                .min(Comparator.comparingDouble(p -> p.pos().distSqr(mob.blockPosition())))
                .map(UniversalAiSnapshot.PlayerInfo::id)
                .orElse(null);
            diff.setTarget(target);
        }
    }
    
    /**
     * Brain内存优化（从BrainCpuCalculator移植）
     */
    private static void optimizeBrainMemories(Mob mob, UniversalAiSnapshot snap, UniversalAiDiff diff) {
        try {
            Map<MemoryModuleType<?>, UniversalAiSnapshot.MemoryEntry<?>> memories = snap.getBrainMemories();
            
            UniversalAiSnapshot.MemoryEntry<?> likedPlayerEntry = memories.get(MemoryModuleType.LIKED_PLAYER);
            if (likedPlayerEntry != null && likedPlayerEntry.getValue() != null) {
                diff.setLikedPlayer(likedPlayerEntry.getValue());
            }
            
        } catch (Exception e) {
            
        }
    }
    
    /**
     * POI优化（从BrainCpuCalculator移植并增强）
     */
    private static void optimizePOI(Mob mob, UniversalAiSnapshot snap, UniversalAiDiff diff) {
        try {
            Map<BlockPos, PoiRecord> poiSnapshot = snap.getPOIs();
            if (poiSnapshot == null || poiSnapshot.isEmpty()) {
                return;
            }
            
            Map<MemoryModuleType<?>, UniversalAiSnapshot.MemoryEntry<?>> memories = snap.getBrainMemories();
            
            List<ScoredPoi> scoredPois = new ArrayList<>();
            for (BlockPos poi : poiSnapshot.keySet()) {
                double score = scorePoi(poi, mob.blockPosition(), memories);
                scoredPois.add(new ScoredPoi(poi, score));
            }
            
            scoredPois.sort(Comparator.comparingDouble(ScoredPoi::score).reversed());
            
            if (!scoredPois.isEmpty()) {
                BlockPos topPoi = scoredPois.get(0).pos();
                diff.setTopPoi(topPoi);
            }
            
        } catch (Exception e) {
            
        }
    }
    
    /**
     * POI评分算法（从BrainCpuCalculator移植）
     */
    private static double scorePoi(BlockPos poi, BlockPos mobPos, 
                                   Map<MemoryModuleType<?>, UniversalAiSnapshot.MemoryEntry<?>> memories) {
        
        double dx = poi.getX() - mobPos.getX();
        double dy = poi.getY() - mobPos.getY();
        double dz = poi.getZ() - mobPos.getZ();
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        
        double match = 0.0;
        if (memories != null) {
            UniversalAiSnapshot.MemoryEntry<?> walkTarget = memories.get(MemoryModuleType.WALK_TARGET);
            if (walkTarget != null) {
                match = poi.getX() % 10 == 0 ? 1.0 : 0.3;
            }
        }
        
        double price = 1.0 - (poi.getY() % 100) / 100.0;
        
        double noise = (poi.hashCode() % 1000) / 1000.0;
        
        return (match * 1000 - dist * 10 - price * 100) * noise;
    }
    
    /**
     * POI评分结果
     */
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
}
