package org.virgil.akiasync.bridge.delegates;

import org.virgil.akiasync.AkiAsyncPlugin;

import java.util.HashMap;
import java.util.Map;

/**
 * Delegate class handling cache-related bridge methods.
 * Extracted from AkiAsyncBridge to reduce its complexity.
 */
public class CacheBridgeDelegate {

    private final AkiAsyncPlugin plugin;

    public CacheBridgeDelegate(AkiAsyncPlugin plugin) {
        this.plugin = plugin;
    }

    public void clearSakuraOptimizationCaches() {
        try {
            org.virgil.akiasync.mixin.async.explosion.density.SakuraBlockDensityCache.clearAllCaches();
            org.virgil.akiasync.mixin.async.explosion.density.AsyncDensityCacheManager.shutdown();
        } catch (Exception e) {
            org.virgil.akiasync.util.DebugLogger.error("[AkiAsync] Error clearing Sakura caches: %s", e.getMessage());
        }
    }

    public void clearEntityThrottleCache(int entityId) {
        try {
            // Currently no-op
        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "CacheBridgeDelegate", "clearEntityThrottleCache", e);
        }
    }

    public Map<String, Object> getSakuraCacheStatistics() {
        Map<String, Object> stats = new HashMap<>();

        try {
            Map<String, String> densityStats = new HashMap<>();
            for (org.bukkit.World world : plugin.getServer().getWorlds()) {
                try {
                    net.minecraft.server.level.ServerLevel serverLevel =
                        ((org.bukkit.craftbukkit.CraftWorld) world).getHandle();
                    org.virgil.akiasync.mixin.async.explosion.density.SakuraBlockDensityCache cache =
                        org.virgil.akiasync.mixin.async.explosion.density.SakuraBlockDensityCache.getOrCreate(serverLevel);
                    densityStats.put(world.getName(), cache.getStats());
                } catch (Exception e) {
                    densityStats.put(world.getName(), "Error: " + e.getMessage());
                }
            }
            stats.put("density_cache", densityStats);

            Map<String, String> asyncStats = new HashMap<>();
            for (org.bukkit.World world : plugin.getServer().getWorlds()) {
                try {
                    net.minecraft.server.level.ServerLevel serverLevel =
                        ((org.bukkit.craftbukkit.CraftWorld) world).getHandle();
                    org.virgil.akiasync.mixin.async.explosion.density.AsyncDensityCacheManager manager =
                        org.virgil.akiasync.mixin.async.explosion.density.AsyncDensityCacheManager.getInstance(serverLevel);
                    asyncStats.put(world.getName(), manager.getStats());
                } catch (Exception e) {
                    asyncStats.put(world.getName(), "Error: " + e.getMessage());
                }
            }
            stats.put("async_density_cache", asyncStats);

        } catch (Exception e) {
            org.virgil.akiasync.util.DebugLogger.error("[AkiAsync] Error getting Sakura cache stats: %s", e.getMessage());
        }

        return stats;
    }

    public void performSakuraCacheCleanup() {
        try {
            for (org.bukkit.World world : plugin.getServer().getWorlds()) {
                try {
                    net.minecraft.server.level.ServerLevel serverLevel =
                        ((org.bukkit.craftbukkit.CraftWorld) world).getHandle();

                    org.virgil.akiasync.mixin.async.explosion.density.SakuraBlockDensityCache cache =
                        org.virgil.akiasync.mixin.async.explosion.density.SakuraBlockDensityCache.getOrCreate(serverLevel);
                    cache.expire(serverLevel.getGameTime());

                    org.virgil.akiasync.mixin.async.explosion.density.AsyncDensityCacheManager manager =
                        org.virgil.akiasync.mixin.async.explosion.density.AsyncDensityCacheManager.getInstance(serverLevel);
                    manager.expire(serverLevel.getGameTime());

                } catch (Exception e) {
                    org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                        "CacheBridgeDelegate", "performSakuraCacheCleanup", e);
                }
            }
        } catch (Exception e) {
            org.virgil.akiasync.util.DebugLogger.error("[AkiAsync] Error performing Sakura cache cleanup: %s", e.getMessage());
        }
    }

    public void clearWorldCaches(String worldName) {
        try {
            org.bukkit.World world = plugin.getServer().getWorld(worldName);
            if (world != null) {
                net.minecraft.server.level.ServerLevel serverLevel =
                    ((org.bukkit.craftbukkit.CraftWorld) world).getHandle();

                org.virgil.akiasync.mixin.async.explosion.density.SakuraBlockDensityCache cache =
                    org.virgil.akiasync.mixin.async.explosion.density.SakuraBlockDensityCache.getOrCreate(serverLevel);
                cache.clear();

                plugin.getLogger().info("[AkiAsync] Cleared caches for world: " + worldName);
            }
        } catch (Exception e) {
            org.virgil.akiasync.util.DebugLogger.error("[AkiAsync] Error clearing world caches for %s: %s", worldName, e.getMessage());
        }
    }

    public void clearVillagerBreedCache() {
        try {
            org.virgil.akiasync.mixin.async.villager.VillagerBreedExecutor.clearOldCache(Long.MAX_VALUE);
        } catch (Exception e) {
            plugin.getLogger().warning("[Bridge] Failed to clear villager breed cache: " + e.getMessage());
        }
    }
}
