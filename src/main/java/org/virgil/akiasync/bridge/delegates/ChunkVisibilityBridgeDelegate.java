package org.virgil.akiasync.bridge.delegates;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.virgil.akiasync.AkiAsyncPlugin;
import org.virgil.akiasync.config.ConfigManager;

/**
 * Delegate class handling chunk visibility related bridge methods.
 * Extracted from AkiAsyncBridge to reduce its complexity.
 */
public class ChunkVisibilityBridgeDelegate {

    private final AkiAsyncPlugin plugin;
    private ConfigManager config;

    public ChunkVisibilityBridgeDelegate(AkiAsyncPlugin plugin, ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
    }

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "ConfigManager is intentionally shared")
    public void updateConfig(ConfigManager newConfig) {
        this.config = newConfig;
    }

    public boolean isChunkVisibilityFilterEnabled() {
        return config != null && config.isChunkVisibilityFilterEnabled();
    }

    public boolean isChunkVisible(net.minecraft.server.level.ServerPlayer player, net.minecraft.world.level.ChunkPos chunkPos, net.minecraft.server.level.ServerLevel level) {
        if (!isChunkVisibilityFilterEnabled() || player == null || chunkPos == null || level == null) {
            return true;
        }

        try {
            return org.virgil.akiasync.mixin.network.ChunkVisibilityFilter.isChunkVisible(player, chunkPos, level);
        } catch (Exception e) {
            return true;
        }
    }

    public void clearWorldCaches(String worldName) {
        try {
            org.bukkit.World world = plugin.getServer().getWorld(worldName);
            if (world == null) {
                plugin.getLogger().warning("[Bridge] World not found: " + worldName);
                return;
            }

            net.minecraft.server.level.ServerLevel level =
                ((org.bukkit.craftbukkit.CraftWorld) world).getHandle();

            org.virgil.akiasync.mixin.poi.PoiSpatialIndexManager.removeIndex(level);
            org.virgil.akiasync.mixin.poi.BatchPoiManager.clearLevelCache(level);
            org.virgil.akiasync.mixin.util.EntitySliceGridManager.clearSliceGrid(level);
            org.virgil.akiasync.mixin.async.explosion.density.SakuraBlockDensityCache.clearLevelCache(level);
            org.virgil.akiasync.mixin.async.explosion.TNTBatchCollector.clearLevelCache(level);
        } catch (Exception e) {
            plugin.getLogger().warning("[Bridge] Failed to clear world caches: " + e.getMessage());
        }
    }

    public void submitChunkLoad(net.minecraft.server.level.ServerPlayer player, net.minecraft.world.level.ChunkPos chunkPos, int priority, double speed) {
        if (plugin == null || player == null || chunkPos == null) {
            return;
        }

        org.virgil.akiasync.chunk.ChunkLoadPriorityScheduler scheduler = plugin.getChunkLoadScheduler();
        if (scheduler != null) {
            org.bukkit.entity.Player bukkitPlayer = player.getBukkitEntity();
            org.bukkit.World world = player.level().getWorld();
            scheduler.submitChunkLoad(bukkitPlayer, world, chunkPos.x, chunkPos.z, priority, speed);
        }
    }
}
