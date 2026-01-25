package org.virgil.akiasync.command;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.virgil.akiasync.AkiAsyncPlugin;
import org.virgil.akiasync.event.ConfigReloadEvent;
import org.virgil.akiasync.language.LanguageManager;

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

    private LanguageManager lang() {
        return plugin.getLanguageManager();
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
        sender.sendMessage(lang().prefixed("command.reload.success"));
    }

    private void sendWarningMessage(CommandSender sender, int timeoutSeconds) {
        sender.sendMessage(Component.empty());
        sender.sendMessage(lang().get("command.reload.warning.title"));
        sender.sendMessage(Component.empty());
        sender.sendMessage(lang().get("command.reload.warning.description"));
        sender.sendMessage(lang().get("command.reload.warning.item-restart-pools"));
        sender.sendMessage(lang().get("command.reload.warning.item-clear-cache"));
        sender.sendMessage(lang().get("command.reload.warning.item-reset-mixin"));
        sender.sendMessage(lang().get("command.reload.warning.item-tps-spike"));
        sender.sendMessage(Component.empty());
        sender.sendMessage(lang().get("command.reload.warning.confirm", "timeout", String.valueOf(timeoutSeconds)));
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
