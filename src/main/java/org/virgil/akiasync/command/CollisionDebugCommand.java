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
            sender.sendMessage(ChatColor.RED + "此命令只能由玩家执行");
            return true;
        }
        
        String playerName = player.getName();
        long currentTime = System.currentTimeMillis();
        if (lastCommandTime.containsKey(playerName)) {
            long timeSinceLastUse = currentTime - lastCommandTime.get(playerName);
            if (timeSinceLastUse < COMMAND_COOLDOWN) {
                player.sendMessage(ChatColor.RED + "命令冷却中，请稍后再试");
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
        player.sendMessage(ChatColor.GOLD + "=== 碰撞优化调试命令 ===");
        player.sendMessage(ChatColor.YELLOW + "/collision stats" + ChatColor.WHITE + " - 显示优化统计");
        player.sendMessage(ChatColor.YELLOW + "/collision nearby [radius]" + ChatColor.WHITE + " - 显示附近实体");
        player.sendMessage(ChatColor.YELLOW + "/collision density" + ChatColor.WHITE + " - 显示实体密度图");
        player.sendMessage(ChatColor.YELLOW + "/collision test" + ChatColor.WHITE + " - 运行性能测试");
    }
    
    private void showStats(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== 碰撞优化统计 ===");
        
        double tps = Bukkit.getTPS()[0];
        String tpsColor = tps >= 19.5 ? ChatColor.GREEN.toString() :
                         tps >= 15.0 ? ChatColor.YELLOW.toString() :
                         ChatColor.RED.toString();
        
        player.sendMessage(ChatColor.AQUA + "当前TPS: " + tpsColor + String.format("%.2f", tps));
        
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
        
        player.sendMessage(ChatColor.AQUA + "总实体数: " + ChatColor.WHITE + totalEntities);
        player.sendMessage(ChatColor.AQUA + "生物实体: " + ChatColor.WHITE + livingEntities);
        player.sendMessage(ChatColor.AQUA + "掉落物: " + ChatColor.WHITE + itemEntities);
        
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
        long maxMemory = runtime.maxMemory() / 1024 / 1024;
        
        player.sendMessage(ChatColor.AQUA + "内存使用: " + ChatColor.WHITE + 
            usedMemory + "MB / " + maxMemory + "MB");
    }
    
    private void showNearbyEntities(Player player, String[] args) {
        int radius = 16;
        if (args.length > 1) {
            try {
                radius = Integer.parseInt(args[1]);
                radius = Math.min(radius, 64); 
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "无效的半径值");
                return;
            }
        }
        
        player.sendMessage(ChatColor.GOLD + "=== 附近实体 (半径: " + radius + ") ===");
        
        Map<String, Integer> entityCounts = new HashMap<>();
        int total = 0;
        
        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            String type = entity.getType().name();
            entityCounts.put(type, entityCounts.getOrDefault(type, 0) + 1);
            total++;
        }
        
        player.sendMessage(ChatColor.AQUA + "总计: " + ChatColor.WHITE + total + " 个实体");
        
        entityCounts.entrySet().stream()
            .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
            .limit(10)
            .forEach(entry -> {
                player.sendMessage(ChatColor.YELLOW + entry.getKey() + ": " + 
                    ChatColor.WHITE + entry.getValue());
            });
        
        if (total > 100) {
            player.sendMessage(ChatColor.RED + "⚠ 警告：实体密度过高！");
            player.sendMessage(ChatColor.YELLOW + "建议：启用实体限制或清理多余实体");
        } else if (total > 50) {
            player.sendMessage(ChatColor.YELLOW + "⚠ 注意：实体密度较高");
        }
    }
    
    private void showDensityMap(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== 实体密度图 ===");
        
        int chunkX = player.getLocation().getChunk().getX();
        int chunkZ = player.getLocation().getChunk().getZ();
        
        player.sendMessage(ChatColor.AQUA + "当前区块: " + chunkX + ", " + chunkZ);
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
        player.sendMessage(ChatColor.GOLD + "=== 性能测试 ===");
        player.sendMessage(ChatColor.YELLOW + "正在测试碰撞检测性能...");
        
        long startTime = System.nanoTime();
        int iterations = 1000;
        
        org.bukkit.util.BoundingBox box = player.getBoundingBox();
        for (int i = 0; i < iterations; i++) {
            player.getNearbyEntities(5, 5, 5);
        }
        
        long endTime = System.nanoTime();
        double avgTime = (endTime - startTime) / 1000000.0 / iterations;
        
        player.sendMessage(ChatColor.AQUA + "测试完成！");
        player.sendMessage(ChatColor.AQUA + "迭代次数: " + ChatColor.WHITE + iterations);
        player.sendMessage(ChatColor.AQUA + "平均耗时: " + ChatColor.WHITE + 
            String.format("%.3f", avgTime) + "ms");
        
        String rating;
        String color;
        if (avgTime < 0.1) {
            rating = "优秀";
            color = ChatColor.GREEN.toString();
        } else if (avgTime < 0.5) {
            rating = "良好";
            color = ChatColor.YELLOW.toString();
        } else if (avgTime < 1.0) {
            rating = "一般";
            color = ChatColor.GOLD.toString();
        } else {
            rating = "较差";
            color = ChatColor.RED.toString();
        }
        
        player.sendMessage(ChatColor.AQUA + "性能评级: " + color + rating);
        
        if (avgTime > 0.5) {
            player.sendMessage(ChatColor.RED + "⚠ 建议：检查实体密度或启用更多优化");
        }
    }
}
