package org.virgil.akiasync.mixin.mixins.spawning;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.phys.AABB;
import org.virgil.akiasync.mixin.bridge.BridgeManager;
@SuppressWarnings("unused")
@Mixin(NaturalSpawner.class)
public abstract class MobSpawningMixin {
    private static volatile boolean cached_enabled;
    private static volatile boolean cached_densityControlEnabled;
    private static volatile int cached_maxPerChunk;
    private static volatile boolean initialized = false;
    private static volatile boolean isFolia = false;
    @WrapOperation(
        method = "spawnCategoryForPosition",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/NaturalSpawner;isValidSpawnPostitionForType(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/MobCategory;Lnet/minecraft/world/level/StructureManager;Lnet/minecraft/world/level/chunk/ChunkGenerator;Lnet/minecraft/world/level/biome/MobSpawnSettings$SpawnerData;Lnet/minecraft/core/BlockPos$MutableBlockPos;D)Z"
        ),
        require = 0
    )
    private static boolean wrapSpawn1(ServerLevel level, MobCategory category, StructureManager sm, ChunkGenerator cg, MobSpawnSettings.SpawnerData sd, MutableBlockPos pos, double dist, Operation<Boolean> original) {
        if (!initialized) { akiasync$initMobSpawning(); }
        boolean ok = original.call(level, category, sm, cg, sd, pos, dist);
        if (ok && cached_enabled && cached_densityControlEnabled && isChunkOverDensity(level, category, pos)) {
            ok = false;
        }
        return ok;
    }
    @WrapOperation(
        method = "spawnForChunk",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/NaturalSpawner;isValidSpawnPostitionForType(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/MobCategory;Lnet/minecraft/world/level/StructureManager;Lnet/minecraft/world/level/chunk/ChunkGenerator;Lnet/minecraft/world/level/biome/MobSpawnSettings$SpawnerData;Lnet/minecraft/core/BlockPos$MutableBlockPos;D)Z"
        ),
        require = 0
    )
    private static boolean wrapSpawn2(ServerLevel level, MobCategory category, StructureManager sm, ChunkGenerator cg, MobSpawnSettings.SpawnerData sd, MutableBlockPos pos, double dist, Operation<Boolean> original) {
        if (!initialized) { akiasync$initMobSpawning(); }
        boolean ok = original.call(level, category, sm, cg, sd, pos, dist);
        if (ok && cached_enabled && cached_densityControlEnabled && isChunkOverDensity(level, category, pos)) {
            ok = false;
        }
        return ok;
    }
    private static boolean isChunkOverDensity(ServerLevel level, MobCategory category, BlockPos pos) {
        if (isFolia) {
            try {
                org.virgil.akiasync.mixin.bridge.Bridge bridge = BridgeManager.getBridge();
                if (bridge == null || !bridge.canAccessBlockPosDirectly(level, pos)) {
                    return false;
                }
            } catch (Throwable t) {
                return false;
            }
        }
        
        try {
            int cx = (pos.getX() >> 4) << 4;
            int cz = (pos.getZ() >> 4) << 4;
            int minY = level.dimensionType().minY();
            int maxY = minY + level.dimensionType().height();
            AABB box = new AABB(cx, minY, cz, cx + 16, maxY, cz + 16);
            
            int mobCount = level.getEntitiesOfClass(Mob.class, box, m -> m.getType().getCategory() == category).size();
            return cached_maxPerChunk > 0 && mobCount >= cached_maxPerChunk;
        } catch (Throwable ignored) {
            return false;
        }
    }
    private static synchronized void akiasync$initMobSpawning() {
        if (initialized) return;
        
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            isFolia = true;
        } catch (ClassNotFoundException e) {
            isFolia = false;
        }
        
        org.virgil.akiasync.mixin.bridge.Bridge bridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        if (bridge != null) {
            cached_enabled = bridge.isMobSpawningEnabled();
            cached_densityControlEnabled = bridge.isDensityControlEnabled();
            cached_maxPerChunk = bridge.getMaxEntitiesPerChunk();
            
            if (isFolia) {
                bridge.debugLog("[AkiAsync] MobSpawningMixin initialized in Folia mode: enabled=" + cached_enabled + 
                    ", densityControl=" + cached_densityControlEnabled + ", maxPerChunk=" + cached_maxPerChunk + " (with region safety checks)");
            } else {
                bridge.debugLog("[AkiAsync] MobSpawningMixin initialized: enabled=" + cached_enabled + ", densityControl=" + cached_densityControlEnabled + ", maxPerChunk=" + cached_maxPerChunk);
            }
        } else {
            cached_enabled = false;
            cached_densityControlEnabled = true;
            cached_maxPerChunk = 80;
        }
        initialized = true;
    }
}