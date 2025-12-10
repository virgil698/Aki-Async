package org.virgil.akiasync.listener;

import net.minecraft.server.level.ServerLevel;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldUnloadEvent;
import org.virgil.akiasync.AkiAsyncPlugin;

/**
 * 世界卸载监听器
 * 
 * 清理与世界相关的静态缓存，防止内存泄漏
 * 
 * @author AkiAsync
 */
public class WorldUnloadListener implements Listener {
    
    private final AkiAsyncPlugin plugin;
    
    public WorldUnloadListener(AkiAsyncPlugin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldUnload(WorldUnloadEvent event) {
        try {
            
            ServerLevel level = ((CraftWorld) event.getWorld()).getHandle();
            
            org.virgil.akiasync.mixin.brain.core.AiSpatialIndexManager.removeIndex(level);
            
            org.virgil.akiasync.mixin.poi.PoiSpatialIndexManager.removeIndex(level);
            org.virgil.akiasync.mixin.poi.BatchPoiManager.clearLevelCache(level);
            
            org.virgil.akiasync.mixin.util.EntitySliceGridManager.clearSliceGrid(level);
            
            org.virgil.akiasync.mixin.async.explosion.density.SakuraBlockDensityCache.clearLevelCache(level);
            
            org.virgil.akiasync.mixin.async.redstone.RedstoneNetworkCache.clearLevelCache(level);
            org.virgil.akiasync.mixin.async.redstone.RedstoneWireHelper.clearLevelCache(level);
            org.virgil.akiasync.mixin.async.redstone.AsyncRedstoneNetworkManager.clearLevelCache(level);
            org.virgil.akiasync.mixin.async.redstone.PandaWireEvaluator.clearLevelCache(level);
            
            org.virgil.akiasync.mixin.async.explosion.TNTBatchCollector.clearLevelCache(level);
            
            org.virgil.akiasync.mixin.lighting.LightingOptimizationManager.clearLevelCache(level);
            
            plugin.getLogger().info("[Memory] Cleaned up caches for world: " + event.getWorld().getName());
            
        } catch (Exception e) {
            plugin.getLogger().warning("[Memory] Failed to clean up world caches: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
