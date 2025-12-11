package org.virgil.akiasync.listener;

import net.minecraft.server.level.ServerPlayer;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.virgil.akiasync.AkiAsyncPlugin;
import org.virgil.akiasync.mixin.pathfinding.EnhancedPathfindingSystem;

public class PlayerPathPrewarmListener implements Listener {
    
    private final AkiAsyncPlugin plugin;
    
    public PlayerPathPrewarmListener(AkiAsyncPlugin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!plugin.getConfigManager().isPathPrewarmEnabled()) {
            return;
        }
        
        try {
            
            ServerPlayer serverPlayer = ((CraftPlayer) event.getPlayer()).getHandle();
            
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                try {
                    
                    EnhancedPathfindingSystem.prewarmPlayerPathsMainThread(serverPlayer);
                    plugin.getLogger().info("[PathPrewarm] Started prewarming paths for player: " + 
                        event.getPlayer().getName());
                } catch (Exception e) {
                    plugin.getLogger().warning("[PathPrewarm] Failed to prewarm paths: " + e.getMessage());
                }
            }, 40L); 
            
        } catch (Exception e) {
            plugin.getLogger().warning("[PathPrewarm] Error on player join: " + e.getMessage());
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (!plugin.getConfigManager().isPathPrewarmEnabled()) {
            return;
        }
        
        try {
            EnhancedPathfindingSystem.cleanupPlayer(event.getPlayer().getUniqueId());
        } catch (Exception e) {
            plugin.getLogger().warning("[PathPrewarm] Error on player quit: " + e.getMessage());
        }
    }
}
