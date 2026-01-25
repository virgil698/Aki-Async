package org.virgil.akiasync.command;

import org.bukkit.Bukkit;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.virgil.akiasync.AkiAsyncPlugin;
import org.virgil.akiasync.event.ConfigReloadEvent;
import org.virgil.akiasync.language.LanguageManager;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;

import java.util.Locale;

@NullMarked
public class DebugCommand implements BasicCommand {

    private final AkiAsyncPlugin plugin;

    public DebugCommand(AkiAsyncPlugin plugin) {
        this.plugin = plugin;
    }

    private LanguageManager lang() {
        return plugin.getLanguageManager();
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        if (args.length != 1) {
            source.getSender().sendMessage(lang().prefixed("command.debug.usage"));
            return;
        }
        String arg = args[0].toLowerCase(Locale.ROOT);
        boolean enableDebug;
        if (arg.equals("true") || arg.equals("on") || arg.equals("enable")) {
            enableDebug = true;
        } else if (arg.equals("false") || arg.equals("off") || arg.equals("disable")) {
            enableDebug = false;
        } else {
            source.getSender().sendMessage(lang().prefixed("command.debug.invalid-arg"));
            return;
        }
        try {
            plugin.getConfigManager().setDebugLoggingEnabled(enableDebug);
            source.getSender().sendMessage(lang().prefixed(
                enableDebug ? "command.debug.enabled" : "command.debug.disabled"
            ));
            Bukkit.getPluginManager().callEvent(new ConfigReloadEvent());
            source.getSender().sendMessage(lang().prefixed("command.debug.config-reloaded"));
        } catch (Exception e) {
            source.getSender().sendMessage(lang().prefixed("command.debug.failed", "error", e.getMessage()));
            plugin.getLogger().severe("Error toggling debug logging: " + e.getMessage());
        }
    }
    @Override
    public @Nullable String permission() {
        return "akiasync.debug";
    }
}
