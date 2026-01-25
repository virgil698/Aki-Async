package org.virgil.akiasync.mixin.mixins.spawn.density;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.virgil.akiasync.mixin.bridge.BridgeManager;
import org.virgil.akiasync.mixin.util.BridgeConfigCache;

import java.util.List;

@Mixin(value = NaturalSpawner.class, priority = 1050)
public class EntityDensityControlMixin {

    @Unique
    private static volatile boolean initialized = false;

    @Unique
    private static volatile boolean densityControlEnabled = false;

    @Unique
    private static volatile int maxEntitiesPerChunk = 80;

    @Inject(
        method = "spawnForChunk",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void checkEntityDensity(
        ServerLevel level,
        LevelChunk chunk,
        NaturalSpawner.SpawnState spawnState,
        List<MobCategory> categories,
        CallbackInfo ci
    ) {
        if (!initialized) {
            akiasync$initDensityControl();
        }

        if (!densityControlEnabled) {
            return;
        }

        net.minecraft.world.phys.AABB chunkBounds = new net.minecraft.world.phys.AABB(
            chunk.getPos().getMinBlockX(), level.getMinY(), chunk.getPos().getMinBlockZ(),
            chunk.getPos().getMaxBlockX() + 1, level.getMaxY(), chunk.getPos().getMaxBlockZ() + 1
        );

        int entityCount = level.getEntities((net.minecraft.world.entity.Entity) null, chunkBounds).size();

        if (entityCount >= maxEntitiesPerChunk) {
            ci.cancel();

            if (BridgeConfigCache.isDebugEnabled()) {
                BridgeConfigCache.debugLog("[EntityDensity] Spawn cancelled in chunk [%d, %d]: %d/%d entities",
                    chunk.getPos().x, chunk.getPos().z, entityCount, maxEntitiesPerChunk);
            }
        }
    }

    @Unique
    private static synchronized void akiasync$initDensityControl() {
        if (initialized) return;

        org.virgil.akiasync.mixin.bridge.Bridge bridge = BridgeManager.getBridge();
        if (bridge != null) {
            densityControlEnabled = bridge.isDensityControlEnabled();
            maxEntitiesPerChunk = bridge.getMaxEntitiesPerChunk();

            BridgeConfigCache.debugLog("[AkiAsync-EntityDensity] Entity density control initialized");
            BridgeConfigCache.debugLog("[AkiAsync-EntityDensity]   Enabled: " + densityControlEnabled);
            BridgeConfigCache.debugLog("[AkiAsync-EntityDensity]   Max entities per chunk: " + maxEntitiesPerChunk);

            initialized = true;
        } else {
            densityControlEnabled = false;
        }
    }
}
