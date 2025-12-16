package org.virgil.akiasync.listener;

import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldUnloadEvent;
import org.virgil.akiasync.AkiAsyncPlugin;

public class WorldUnloadListener implements Listener {
    
    private final AkiAsyncPlugin plugin;
    
    public WorldUnloadListener(AkiAsyncPlugin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldUnload(WorldUnloadEvent event) {
        try {
            World world = event.getWorld();
            
            if (plugin.getBridge() != null) {
                plugin.getBridge().clearWorldCaches(world.getName());
            }
            
            plugin.getLogger().info("[Memory] Cleaned up caches for world: " + world.getName());
            
        } catch (Exception e) {
            plugin.getLogger().warning("[Memory] Failed to clean up world caches: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
