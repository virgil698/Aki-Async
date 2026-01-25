package org.virgil.akiasync.bridge.delegates;

import org.virgil.akiasync.AkiAsyncPlugin;

/**
 * Delegate class handling executor restart and management methods.
 * Extracted from AkiAsyncBridge to reduce its complexity.
 */
public class ExecutorBridgeDelegate {

    private final AkiAsyncPlugin plugin;

    public ExecutorBridgeDelegate(AkiAsyncPlugin plugin) {
        this.plugin = plugin;
    }

    public void restartVillagerExecutor() {
        try {
            org.virgil.akiasync.mixin.async.villager.VillagerBreedExecutor.restartSmooth();
        } catch (Exception e) {
            plugin.getLogger().warning("[Bridge] Failed to restart villager executor: " + e.getMessage());
        }
    }

    public void restartTNTExecutor() {
        try {
            org.virgil.akiasync.mixin.async.TNTThreadPool.restartSmooth();
        } catch (Exception e) {
            plugin.getLogger().warning("[Bridge] Failed to restart TNT executor: " + e.getMessage());
        }
    }

    public void restartBrainExecutor() {
        // Currently no-op
    }

    public void restartChunkExecutor(int threadCount) {
        try {
            org.virgil.akiasync.mixin.async.chunk.ChunkTickExecutor.setThreadCount(threadCount);
            org.virgil.akiasync.mixin.async.chunk.ChunkTickExecutor.restartSmooth();
        } catch (Exception e) {
            plugin.getLogger().warning("[Bridge] Failed to restart chunk executor: " + e.getMessage());
        }
    }

    public void resetBrainExecutorStatistics() {
        // Currently no-op
    }

    public void resetAsyncMetrics() {
        try {
            org.virgil.akiasync.mixin.metrics.AsyncMetrics.reset();
        } catch (Exception e) {
            plugin.getLogger().warning("[Bridge] Failed to reset async metrics: " + e.getMessage());
        }
    }

    public void prewarmPlayerPaths(java.util.UUID playerId) {
        try {
            org.bukkit.entity.Player player = plugin.getServer().getPlayer(playerId);
            if (player == null || !player.isOnline()) {
                return;
            }

            net.minecraft.server.level.ServerPlayer serverPlayer =
                ((org.bukkit.craftbukkit.entity.CraftPlayer) player).getHandle();

            org.virgil.akiasync.mixin.pathfinding.EnhancedPathfindingSystem.prewarmPlayerPathsMainThread(serverPlayer);
        } catch (Exception e) {
            plugin.getLogger().warning("[Bridge] Failed to prewarm player paths: " +
                (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
        }
    }

    public void cleanupPlayerPaths(java.util.UUID playerId) {
        try {
            org.virgil.akiasync.mixin.pathfinding.EnhancedPathfindingSystem.cleanupPlayer(playerId);
        } catch (Exception e) {
            plugin.getLogger().warning("[Bridge] Failed to cleanup player paths: " + e.getMessage());
        }
    }
}
