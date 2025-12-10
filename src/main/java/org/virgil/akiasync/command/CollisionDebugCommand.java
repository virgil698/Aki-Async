package org.virgil.akiasync.command;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class CollisionDebugCommand implements CommandExecutor {
    
    private static final Map<String, Long> lastCommandTime = new HashMap<>();
    private static final long COMMAND_COOLDOWN = 1000; 
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, 
                           @NotNull String label, @NotNull String[] args) {
        
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be executed by players");
            return true;
        }
        
        String playerName = player.getName();
        long currentTime = System.currentTimeMillis();
        if (lastCommandTime.containsKey(playerName)) {
            long timeSinceLastUse = currentTime - lastCommandTime.get(playerName);
            if (timeSinceLastUse < COMMAND_COOLDOWN) {
                player.sendMessage(ChatColor.RED + "Command on cooldown, please try again later");
                return true;
            }
        }
        lastCommandTime.put(playerName, currentTime);
        
        if (args.length == 0) {
            showHelp(player);
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "stats" -> showStats(player);
            case "nearby" -> showNearbyEntities(player, args);
            case "density" -> showDensityMap(player);
            case "test" -> runPerformanceTest(player);
            default -> showHelp(player);
        }
        
        return true;
    }
    
    private void showHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== Collision Optimization Debug Commands ===");
        player.sendMessage(ChatColor.YELLOW + "/collision stats" + ChatColor.WHITE + " - Show optimization statistics");
        player.sendMessage(ChatColor.YELLOW + "/collision nearby [radius]" + ChatColor.WHITE + " - Show nearby entities");
        player.sendMessage(ChatColor.YELLOW + "/collision density" + ChatColor.WHITE + " - Show entity density map");
        player.sendMessage(ChatColor.YELLOW + "/collision test" + ChatColor.WHITE + " - Run performance test");
    }
    
    private void showStats(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== Collision Optimization Statistics ===");
        
        double tps = Bukkit.getTPS()[0];
        String tpsColor = tps >= 19.5 ? ChatColor.GREEN.toString() :
                         tps >= 15.0 ? ChatColor.YELLOW.toString() :
                         ChatColor.RED.toString();
        
        player.sendMessage(ChatColor.AQUA + "Current TPS: " + tpsColor + String.format("%.2f", tps));
        
        int totalEntities = 0;
        int livingEntities = 0;
        int itemEntities = 0;
        
        for (org.bukkit.World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                totalEntities++;
                if (entity instanceof org.bukkit.entity.LivingEntity) {
                    livingEntities++;
                } else if (entity instanceof org.bukkit.entity.Item) {
                    itemEntities++;
                }
            }
        }
        
        player.sendMessage(ChatColor.AQUA + "Total Entities: " + ChatColor.WHITE + totalEntities);
        player.sendMessage(ChatColor.AQUA + "Living Entities: " + ChatColor.WHITE + livingEntities);
        player.sendMessage(ChatColor.AQUA + "Item Entities: " + ChatColor.WHITE + itemEntities);
        
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
        long maxMemory = runtime.maxMemory() / 1024 / 1024;
        
        player.sendMessage(ChatColor.AQUA + "Memory Usage: " + ChatColor.WHITE + 
            usedMemory + "MB / " + maxMemory + "MB");
    }
    
    private void showNearbyEntities(Player player, String[] args) {
        int radius = 16;
        if (args.length > 1) {
            try {
                radius = Integer.parseInt(args[1]);
                radius = Math.min(radius, 64); 
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Invalid radius value");
                return;
            }
        }
        
        player.sendMessage(ChatColor.GOLD + "=== Nearby Entities (Radius: " + radius + ") ===");
        
        Map<String, Integer> entityCounts = new HashMap<>();
        int total = 0;
        
        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            String type = entity.getType().name();
            entityCounts.put(type, entityCounts.getOrDefault(type, 0) + 1);
            total++;
        }
        
        player.sendMessage(ChatColor.AQUA + "Total: " + ChatColor.WHITE + total + " entities");
        
        entityCounts.entrySet().stream()
            .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
            .limit(10)
            .forEach(entry -> {
                player.sendMessage(ChatColor.YELLOW + entry.getKey() + ": " + 
                    ChatColor.WHITE + entry.getValue());
            });
        
        if (total > 100) {
            player.sendMessage(ChatColor.RED + "⚠ Warning: Entity density is too high!");
            player.sendMessage(ChatColor.YELLOW + "Suggestion: Enable entity limits or clear excess entities");
        } else if (total > 50) {
            player.sendMessage(ChatColor.YELLOW + "⚠ Notice: Entity density is high");
        }
    }
    
    private void showDensityMap(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== Entity Density Map ===");
        
        int chunkX = player.getLocation().getChunk().getX();
        int chunkZ = player.getLocation().getChunk().getZ();
        
        player.sendMessage(ChatColor.AQUA + "Current Chunk: " + chunkX + ", " + chunkZ);
        player.sendMessage("");
        
        for (int dz = -1; dz <= 1; dz++) {
            StringBuilder line = new StringBuilder();
            for (int dx = -1; dx <= 1; dx++) {
                org.bukkit.Chunk chunk = player.getWorld().getChunkAt(chunkX + dx, chunkZ + dz);
                int entityCount = chunk.getEntities().length;
                
                String color;
                String symbol;
                if (entityCount > 100) {
                    color = ChatColor.RED.toString();
                    symbol = "█";
                } else if (entityCount > 50) {
                    color = ChatColor.YELLOW.toString();
                    symbol = "▓";
                } else if (entityCount > 20) {
                    color = ChatColor.GREEN.toString();
                    symbol = "▒";
                } else {
                    color = ChatColor.GRAY.toString();
                    symbol = "░";
                }
                
                line.append(color).append(symbol).append(symbol).append(" ");
            }
            player.sendMessage(line.toString());
        }
        
        player.sendMessage("");
        player.sendMessage(ChatColor.GRAY + "░░ " + ChatColor.WHITE + "< 20  " +
                          ChatColor.GREEN + "▒▒ " + ChatColor.WHITE + "20-50  " +
                          ChatColor.YELLOW + "▓▓ " + ChatColor.WHITE + "50-100  " +
                          ChatColor.RED + "██ " + ChatColor.WHITE + "> 100");
    }
    
    private void runPerformanceTest(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== Performance Test ===");
        player.sendMessage(ChatColor.YELLOW + "Testing collision detection performance...");
        
        long startTime = System.nanoTime();
        int iterations = 1000;
        
        org.bukkit.util.BoundingBox box = player.getBoundingBox();
        for (int i = 0; i < iterations; i++) {
            player.getNearbyEntities(5, 5, 5);
        }
        
        long endTime = System.nanoTime();
        double avgTime = (endTime - startTime) / 1000000.0 / iterations;
        
        player.sendMessage(ChatColor.AQUA + "Test completed!");
        player.sendMessage(ChatColor.AQUA + "Iterations: " + ChatColor.WHITE + iterations);
        player.sendMessage(ChatColor.AQUA + "Average Time: " + ChatColor.WHITE + 
            String.format("%.3f", avgTime) + "ms");
        
        String rating;
        String color;
        if (avgTime < 0.1) {
            rating = "Excellent";
            color = ChatColor.GREEN.toString();
        } else if (avgTime < 0.5) {
            rating = "Good";
            color = ChatColor.YELLOW.toString();
        } else if (avgTime < 1.0) {
            rating = "Fair";
            color = ChatColor.GOLD.toString();
        } else {
            rating = "Poor";
            color = ChatColor.RED.toString();
        }
        
        player.sendMessage(ChatColor.AQUA + "Performance Rating: " + color + rating);
        
        if (avgTime > 0.5) {
            player.sendMessage(ChatColor.RED + "⚠ Suggestion: Check entity density or enable more optimizations");
        }
    }
}
