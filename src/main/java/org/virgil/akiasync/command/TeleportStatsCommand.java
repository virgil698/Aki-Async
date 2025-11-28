package org.virgil.akiasync.command;

import org.bukkit.command.CommandSender;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.virgil.akiasync.AkiAsyncPlugin;
import org.virgil.akiasync.network.NetworkOptimizationManager;
import org.virgil.akiasync.network.PlayerTeleportTracker;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;

@NullMarked
public class TeleportStatsCommand implements BasicCommand {
    private final AkiAsyncPlugin plugin;

    public TeleportStatsCommand(AkiAsyncPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        CommandSender sender = source.getSender();
        
        NetworkOptimizationManager manager = plugin.getNetworkOptimizationManager();
        if (manager == null) {
            sender.sendMessage("§c[AkiAsync] Network optimization manager not available");
            return;
        }

        if (!manager.isTeleportOptimizationEnabled()) {
            sender.sendMessage("§c[AkiAsync] Teleport optimization is disabled");
            return;
        }

        PlayerTeleportTracker tracker = manager.getTeleportTracker();
        if (tracker == null) {
            sender.sendMessage("§c[AkiAsync] Teleport tracker not available");
            return;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("reset")) {
            tracker.resetStatistics();
            sender.sendMessage("§a[AkiAsync] Teleport statistics reset successfully");
            return;
        }

        String stats = tracker.getDetailedStatistics();
        for (String line : stats.split("\n")) {
            sender.sendMessage("§7" + line);
        }
    }

    @Override
    public @Nullable String permission() {
        return "akiasync.teleportstats";
    }
}
