package org.virgil.akiasync.command;
import org.bukkit.Bukkit;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.virgil.akiasync.AkiAsyncPlugin;
import org.virgil.akiasync.event.ConfigReloadEvent;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
@NullMarked
public class DebugCommand implements BasicCommand {
    private final AkiAsyncPlugin plugin;
    public DebugCommand(AkiAsyncPlugin plugin) {
        this.plugin = plugin;
    }
    @Override
    public void execute(CommandSourceStack source, String[] args) {
        if (args.length != 1) {
            source.getSender().sendMessage("[AkiAsync] Usage: /aki-debug <true|false>");
            return;
        }
        String arg = args[0].toLowerCase();
        boolean enableDebug;
        if (arg.equals("true") || arg.equals("on") || arg.equals("enable")) {
            enableDebug = true;
        } else if (arg.equals("false") || arg.equals("off") || arg.equals("disable")) {
            enableDebug = false;
        } else {
            source.getSender().sendMessage("[AkiAsync] Invalid argument. Use 'true' or 'false'");
            return;
        }
        try {
            plugin.getConfigManager().setDebugLoggingEnabled(enableDebug);
            source.getSender().sendMessage("[AkiAsync] Debug logging " + (enableDebug ? "enabled" : "disabled") + " successfully!");
            Bukkit.getPluginManager().callEvent(new ConfigReloadEvent());
            source.getSender().sendMessage("[AkiAsync] Configuration reloaded to apply debug changes.");
        } catch (Exception e) {
            source.getSender().sendMessage("[AkiAsync] Failed to toggle debug logging: " + e.getMessage());
            plugin.getLogger().severe("Error toggling debug logging: " + e.getMessage());
        }
    }
    @Override
    public @Nullable String permission() {
        return "akiasync.debug";
    }
}
