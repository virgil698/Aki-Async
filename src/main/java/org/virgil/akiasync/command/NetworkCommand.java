package org.virgil.akiasync.command;

import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.virgil.akiasync.AkiAsyncPlugin;
import org.virgil.akiasync.language.LanguageManager;
import org.virgil.akiasync.network.NetworkTrafficMonitor;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;

import java.util.Locale;

@NullMarked
public class NetworkCommand implements BasicCommand {

    private final AkiAsyncPlugin plugin;

    public NetworkCommand(AkiAsyncPlugin plugin) {
        this.plugin = plugin;
    }

    private LanguageManager lang() {
        return plugin.getLanguageManager();
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        if (!(source.getSender() instanceof Player player)) {
            source.getSender().sendMessage(lang().prefixed("common.player-only"));
            return;
        }

        if (args.length != 1) {
            source.getSender().sendMessage(lang().prefixed("command.network.usage"));
            return;
        }

        String arg = args[0].toLowerCase(Locale.ROOT);
        boolean enable;

        if (arg.equals("true") || arg.equals("on") || arg.equals("enable")) {
            enable = true;
        } else if (arg.equals("false") || arg.equals("off") || arg.equals("disable")) {
            enable = false;
        } else {
            source.getSender().sendMessage(lang().prefixed("command.network.invalid-arg"));
            return;
        }

        NetworkTrafficMonitor monitor = NetworkTrafficMonitor.getInstance(plugin);

        if (enable) {
            if (monitor.isViewing(player)) {
                player.sendMessage(lang().prefixed("command.network.already-enabled"));
            } else {
                monitor.addViewer(player);
                player.sendMessage(lang().prefixed("command.network.enabled"));
            }
        } else {
            if (!monitor.isViewing(player)) {
                player.sendMessage(lang().prefixed("command.network.already-disabled"));
            } else {
                monitor.removeViewer(player);
                player.sendMessage(lang().prefixed("command.network.disabled"));
            }
        }
    }

    @Override
    public @Nullable String permission() {
        return "akiasync.network";
    }
}
