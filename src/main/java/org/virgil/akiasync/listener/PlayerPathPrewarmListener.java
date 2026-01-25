package org.virgil.akiasync.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.virgil.akiasync.AkiAsyncPlugin;

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
            Player player = event.getPlayer();

            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                try {

                    if (plugin.getBridge() != null) {
                        plugin.getBridge().prewarmPlayerPaths(player.getUniqueId());
                    }
                    plugin.getLogger().info("[PathPrewarm] Started prewarming paths for player: " +
                        player.getName());
                } catch (Exception e) {
                    plugin.getLogger().warning("[PathPrewarm] Failed to prewarm paths: " + e.getMessage());
                }
            }, 100L);

        } catch (Exception e) {
            plugin.getLogger().warning("[PathPrewarm] Error on player join: " +
                (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
            if (plugin.getConfigManager().isDebugLoggingEnabled()) {
                e.printStackTrace();
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (!plugin.getConfigManager().isPathPrewarmEnabled()) {
            return;
        }

        try {

            if (plugin.getBridge() != null) {
                plugin.getBridge().cleanupPlayerPaths(event.getPlayer().getUniqueId());
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[PathPrewarm] Error on player quit: " + e.getMessage());
        }
    }
}
