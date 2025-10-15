package org.virgil.akiasync.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.virgil.akiasync.mixin.metrics.AsyncMetrics;

/**
 * Metrics command for runtime monitoring
 * 
 * Usage: /akiasync metrics
 * 
 * @author Virgil
 */
public class MetricsCommand implements CommandExecutor {
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("metrics")) {
            // Output Prometheus-style metrics
            sender.sendMessage("§a[AkiAsync] Metrics:");
            sender.sendMessage("§7" + AsyncMetrics.getPrometheusMetrics().replace("\n", "\n§7"));
            return true;
        }
        
        return false;
    }
}

