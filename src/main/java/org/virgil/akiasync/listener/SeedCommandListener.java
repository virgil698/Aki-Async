package org.virgil.akiasync.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;
import org.virgil.akiasync.AkiAsyncPlugin;

import java.util.Locale;

public class SeedCommandListener implements Listener {

    private final AkiAsyncPlugin plugin;

    public SeedCommandListener(AkiAsyncPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage().toLowerCase(Locale.ROOT).trim();

        if (!isSeedCommand(message)) {
            return;
        }

        if (!isEnabled()) {
            return;
        }

        if (!player.isOp()) {
            event.setCancelled(true);

            String denyMessage = getDenyMessage();
            player.sendMessage(ChatColor.RED + denyMessage);

            plugin.getLogger().warning(String.format(
                "Player %s attempted to use /seed command without OP permission",
                player.getName()
            ));
        }
    }

    private boolean isSeedCommand(String command) {

        if (command.startsWith("/")) {
            command = command.substring(1);
        }

        return command.equals("seed") ||
               command.startsWith("seed ") ||
               command.equals("minecraft:seed") ||
               command.startsWith("minecraft:seed ");
    }

    private boolean isEnabled() {
        try {
            return plugin.getConfig().getBoolean("seed-encryption.restrict-seed-command", true);
        } catch (Exception e) {

            return true;
        }
    }

    private String getDenyMessage() {
        try {
            String message = plugin.getConfig().getString(
                "seed-encryption.seed-command-deny-message",
                "You don't have permission to use this command. Only server operators can view the world seed."
            );
            if (message == null) {
                message = "You don't have permission to use this command. Only server operators can view the world seed.";
            }
            return ChatColor.translateAlternateColorCodes('&', message);
        } catch (Exception e) {
            return "You don't have permission to use this command. Only server operators can view the world seed.";
        }
    }
}
