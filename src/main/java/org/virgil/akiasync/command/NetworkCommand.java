package org.virgil.akiasync.command;

import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.virgil.akiasync.AkiAsyncPlugin;
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
    
    @Override
    public void execute(CommandSourceStack source, String[] args) {
        if (!(source.getSender() instanceof Player player)) {
            source.getSender().sendMessage("[AkiAsync] This command can only be executed by players");
            return;
        }
        
        if (args.length != 1) {
            source.getSender().sendMessage("[AkiAsync] Usage: /aki-network <true|false>");
            return;
        }
        
        String arg = args[0].toLowerCase(Locale.ROOT);
        boolean enable;
        
        if (arg.equals("true") || arg.equals("on") || arg.equals("enable")) {
            enable = true;
        } else if (arg.equals("false") || arg.equals("off") || arg.equals("disable")) {
            enable = false;
        } else {
            source.getSender().sendMessage("[AkiAsync] Invalid argument. Use 'true' or 'false'");
            return;
        }
        
        NetworkTrafficMonitor monitor = NetworkTrafficMonitor.getInstance(plugin);
        
        if (enable) {
            if (monitor.isViewing(player)) {
                player.sendMessage("[AkiAsync] Network monitor is already enabled");
            } else {
                monitor.addViewer(player);
                player.sendMessage("[AkiAsync] Network monitor enabled");
            }
        } else {
            if (!monitor.isViewing(player)) {
                player.sendMessage("[AkiAsync] Network monitor is already disabled");
            } else {
                monitor.removeViewer(player);
                player.sendMessage("[AkiAsync] Network monitor disabled");
            }
        }
    }
    
    @Override
    public @Nullable String permission() {
        return "akiasync.network";
    }
}
