package org.virgil.akiasync.async.structure;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.QuartPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 优化的结构查找器，基于MC源代码改进
 * 
 * MC原版算法分析：
 * 1. ChunkGenerator.findNearestMapStructure() 使用同心圆搜索
 * 2. 从内到外逐层搜索，每层检查所有区块
 * 3. 对每个区块调用 getStructureAt() 检查结构
 * 4. 使用 skipKnownStructures 跳过已知结构
 * 
 * 优化策略：
 * 1. 螺旋搜索：更早找到近距离结构
 * 2. 生物群系感知：优先搜索匹配的生物群系
 * 3. 结构缓存：避免重复计算
 * 4. 智能跳过：基于结构生成规律跳过不可能的区块
 */
public class OptimizedStructureLocator {
    
    // 使用专用的缓存管理器
    private static StructureCacheManager cacheManager;
    
    // 生物群系-结构兼容性映射
    private static final Map<String, Set<String>> BIOME_STRUCTURE_COMPATIBILITY = new ConcurrentHashMap<>();
    
    // 结构稀有度映射 - 用于动态调整搜索半径
    private static final Map<String, Integer> STRUCTURE_RARITY = Map.of(
        "minecraft:village", 32,
        "minecraft:pillager_outpost", 64,
        "minecraft:stronghold", 256,
        "minecraft:woodland_mansion", 512,
        "minecraft:ocean_monument", 128,
        "minecraft:desert_pyramid", 64,
        "minecraft:jungle_pyramid", 64,
        "minecraft:swamp_hut", 64
    );
    
    static {
        initializeBiomeCompatibility();
    }
    
    /**
     * 初始化缓存管理器
     */
    public static void initialize(org.virgil.akiasync.AkiAsyncPlugin plugin) {
        if (cacheManager == null) {
            cacheManager = StructureCacheManager.getInstance(plugin);
        }
    }
    
    /**
     * 优化的结构查找方法
     * 
     * @param level 服务器世界
     * @param structures 要查找的结构集合
     * @param startPos 起始位置
     * @param searchRadius 搜索半径（区块）
     * @param skipKnownStructures 是否跳过已知结构
     * @return 找到的结构位置和类型，如果没找到返回null
     */
    public static Pair<BlockPos, Holder<Structure>> findNearestStructureOptimized(
            ServerLevel level, 
            HolderSet<Structure> structures, 
            BlockPos startPos, 
            int searchRadius, 
            boolean skipKnownStructures) {
        
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        ChunkPos startChunk = new ChunkPos(startPos);
        
        // 1. 检查缓存
        String cacheKey = getCacheKey(structures, startPos, searchRadius);
        if (cacheManager != null) {
            BlockPos cachedPos = cacheManager.getCachedStructure(cacheKey);
            if (cachedPos != null) {
                // 验证缓存的结构是否仍然存在
                Structure structure = getStructureAtPosition(level, cachedPos, structures);
                if (structure != null) {
                    return Pair.of(cachedPos, Holder.direct(structure));
                }
            }
            
            // 2. 检查负缓存
            if (cacheManager.isNegativeCached(cacheKey)) {
                return null;
            }
        }
        
        // 3. 动态调整搜索半径
        int adaptiveRadius = getAdaptiveSearchRadius(structures, searchRadius);
        
        // 4. 获取生物群系感知的搜索顺序
        List<ChunkPos> searchOrder = getBiomeAwareSearchOrder(level, startChunk, structures, adaptiveRadius);
        
        // 5. 执行优化搜索
        Pair<BlockPos, Holder<Structure>> result = executeOptimizedSearch(
            level, generator, structures, searchOrder, skipKnownStructures);
        
        // 6. 更新缓存
        if (cacheManager != null) {
            if (result != null) {
                cacheManager.cacheStructure(cacheKey, result.getFirst());
            } else {
                cacheManager.cacheNegativeResult(cacheKey);
            }
        }
        
        return result;
    }
    
    /**
     * 生成螺旋搜索顺序，优先搜索近距离区块
     */
    private static List<ChunkPos> getSpiralSearchOrder(ChunkPos center, int radius) {
        List<ChunkPos> positions = new ArrayList<>();
        
        // 添加中心点
        positions.add(center);
        
        // 螺旋搜索：右->下->左->上的顺序
        int x = center.x;
        int z = center.z;
        
        for (int layer = 1; layer <= radius; layer++) {
            // 移动到当前层的起始位置（右上角）
            x = center.x + layer;
            z = center.z - layer + 1;
            
            // 向下移动
            for (int i = 0; i < 2 * layer; i++) {
                positions.add(new ChunkPos(x, z));
                z++;
            }
            
            // 向左移动
            for (int i = 0; i < 2 * layer; i++) {
                x--;
                positions.add(new ChunkPos(x, z));
            }
            
            // 向上移动
            for (int i = 0; i < 2 * layer; i++) {
                z--;
                positions.add(new ChunkPos(x, z));
            }
            
            // 向右移动
            for (int i = 0; i < 2 * layer; i++) {
                x++;
                positions.add(new ChunkPos(x, z));
            }
        }
        
        return positions;
    }
    
    /**
     * 生物群系感知的搜索顺序
     */
    private static List<ChunkPos> getBiomeAwareSearchOrder(
            ServerLevel level, ChunkPos center, HolderSet<Structure> structures, int radius) {
        
        List<ChunkPos> spiralOrder = getSpiralSearchOrder(center, radius);
        
        // 如果没有生物群系兼容性信息，直接返回螺旋顺序
        Set<String> compatibleBiomes = getCompatibleBiomes(structures);
        if (compatibleBiomes.isEmpty()) {
            return spiralOrder;
        }
        
        // 按生物群系兼容性排序
        spiralOrder.sort((pos1, pos2) -> {
            float score1 = getBiomeCompatibilityScore(level, pos1, compatibleBiomes);
            float score2 = getBiomeCompatibilityScore(level, pos2, compatibleBiomes);
            return Float.compare(score2, score1); // 降序排列
        });
        
        return spiralOrder;
    }
    
    /**
     * 执行优化搜索
     */
    private static Pair<BlockPos, Holder<Structure>> executeOptimizedSearch(
            ServerLevel level, 
            ChunkGenerator generator, 
            HolderSet<Structure> structures, 
            List<ChunkPos> searchOrder, 
            boolean skipKnownStructures) {
        
        for (ChunkPos chunkPos : searchOrder) {
            // 检查区块是否已生成（避免强制生成）
            if (!level.hasChunk(chunkPos.x, chunkPos.z)) {
                continue;
            }
            
            // 使用MC原版的结构检查逻辑
            for (Holder<Structure> structureHolder : structures) {
                Structure structure = structureHolder.value();
                
                // 直接获取结构起始位置并检查
                try {
                    StructureStart structureStart = level.getChunk(chunkPos.x, chunkPos.z)
                        .getStartForStructure(structure);
                    
                    if (structureStart != null && structureStart.isValid()) {
                        if (skipKnownStructures) {
                            // 检查是否为已知结构（这里可以添加更复杂的逻辑）
                            continue;
                        }
                        
                        BlockPos structurePos = structureStart.getBoundingBox().getCenter();
                        return Pair.of(structurePos, structureHolder);
                    }
                } catch (Exception e) {
                    // 如果区块未完全加载或结构检查失败，跳过此区块
                    continue;
                }
            }
        }
        
        return null;
    }
    
    /**
     * 获取自适应搜索半径
     */
    private static int getAdaptiveSearchRadius(HolderSet<Structure> structures, int defaultRadius) {
        int maxRarity = 0;
        
        for (Holder<Structure> holder : structures) {
            String structureName = getStructureName(holder);
            int rarity = STRUCTURE_RARITY.getOrDefault(structureName, defaultRadius);
            maxRarity = Math.max(maxRarity, rarity);
        }
        
        return Math.max(defaultRadius, maxRarity);
    }
    
    /**
     * 获取兼容的生物群系
     */
    private static Set<String> getCompatibleBiomes(HolderSet<Structure> structures) {
        Set<String> compatibleBiomes = new HashSet<>();
        
        for (Holder<Structure> holder : structures) {
            String structureName = getStructureName(holder);
            Set<String> biomes = BIOME_STRUCTURE_COMPATIBILITY.get(structureName);
            if (biomes != null) {
                compatibleBiomes.addAll(biomes);
            }
        }
        
        return compatibleBiomes;
    }
    
    /**
     * 计算生物群系兼容性得分
     */
    private static float getBiomeCompatibilityScore(ServerLevel level, ChunkPos chunkPos, Set<String> compatibleBiomes) {
        if (compatibleBiomes.isEmpty()) {
            return 1.0f; // 没有限制时所有位置得分相同
        }
        
        // 采样区块中心的生物群系
        BlockPos samplePos = chunkPos.getMiddleBlockPosition(64);
        Biome biome = level.getBiome(samplePos).value();
        String biomeName = getBiomeName(level, samplePos); // 兼容性生物群系名称获取
        
        return compatibleBiomes.contains(biomeName) ? 1.0f : 0.1f;
    }
    
    /**
     * 获取指定位置的结构
     */
    private static Structure getStructureAtPosition(ServerLevel level, BlockPos pos, HolderSet<Structure> structures) {
        ChunkPos chunkPos = new ChunkPos(pos);
        
        if (!level.hasChunk(chunkPos.x, chunkPos.z)) {
            return null;
        }
        
        for (Holder<Structure> holder : structures) {
            Structure structure = holder.value();
            StructureStart start = level.getChunk(chunkPos.x, chunkPos.z).getStartForStructure(structure);
            
            if (start != null && start.isValid() && start.getBoundingBox().isInside(pos)) {
                return structure;
            }
        }
        
        return null;
    }
    
    /**
     * 生成缓存键
     */
    private static String getCacheKey(HolderSet<Structure> structures, BlockPos startPos, int radius) {
        StringBuilder sb = new StringBuilder();
        
        for (Holder<Structure> holder : structures) {
            sb.append(holder.getRegisteredName()).append(",");
        }
        
        // 将位置量化到区块级别以提高缓存命中率
        ChunkPos chunkPos = new ChunkPos(startPos);
        sb.append(chunkPos.x).append(",").append(chunkPos.z).append(",").append(radius);
        
        return sb.toString();
    }
    
    /**
     * 初始化生物群系-结构兼容性映射
     */
    private static void initializeBiomeCompatibility() {
        // 村庄
        BIOME_STRUCTURE_COMPATIBILITY.put("minecraft:village", Set.of(
            "minecraft:plains", "minecraft:desert", "minecraft:savanna", 
            "minecraft:taiga", "minecraft:snowy_plains", "minecraft:snowy_taiga"
        ));
        
        // 沙漠神殿
        BIOME_STRUCTURE_COMPATIBILITY.put("minecraft:desert_pyramid", Set.of(
            "minecraft:desert"
        ));
        
        // 丛林神殿
        BIOME_STRUCTURE_COMPATIBILITY.put("minecraft:jungle_pyramid", Set.of(
            "minecraft:jungle", "minecraft:bamboo_jungle"
        ));
        
        // 海底神殿
        BIOME_STRUCTURE_COMPATIBILITY.put("minecraft:ocean_monument", Set.of(
            "minecraft:ocean", "minecraft:deep_ocean", "minecraft:cold_ocean", 
            "minecraft:lukewarm_ocean", "minecraft:warm_ocean"
        ));
        
        // 林地府邸
        BIOME_STRUCTURE_COMPATIBILITY.put("minecraft:woodland_mansion", Set.of(
            "minecraft:dark_forest"
        ));
        
        // 掠夺者前哨站
        BIOME_STRUCTURE_COMPATIBILITY.put("minecraft:pillager_outpost", Set.of(
            "minecraft:plains", "minecraft:desert", "minecraft:savanna", 
            "minecraft:taiga", "minecraft:grove", "minecraft:snowy_plains"
        ));
    }
    
    /**
     * 清理缓存（定期调用以防止内存泄漏）
     */
    public static void clearCache() {
        if (cacheManager != null) {
            cacheManager.clearCache();
        }
    }
    
    /**
     * 获取缓存统计信息
     */
    public static String getCacheStats() {
        if (cacheManager != null) {
            return cacheManager.getStatistics().toString();
        }
        return "Structure Cache: Not initialized";
    }
    
    /**
     * 获取结构名称（兼容性方法）
     */
    private static String getStructureName(Holder<Structure> holder) {
        try {
            // 尝试使用新的API
            if (holder.unwrapKey().isPresent()) {
                return holder.unwrapKey().get().location().toString();
            }
        } catch (Exception e) {
            // 忽略异常，尝试其他方法
        }
        
        try {
            // 尝试使用旧的API
            return holder.toString();
        } catch (Exception e) {
            // 如果都失败了，返回默认值
            return "unknown_structure";
        }
    }
    
    /**
     * 获取生物群系名称（兼容性方法）
     */
    private static String getBiomeName(ServerLevel level, BlockPos pos) {
        try {
            Holder<Biome> biomeHolder = level.getBiome(pos);
            
            // 尝试使用新的API
            if (biomeHolder.unwrapKey().isPresent()) {
                return biomeHolder.unwrapKey().get().location().toString();
            }
            
            // 尝试使用旧的API
            return biomeHolder.value().toString();
        } catch (Exception e) {
            // 如果获取失败，返回默认值
            return "unknown_biome";
        }
    }
    
    /**
     * 关闭优化器
     */
    public static void shutdown() {
        if (cacheManager != null) {
            cacheManager.shutdown();
            cacheManager = null;
        }
    }
}
