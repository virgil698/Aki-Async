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

public class OptimizedStructureLocator {
    
    private static StructureCacheManager cacheManager;
    
    private static final Map<String, Set<String>> BIOME_STRUCTURE_COMPATIBILITY = new ConcurrentHashMap<>();
    
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
    
    public static void initialize(org.virgil.akiasync.AkiAsyncPlugin plugin) {
        if (cacheManager == null) {
            cacheManager = StructureCacheManager.getInstance(plugin);
        }
    }
    
    public static Pair<BlockPos, Holder<Structure>> findNearestStructureOptimized(
            ServerLevel level, 
            HolderSet<Structure> structures, 
            BlockPos startPos, 
            int searchRadius, 
            boolean skipKnownStructures) {
        
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        ChunkPos startChunk = new ChunkPos(startPos);
        
        String cacheKey = getCacheKey(structures, startPos, searchRadius);
        if (cacheManager != null) {
            BlockPos cachedPos = cacheManager.getCachedStructure(cacheKey);
            if (cachedPos != null) {
                Structure structure = getStructureAtPosition(level, cachedPos, structures);
                if (structure != null) {
                    return Pair.of(cachedPos, Holder.direct(structure));
                }
            }
            
            if (cacheManager.isNegativeCached(cacheKey)) {
                return null;
            }
        }
        
        int adaptiveRadius = getAdaptiveSearchRadius(structures, searchRadius);
        
        List<ChunkPos> searchOrder = getBiomeAwareSearchOrder(level, startChunk, structures, adaptiveRadius);
        
        Pair<BlockPos, Holder<Structure>> result = executeOptimizedSearch(
            level, generator, structures, searchOrder, skipKnownStructures);
        
        if (cacheManager != null) {
            if (result != null) {
                cacheManager.cacheStructure(cacheKey, result.getFirst());
            } else {
                cacheManager.cacheNegativeResult(cacheKey);
            }
        }
        
        return result;
    }
    
    private static List<ChunkPos> getSpiralSearchOrder(ChunkPos center, int radius) {
        List<ChunkPos> positions = new ArrayList<>();
        
        positions.add(center);
        
        int x = center.x;
        int z = center.z;
        
        for (int layer = 1; layer <= radius; layer++) {
            x = center.x + layer;
            z = center.z - layer + 1;
            
            for (int i = 0; i < 2 * layer; i++) {
                positions.add(new ChunkPos(x, z));
                z++;
            }
            
            for (int i = 0; i < 2 * layer; i++) {
                x--;
                positions.add(new ChunkPos(x, z));
            }
            
            for (int i = 0; i < 2 * layer; i++) {
                z--;
                positions.add(new ChunkPos(x, z));
            }
            
            for (int i = 0; i < 2 * layer; i++) {
                x++;
                positions.add(new ChunkPos(x, z));
            }
        }
        
        return positions;
    }
    
    private static List<ChunkPos> getBiomeAwareSearchOrder(
            ServerLevel level, ChunkPos center, HolderSet<Structure> structures, int radius) {
        
        List<ChunkPos> spiralOrder = getSpiralSearchOrder(center, radius);
        
        Set<String> compatibleBiomes = getCompatibleBiomes(structures);
        if (compatibleBiomes.isEmpty()) {
            return spiralOrder;
        }
        
        spiralOrder.sort((pos1, pos2) -> {
            float score1 = getBiomeCompatibilityScore(level, pos1, compatibleBiomes);
            float score2 = getBiomeCompatibilityScore(level, pos2, compatibleBiomes);
            return Float.compare(score2, score1);
        });
        
        return spiralOrder;
    }
    
    private static Pair<BlockPos, Holder<Structure>> executeOptimizedSearch(
            ServerLevel level, 
            ChunkGenerator generator, 
            HolderSet<Structure> structures, 
            List<ChunkPos> searchOrder, 
            boolean skipKnownStructures) {
        
        for (ChunkPos chunkPos : searchOrder) {
            if (!level.hasChunk(chunkPos.x, chunkPos.z)) {
                continue;
            }
            
            for (Holder<Structure> structureHolder : structures) {
                Structure structure = structureHolder.value();
                
                try {
                    StructureStart structureStart = level.getChunk(chunkPos.x, chunkPos.z)
                        .getStartForStructure(structure);
                    
                    if (structureStart != null && structureStart.isValid()) {
                        if (skipKnownStructures) {
                            continue;
                        }
                        
                        BlockPos structurePos = structureStart.getBoundingBox().getCenter();
                        return Pair.of(structurePos, structureHolder);
                    }
                } catch (Exception e) {
                    continue;
                }
            }
        }
        
        return null;
    }
    
    private static int getAdaptiveSearchRadius(HolderSet<Structure> structures, int defaultRadius) {
        int maxRarity = 0;
        
        for (Holder<Structure> holder : structures) {
            String structureName = getStructureName(holder);
            int rarity = STRUCTURE_RARITY.getOrDefault(structureName, defaultRadius);
            maxRarity = Math.max(maxRarity, rarity);
        }
        
        return Math.max(defaultRadius, maxRarity);
    }
    
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
    
    private static float getBiomeCompatibilityScore(ServerLevel level, ChunkPos chunkPos, Set<String> compatibleBiomes) {
        if (compatibleBiomes.isEmpty()) {
            return 1.0f;
        }
        
        BlockPos samplePos = chunkPos.getMiddleBlockPosition(64);
        Biome biome = level.getBiome(samplePos).value();
        String biomeName = getBiomeName(level, samplePos);
        
        return compatibleBiomes.contains(biomeName) ? 1.0f : 0.1f;
    }
    
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
    
    private static String getCacheKey(HolderSet<Structure> structures, BlockPos startPos, int radius) {
        StringBuilder sb = new StringBuilder();
        
        for (Holder<Structure> holder : structures) {
            sb.append(holder.getRegisteredName()).append(",");
        }
        
        ChunkPos chunkPos = new ChunkPos(startPos);
        sb.append(chunkPos.x).append(",").append(chunkPos.z).append(",").append(radius);
        
        return sb.toString();
    }
    
    private static void initializeBiomeCompatibility() {
        BIOME_STRUCTURE_COMPATIBILITY.put("minecraft:village", Set.of(
            "minecraft:plains", "minecraft:desert", "minecraft:savanna", 
            "minecraft:taiga", "minecraft:snowy_plains", "minecraft:snowy_taiga"
        ));
        
        BIOME_STRUCTURE_COMPATIBILITY.put("minecraft:desert_pyramid", Set.of(
            "minecraft:desert"
        ));
        
        BIOME_STRUCTURE_COMPATIBILITY.put("minecraft:jungle_pyramid", Set.of(
            "minecraft:jungle", "minecraft:bamboo_jungle"
        ));
        
        BIOME_STRUCTURE_COMPATIBILITY.put("minecraft:ocean_monument", Set.of(
            "minecraft:ocean", "minecraft:deep_ocean", "minecraft:cold_ocean", 
            "minecraft:lukewarm_ocean", "minecraft:warm_ocean"
        ));
        
        BIOME_STRUCTURE_COMPATIBILITY.put("minecraft:woodland_mansion", Set.of(
            "minecraft:dark_forest"
        ));
        
        BIOME_STRUCTURE_COMPATIBILITY.put("minecraft:pillager_outpost", Set.of(
            "minecraft:plains", "minecraft:desert", "minecraft:savanna", 
            "minecraft:taiga", "minecraft:grove", "minecraft:snowy_plains"
        ));
    }
    
    public static void clearCache() {
        if (cacheManager != null) {
            cacheManager.clearCache();
        }
    }
    
    public static String getCacheStats() {
        if (cacheManager != null) {
            return cacheManager.getStatistics().toString();
        }
        return "Structure Cache: Not initialized";
    }
    
    private static String getStructureName(Holder<Structure> holder) {
        try {
            if (holder.unwrapKey().isPresent()) {
                return holder.unwrapKey().get().location().toString();
            }
        } catch (Exception e) {
        }
        
        try {
            return holder.toString();
        } catch (Exception e) {
            return "unknown_structure";
        }
    }
    
    private static String getBiomeName(ServerLevel level, BlockPos pos) {
        try {
            Holder<Biome> biomeHolder = level.getBiome(pos);
            
            if (biomeHolder.unwrapKey().isPresent()) {
                return biomeHolder.unwrapKey().get().location().toString();
            }
            
            return biomeHolder.value().toString();
        } catch (Exception e) {
            return "unknown_biome";
        }
    }
    
    public static void shutdown() {
        if (cacheManager != null) {
            cacheManager.shutdown();
            cacheManager = null;
        }
    }
}
