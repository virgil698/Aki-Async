package org.virgil.akiasync.network;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.virgil.akiasync.AkiAsyncPlugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class NetworkTrafficMonitor {
    
    private static NetworkTrafficMonitor instance;
    
    private final AkiAsyncPlugin plugin;
    private final Set<UUID> activeViewers = ConcurrentHashMap.newKeySet();
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "AkiAsync-NetworkMonitor");
        t.setDaemon(true);
        return t;
    });
    
    private ScheduledFuture<?> displayTask;
    private ScheduledFuture<?> calculateTask;
    
    private NetworkTrafficMonitor(AkiAsyncPlugin plugin) {
        this.plugin = plugin;
    }
    
    private void start() {
        startCalculateTask();
    }
    
    public static synchronized NetworkTrafficMonitor getInstance(AkiAsyncPlugin plugin) {
        if (instance == null) {
            instance = new NetworkTrafficMonitor(plugin);
            instance.start();
        }
        return instance;
    }
    
    public static synchronized NetworkTrafficMonitor getInstance() {
        return instance;
    }
    
    private void startCalculateTask() {
        calculateTask = scheduler.scheduleAtFixedRate(() -> {
            try {
                if (plugin.getBridge() != null) {
                    plugin.getBridge().calculateNetworkTrafficRates();
                }
            } catch (Exception e) {
                plugin.getLogger().warning("[NetworkMonitor] Calculate task error: " + e.getMessage());
            }
        }, 1, 1, TimeUnit.SECONDS);
    }
    
    public void addViewer(Player player) {
        activeViewers.add(player.getUniqueId());
        ensureDisplayTaskRunning();
    }
    
    public void removeViewer(Player player) {
        activeViewers.remove(player.getUniqueId());
        if (activeViewers.isEmpty()) {
            stopDisplayTask();
        }
    }
    
    public boolean isViewing(Player player) {
        return activeViewers.contains(player.getUniqueId());
    }
    
    private void ensureDisplayTaskRunning() {
        if (displayTask == null || displayTask.isCancelled()) {
            displayTask = scheduler.scheduleAtFixedRate(this::updateDisplay, 0, 500, TimeUnit.MILLISECONDS);
        }
    }
    
    private void stopDisplayTask() {
        if (displayTask != null && !displayTask.isCancelled()) {
            displayTask.cancel(false);
            displayTask = null;
        }
    }
    
    private void updateDisplay() {
        if (activeViewers.isEmpty()) {
            return;
        }
        
        try {
            long inRate = 0;
            long outRate = 0;
            
            if (plugin.getBridge() != null) {
                inRate = plugin.getBridge().getNetworkTrafficInRate();
                outRate = plugin.getBridge().getNetworkTrafficOutRate();
            }
            
            String inRateStr = formatBytes(inRate);
            String outRateStr = formatBytes(outRate);
            
            String message = String.format(
                "<gradient:#FF69B4:#9370DB>↓ %s/s</gradient> <gray>|</gray> <gradient:#9370DB:#FF69B4>↑ %s/s</gradient>",
                inRateStr, outRateStr
            );
            
            Component component = miniMessage.deserialize(message);
            
            for (UUID uuid : activeViewers) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    player.sendActionBar(component);
                } else {
                    activeViewers.remove(uuid);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[NetworkMonitor] Display update error: " + e.getMessage());
        }
    }
    
    private String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }
    
    public void shutdown() {
        if (calculateTask != null) {
            calculateTask.cancel(false);
        }
        stopDisplayTask();
        activeViewers.clear();
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    public long getCurrentInRate() {
        return plugin.getBridge() != null ? plugin.getBridge().getNetworkTrafficInRate() : 0;
    }
    
    public long getCurrentOutRate() {
        return plugin.getBridge() != null ? plugin.getBridge().getNetworkTrafficOutRate() : 0;
    }
    
    public long getTotalBytesIn() {
        return plugin.getBridge() != null ? plugin.getBridge().getNetworkTrafficTotalIn() : 0;
    }
    
    public long getTotalBytesOut() {
        return plugin.getBridge() != null ? plugin.getBridge().getNetworkTrafficTotalOut() : 0;
    }
}
