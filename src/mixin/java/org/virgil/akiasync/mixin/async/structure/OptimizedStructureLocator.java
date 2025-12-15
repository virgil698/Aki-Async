package org.virgil.akiasync.mixin.async.structure;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class OptimizedStructureLocator {

    private static final Logger LOGGER = Logger.getLogger("AkiAsync");

    private static volatile StructureCacheManager cacheManager;
    private static volatile String searchPattern = "hybrid";
    private static volatile boolean biomeAwareSearchEnabled = true;

    private static final Map<String, Set<String>> BIOME_STRUCTURE_COMPATIBILITY = new ConcurrentHashMap<>();

    private static final Map<ChunkPos, String> BIOME_CACHE = new ConcurrentHashMap<>();
    private static final int MAX_BIOME_CACHE_SIZE = 1000;
    
    private static final Map<String, Integer> STRUCTURE_RARITY = Map.of(
        "minecraft:village", 32,
        "minecraft:pillager_outpost", 64,
        "minecraft:stronghold", 256,
        "minecraft:woodland_mansion", 512,
        "minecraft:ocean_monument", 128,
        "minecraft:desert_pyramid", 64,
        "minecraft:jungle_pyramid", 64,
        "minecraft:swamp_hut", 64,
        "minecraft:buried_treasure", 32
    );

    static {
        initializeBiomeCompatibility();
    }

    public static synchronized void initialize(Object plugin) {
        if (cacheManager == null) {
            cacheManager = StructureCacheManager.getInstance(plugin);
        }
        updateConfiguration();
    }
    
    public static synchronized void shutdown() {

        BIOME_CACHE.clear();
        
        if (cacheManager != null) {
            cacheManager.shutdown();
            cacheManager = null;
        }
    }
    
    public static void updateConfiguration() {
        org.virgil.akiasync.mixin.bridge.Bridge bridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        if (bridge != null) {
            searchPattern = bridge.getStructureSearchPattern();
            biomeAwareSearchEnabled = bridge.isBiomeAwareSearchEnabled();
        }
    }

    public static Pair<BlockPos, Holder<Structure>> findNearestStructureOptimized(
            ServerLevel level,
            HolderSet<Structure> structures,
            BlockPos startPos,
            int searchRadius,
            boolean skipKnownStructures) {

        if (searchRadius <= 0) {
            searchRadius = 100;
        }

        ChunkGenerator generator = level.getChunkSource().getGenerator();
        ChunkPos startChunk = new ChunkPos(startPos);

        String cacheKey = getCacheKey(structures, startChunk, searchRadius);
        if (cacheManager != null) {

            if (cacheManager.isNegativeCached(cacheKey)) {

                if (!shouldRetryNegativeCache(cacheKey)) {
                    return null;
                }
            }
        }

        int adaptiveRadius = getAdaptiveSearchRadius(structures, searchRadius);

        List<ChunkPos> searchOrder = getSearchOrder(level, startChunk, structures, adaptiveRadius);
        
        if (searchOrder == null || searchOrder.isEmpty()) {
            return null;
        }

        Pair<BlockPos, Holder<Structure>> result = null;
        
        try {
            result = executeThreadSafeSearch(
                level, generator, structures, searchOrder, skipKnownStructures);
        } catch (Exception e) {
            LOGGER.warning("[Structure] Optimized search failed: " + e.getMessage());
        }
        
        if (result == null) {
            try {
                result = generator.findNearestMapStructure(
                    level, structures, startPos, searchRadius, skipKnownStructures);
            } catch (Exception e) {
                LOGGER.warning("[Structure] Vanilla search failed: " + e.getMessage());
            }
        }
        
        if (result == null && searchRadius < 200) {
            try {
                result = generator.findNearestMapStructure(
                    level, structures, startPos, searchRadius * 2, skipKnownStructures);
            } catch (Exception e) {
                LOGGER.warning("[Structure] Extended search failed: " + e.getMessage());
            }
        }

        if (cacheManager != null) {
            if (result == null) {
                cacheManager.cacheNegativeResult(cacheKey);
            }
        }

        return result;
    }
    
    private static boolean shouldRetryNegativeCache(String cacheKey) {

        return Math.random() < 0.1;
    }

    private static List<ChunkPos> getSearchOrder(
            ServerLevel level, ChunkPos center, HolderSet<Structure> structures, int radius) {
        
        List<ChunkPos> baseOrder;
        
        switch (searchPattern.toLowerCase()) {
            case "spiral":
                baseOrder = getSpiralSearchOrder(center, radius);
                break;
            case "layered":
                baseOrder = getLayeredSearchOrder(center, radius);
                break;
            case "hybrid":
            default:
                baseOrder = getHybridSearchOrder(center, radius);
                break;
        }
        
        if (biomeAwareSearchEnabled) {
            return applyBiomeAwareOrdering(level, baseOrder, structures);
        }
        
        return baseOrder;
    }
    
    private static List<ChunkPos> getSpiralSearchOrder(ChunkPos center, int radius) {
        List<ChunkPos> positions = new ArrayList<>();
        positions.add(center);
        
        if (radius <= 0) {
            return positions;
        }

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
    
    private static List<ChunkPos> getLayeredSearchOrder(ChunkPos center, int radius) {
        List<ChunkPos> positions = new ArrayList<>();
        positions.add(center);
        
        if (radius <= 0) {
            return positions;
        }
        
        for (int layer = 1; layer <= radius; layer++) {
            List<ChunkPos> layerPositions = new ArrayList<>();
            
            for (int dx = -layer; dx <= layer; dx++) {
                for (int dz = -layer; dz <= layer; dz++) {

                    if (Math.abs(dx) == layer || Math.abs(dz) == layer) {
                        layerPositions.add(new ChunkPos(center.x + dx, center.z + dz));
                    }
                }
            }
            
            Collections.shuffle(layerPositions);
            positions.addAll(layerPositions);
        }
        
        return positions;
    }
    
    private static List<ChunkPos> getHybridSearchOrder(ChunkPos center, int radius) {
        List<ChunkPos> positions = new ArrayList<>();
        
        if (radius <= 0) {
            positions.add(center);
            return positions;
        }
        
        int spiralRadius = Math.min(radius / 3, 10);
        int layeredStart = spiralRadius + 1;
        
        positions.addAll(getSpiralSearchOrder(center, spiralRadius));
        
        if (radius > spiralRadius) {
            for (int layer = layeredStart; layer <= radius; layer++) {
                List<ChunkPos> layerPositions = new ArrayList<>();
                
                for (int dx = -layer; dx <= layer; dx++) {
                    for (int dz = -layer; dz <= layer; dz++) {
                        if (Math.abs(dx) == layer || Math.abs(dz) == layer) {
                            layerPositions.add(new ChunkPos(center.x + dx, center.z + dz));
                        }
                    }
                }
                
                Collections.shuffle(layerPositions);
                positions.addAll(layerPositions);
            }
        }
        
        return positions;
    }

    private static List<ChunkPos> applyBiomeAwareOrdering(
            ServerLevel level, List<ChunkPos> positions, HolderSet<Structure> structures) {

        Set<String> compatibleBiomes = getCompatibleBiomes(structures);
        if (compatibleBiomes.isEmpty()) {
            return positions;
        }
        
        if (positions.size() <= 1) {
            return positions;
        }

        int sortLimit = Math.min(positions.size(), Math.max(100, positions.size() / 3));
        sortLimit = Math.min(sortLimit, positions.size());
        
        List<ChunkPos> toSort = new ArrayList<>(positions.subList(0, sortLimit));
        List<ChunkPos> rest = sortLimit < positions.size() ? 
            new ArrayList<>(positions.subList(sortLimit, positions.size())) : 
            new ArrayList<>();
        
        toSort.sort((pos1, pos2) -> {
            float score1 = getCachedBiomeCompatibilityScore(level, pos1, compatibleBiomes);
            float score2 = getCachedBiomeCompatibilityScore(level, pos2, compatibleBiomes);
            return Float.compare(score2, score1);
        });
        
        toSort.addAll(rest);
        return toSort;
    }
    
    private static float getCachedBiomeCompatibilityScore(ServerLevel level, ChunkPos chunkPos, Set<String> compatibleBiomes) {

        String cachedBiome = BIOME_CACHE.get(chunkPos);
        if (cachedBiome != null) {
            return compatibleBiomes.contains(cachedBiome) ? 1.0f : 0.1f;
        }
        
        float score = getBiomeCompatibilityScore(level, chunkPos, compatibleBiomes);
        
        if (BIOME_CACHE.size() >= MAX_BIOME_CACHE_SIZE) {
            int toRemove = MAX_BIOME_CACHE_SIZE / 10;
            BIOME_CACHE.entrySet().stream()
                .limit(toRemove)
                .map(Map.Entry::getKey)
                .collect(java.util.stream.Collectors.toList())
                .forEach(BIOME_CACHE::remove);
        }
        
        BlockPos samplePos = chunkPos.getMiddleBlockPosition(64);
        String biomeName = getBiomeName(level, samplePos);
        BIOME_CACHE.put(chunkPos, biomeName);
        
        return score;
    }

    private static Pair<BlockPos, Holder<Structure>> executeThreadSafeSearch(
            ServerLevel level,
            ChunkGenerator generator,
            HolderSet<Structure> structures,
            List<ChunkPos> searchOrder,
            boolean skipKnownStructures) {

        BlockPos startPos = searchOrder.get(0).getMiddleBlockPosition(64);
        
        long startTime = System.currentTimeMillis();
        long timeout = 5000;
        
        try {
            Pair<BlockPos, Holder<Structure>> result = generator.findNearestMapStructure(
                level, structures, startPos, 
                searchOrder.size(), skipKnownStructures
            );
            
            if (System.currentTimeMillis() - startTime > timeout) {
                LOGGER.warning("[Structure] Structure search timeout, using fallback");
                return fallbackStructureSearch(level, structures, searchOrder, skipKnownStructures);
            }
            
            if (result != null && result.getFirst() != null) {

                double distance = startPos.distSqr(result.getFirst());
                if (distance > searchOrder.size() * searchOrder.size() * 256 * 256) {
                    LOGGER.warning("[Structure] Structure too far, result may be invalid");
                    return null;
                }
            }
            
            return result;
            
        } catch (Exception e) {
            LOGGER.warning("[Structure] Structure search exception: " + e.getMessage());

            return fallbackStructureSearch(level, structures, searchOrder, skipKnownStructures);
        }
    }
    
    private static Pair<BlockPos, Holder<Structure>> fallbackStructureSearch(
            ServerLevel level,
            HolderSet<Structure> structures,
            List<ChunkPos> searchOrder,
            boolean skipKnownStructures) {
        
        int checkedChunks = 0;
        int maxChecks = Math.min(searchOrder.size(), 100);
        
        for (ChunkPos chunkPos : searchOrder) {
            if (checkedChunks >= maxChecks) {
                break;
            }
            
            if (!level.getChunkSource().hasChunk(chunkPos.x, chunkPos.z)) {
                continue;
            }
            
            checkedChunks++;
            
            try {

                var chunk = level.getChunkSource().getChunkNow(chunkPos.x, chunkPos.z);
                if (chunk == null) {
                    continue;
                }
                
                for (Holder<Structure> structureHolder : structures) {
                    Structure structure = structureHolder.value();
                    
                    try {
                        StructureStart structureStart = chunk.getStartForStructure(structure);
                        
                        if (structureStart != null && structureStart.isValid()) {
                            if (skipKnownStructures) {
                                continue;
                            }
                            
                            BlockPos structurePos = structureStart.getBoundingBox().getCenter();
                            
                            if (structurePos != null && isValidStructurePosition(structurePos)) {
                                return Pair.of(structurePos, structureHolder);
                            }
                        }
                    } catch (Exception e) {
                        org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                            "OptimizedStructureLocator", "processStructureStart", e);
                    }
                }
            } catch (Exception e) {
                org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                    "OptimizedStructureLocator", "processChunkStructures", e);
            }
        }
        
        return null;
    }
    
    private static boolean isValidStructurePosition(BlockPos pos) {

        if (pos.getY() < -64 || pos.getY() > 320) {
            return false;
        }

        if (Math.abs(pos.getX()) > 30000000 || Math.abs(pos.getZ()) > 30000000) {
            return false;
        }
        return true;
    }
    

    private static int getAdaptiveSearchRadius(HolderSet<Structure> structures, int defaultRadius) {

        if (defaultRadius <= 0) {
            defaultRadius = 100;
        }
        
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
        String biomeName = getBiomeName(level, samplePos);

        return compatibleBiomes.contains(biomeName) ? 1.0f : 0.1f;
    }

    private static Structure getStructureAtPosition(ServerLevel level, BlockPos pos, HolderSet<Structure> structures) {
        ChunkPos chunkPos = new ChunkPos(pos);

        if (!level.getChunkSource().hasChunk(chunkPos.x, chunkPos.z)) {
            return null;
        }

        try {
            var chunk = level.getChunkSource().getChunkNow(chunkPos.x, chunkPos.z);
            if (chunk == null) {
                return null;
            }
            
            for (Holder<Structure> holder : structures) {
                Structure structure = holder.value();
                StructureStart start = chunk.getStartForStructure(structure);

                if (start != null && start.isValid() && start.getBoundingBox().isInside(pos)) {
                    return structure;
                }
            }
        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "OptimizedStructureLocator", "getStructureAtPosition", e);
        }

        return null;
    }

    private static String getCacheKey(HolderSet<Structure> structures, ChunkPos chunkPos, int radius) {
        StringBuilder sb = new StringBuilder();

        for (Holder<Structure> holder : structures) {
            try {
                if (holder.unwrapKey().isPresent()) {
                    sb.append(holder.unwrapKey().get().location()).append(",");
                } else {
                    sb.append(holder.hashCode()).append(",");
                }
            } catch (Exception e) {
                sb.append(holder.hashCode()).append(",");
            }
        }

        int regionX = chunkPos.x / 8;
        int regionZ = chunkPos.z / 8;
        sb.append("r").append(regionX).append(",").append(regionZ);
        sb.append(",rad").append(radius);

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
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "OptimizedStructureLocator", "getStructureName", e);
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
}
