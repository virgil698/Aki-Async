package org.virgil.akiasync.mixin.mixins.spawn.density;

import net.minecraft.core.BlockPos;
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
import java.util.concurrent.ConcurrentHashMap;

@Mixin(value = NaturalSpawner.class, priority = 1100)
public class MobSpawningMixin {

    @Unique
    private static volatile boolean initialized = false;

    @Unique
    private static volatile boolean optimizationEnabled = false;

    @Unique
    private static volatile int spawnInterval = 1;

    @Unique
    private static volatile boolean cacheEnabled = true;

    @Unique
    private static final ConcurrentHashMap<Long, Long> lastSpawnTickCache = new ConcurrentHashMap<>();

    @Unique
    private static volatile int cacheAccessCount = 0;
    @Unique
    private static final int CACHE_CLEANUP_INTERVAL = 1000;
    @Unique
    private static final int MAX_CACHE_SIZE = 5000;

    @Unique
    private static volatile long currentTick = 0;

    @Inject(
        method = "spawnForChunk",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void onSpawnForChunk(
        ServerLevel level,
        LevelChunk chunk,
        NaturalSpawner.SpawnState spawnState,
        List<MobCategory> categories,
        CallbackInfo ci
    ) {
        if (!initialized) {
            akiasync$init();
        }

        if (!optimizationEnabled) {
            return;
        }

        currentTick = level.getGameTime();

        if (spawnInterval > 1) {
            long chunkPos = chunk.getPos().toLong();
            Long lastSpawnTick = lastSpawnTickCache.get(chunkPos);

            if (lastSpawnTick != null) {
                long ticksSinceLastSpawn = currentTick - lastSpawnTick;
                if (ticksSinceLastSpawn < spawnInterval) {

                    ci.cancel();
                    return;
                }
            }

            lastSpawnTickCache.put(chunkPos, currentTick);
        }

        if (cacheEnabled && ++cacheAccessCount >= CACHE_CLEANUP_INTERVAL) {
            cacheAccessCount = 0;
            akiasync$cleanupCache();
        }
    }

    @Unique
    private static void akiasync$cleanupCache() {
        if (lastSpawnTickCache.size() > MAX_CACHE_SIZE) {
            long expirationTicks = 1200;

            lastSpawnTickCache.entrySet().removeIf(entry -> {
                if (entry == null || entry.getValue() == null) {
                    return true;
                }
                return currentTick - entry.getValue() > expirationTicks;
            });

            BridgeConfigCache.debugLog("[MobSpawn] Cache cleaned, size: " + lastSpawnTickCache.size());
        }
    }

    @Unique
    private static synchronized void akiasync$init() {
        if (initialized) return;

        org.virgil.akiasync.mixin.bridge.Bridge bridge = BridgeManager.getBridge();
        if (bridge != null) {
            optimizationEnabled = bridge.isMobSpawningEnabled();
            spawnInterval = bridge.getMobSpawnInterval();

            BridgeConfigCache.debugLog("[AkiAsync-MobSpawn] Mob spawning optimization initialized");
            BridgeConfigCache.debugLog("[AkiAsync-MobSpawn]   Optimization enabled: " + optimizationEnabled);
            BridgeConfigCache.debugLog("[AkiAsync-MobSpawn]   Spawn interval: " + spawnInterval + " ticks");
            BridgeConfigCache.debugLog("[AkiAsync-MobSpawn]   Cache enabled: " + cacheEnabled);
            BridgeConfigCache.debugLog("[AkiAsync-MobSpawn]   Strategy: Interval-based spawn throttling");

            initialized = true;
        } else {
            optimizationEnabled = false;
        }
    }

    @Unique
    private static String akiasync$getCacheStats() {
        return String.format("MobSpawnCache: size=%d/%d, interval=%d",
            lastSpawnTickCache.size(), MAX_CACHE_SIZE, spawnInterval);
    }

    @Unique
    private static void akiasync$clearCache() {
        lastSpawnTickCache.clear();
        BridgeConfigCache.debugLog("[MobSpawn] All caches cleared");
    }
}
