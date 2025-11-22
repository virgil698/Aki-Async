package org.virgil.akiasync.command;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.virgil.akiasync.AkiAsyncPlugin;
import org.virgil.akiasync.event.ConfigReloadEvent;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@NullMarked
public class ReloadCommand implements BasicCommand {
    private final AkiAsyncPlugin plugin;
    private final Map<UUID, Long> confirmationMap = new ConcurrentHashMap<>();
    
    public ReloadCommand(AkiAsyncPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public void execute(CommandSourceStack source, String[] args) {
        CommandSender sender = source.getSender();
        FileConfiguration config = plugin.getConfig();
        
        boolean requireConfirmation = config.getBoolean("commands.reload.require-confirmation", true);
        
        if (!requireConfirmation) {
            performReload(sender);
            return;
        }
        
        int timeoutSeconds = config.getInt("commands.reload.confirmation-timeout", 30);
        long timeoutMillis = timeoutSeconds * 1000L;
        
        UUID senderId = getSenderId(sender);
        
        Long lastConfirmTime = confirmationMap.get(senderId);
        long currentTime = System.currentTimeMillis();
        
        if (lastConfirmTime != null && (currentTime - lastConfirmTime) < timeoutMillis) {
            confirmationMap.remove(senderId);
            performReload(sender);
        } else {
            confirmationMap.put(senderId, currentTime);
            sendWarningMessage(sender, timeoutSeconds);
        }
    }
    
    private void performReload(CommandSender sender) {
        Bukkit.getPluginManager().callEvent(new ConfigReloadEvent());
        sender.sendMessage(Component.text("[AkiAsync] ", NamedTextColor.GOLD)
            .append(Component.text("Configuration hot-reloaded, thread pools smoothly restarted.", NamedTextColor.GREEN)));
    }
    
    private void sendWarningMessage(CommandSender sender, int timeoutSeconds) {
        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("⚠ ", NamedTextColor.YELLOW, TextDecoration.BOLD)
            .append(Component.text("[AkiAsync] RELOAD WARNING", NamedTextColor.RED, TextDecoration.BOLD))
            .append(Component.text(" ⚠", NamedTextColor.YELLOW, TextDecoration.BOLD)));
        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("⚠ ", NamedTextColor.YELLOW)
            .append(Component.text("Reloading AkiAsync will:", NamedTextColor.GOLD)));
        sender.sendMessage(Component.text("  • ", NamedTextColor.GRAY)
            .append(Component.text("Restart all async thread pools", NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("  • ", NamedTextColor.GRAY)
            .append(Component.text("Clear all internal caches", NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("  • ", NamedTextColor.GRAY)
            .append(Component.text("Reset all Mixin states", NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("  • ", NamedTextColor.GRAY)
            .append(Component.text("May cause temporary TPS spike", NamedTextColor.YELLOW)));
        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("⚠ ", NamedTextColor.YELLOW)
            .append(Component.text("Execute the command again within ", NamedTextColor.GOLD))
            .append(Component.text(timeoutSeconds + " seconds", NamedTextColor.RED, TextDecoration.BOLD))
            .append(Component.text(" to confirm.", NamedTextColor.GOLD)));
        sender.sendMessage(Component.empty());
    }
    
    private UUID getSenderId(CommandSender sender) {
        if (sender instanceof org.bukkit.entity.Player player) {
            return player.getUniqueId();
        }
        return UUID.fromString("00000000-0000-0000-0000-000000000000");
    }
    
    @Override
    public @Nullable String permission() {
        return "akiasync.reload";
    }
}