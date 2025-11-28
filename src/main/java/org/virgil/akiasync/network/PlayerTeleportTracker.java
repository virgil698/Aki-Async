package org.virgil.akiasync.network;

import net.minecraft.network.protocol.Packet;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

public class PlayerTeleportTracker {

    private final ConcurrentHashMap<UUID, TeleportState> teleportStates = new ConcurrentHashMap<>();
    private final Plugin plugin;
    private final Logger logger;
    private final boolean debugEnabled;
    private final int boostDurationSeconds;
    private final boolean filterNonEssentialPackets;

    private Object cleanupTask = null;
    private boolean isFolia = false;

    private long totalTeleports = 0;
    private long successfulTeleports = 0;
    private long failedTeleports = 0;
    private long totalTeleportDelayNanos = 0;
    private long maxTeleportDelayNanos = 0;
    private long bypassedPackets = 0;
    private long filteredPackets = 0;

    private static class TeleportState {
        private final long startTime;
        private final AtomicBoolean active;
        private boolean completed;
        private long completionTime;

        public TeleportState() {
            this.startTime = System.nanoTime();
            this.active = new AtomicBoolean(true);
            this.completed = false;
            this.completionTime = 0;
        }

        public long getStartTime() {
            return startTime;
        }

        public boolean isActive() {
            return active.get();
        }

        public void setInactive() {
            active.set(false);
        }

        public boolean isExpired(long durationNanos) {
            return (System.nanoTime() - startTime) > durationNanos;
        }

        public void markCompleted() {
            this.completed = true;
            this.completionTime = System.nanoTime();
        }

        public boolean isCompleted() {
            return completed;
        }

        public long getDurationNanos() {
            if (completionTime > 0) {
                return completionTime - startTime;
            }
            return System.nanoTime() - startTime;
        }
    }

    public PlayerTeleportTracker(Plugin plugin, Logger logger, boolean debugEnabled, 
                                 int boostDurationSeconds, boolean filterNonEssentialPackets) {
        this.plugin = plugin;
        this.logger = logger;
        this.debugEnabled = debugEnabled;
        this.boostDurationSeconds = boostDurationSeconds;
        this.filterNonEssentialPackets = filterNonEssentialPackets;

        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            isFolia = true;
        } catch (ClassNotFoundException e) {
            isFolia = false;
        }

        startCleanupTask();
    }

    private void startCleanupTask() {
        long cleanupIntervalTicks = 20L; 

        if (isFolia) {
            
            try {
                Object server = Bukkit.getServer();
                Object globalScheduler = server.getClass().getMethod("getGlobalRegionScheduler").invoke(server);
                java.lang.reflect.Method runAtFixedRateMethod = globalScheduler.getClass().getMethod(
                    "runAtFixedRate",
                    org.bukkit.plugin.Plugin.class,
                    java.util.function.Consumer.class,
                    long.class,
                    long.class
                );

                cleanupTask = runAtFixedRateMethod.invoke(
                    globalScheduler, plugin,
                    (java.util.function.Consumer<Object>) task -> cleanupExpiredStates(),
                    cleanupIntervalTicks, cleanupIntervalTicks
                );

                if (debugEnabled) {
                    logger.info("[TeleportTracker] Using Folia GlobalRegionScheduler for cleanup");
                }
            } catch (Exception e) {
                logger.severe("[TeleportTracker] Failed to start Folia cleanup task: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            
            Bukkit.getScheduler().runTaskTimer(plugin, this::cleanupExpiredStates, 
                cleanupIntervalTicks, cleanupIntervalTicks);
        }
    }

    private void cleanupExpiredStates() {
        long durationNanos = boostDurationSeconds * 1_000_000_000L;
        int cleanedCount = 0;

        for (UUID playerId : teleportStates.keySet()) {
            TeleportState state = teleportStates.get(playerId);
            if (state != null && state.isExpired(durationNanos)) {
                state.setInactive();
                teleportStates.remove(playerId);
                cleanedCount++;

                if (debugEnabled) {
                    logger.info(String.format(
                        "[TeleportTracker] Cleaned expired teleport state for player %s",
                        playerId
                    ));
                }
            }
        }

        if (debugEnabled && cleanedCount > 0) {
            logger.info(String.format(
                "[TeleportTracker] Cleanup completed: removed %d expired states",
                cleanedCount
            ));
        }
    }

    public void markTeleportStart(UUID playerId) {
        if (playerId == null) {
            return;
        }

        TeleportState state = new TeleportState();
        teleportStates.put(playerId, state);
        totalTeleports++;

        if (debugEnabled) {
            logger.info(String.format(
                "[TeleportTracker] Player %s started teleporting (Total: %d)",
                playerId, totalTeleports
            ));
        }
    }

    public boolean isTeleporting(UUID playerId) {
        if (playerId == null) {
            return false;
        }

        TeleportState state = teleportStates.get(playerId);
        if (state == null) {
            return false;
        }

        long durationNanos = boostDurationSeconds * 1_000_000_000L;
        if (state.isExpired(durationNanos)) {
            state.setInactive();
            teleportStates.remove(playerId);
            return false;
        }

        return state.isActive();
    }

    public void markTeleportComplete(UUID playerId) {
        markTeleportComplete(playerId, true);
    }

    public void markTeleportComplete(UUID playerId, boolean success) {
        if (playerId == null) {
            return;
        }

        TeleportState state = teleportStates.get(playerId);
        if (state != null) {
            state.markCompleted();
            long duration = state.getDurationNanos();
            
            if (success) {
                successfulTeleports++;
                totalTeleportDelayNanos += duration;
                if (duration > maxTeleportDelayNanos) {
                    maxTeleportDelayNanos = duration;
                }
            } else {
                failedTeleports++;
            }
            
            state.setInactive();
            teleportStates.remove(playerId);

            if (debugEnabled) {
                double durationMs = duration / 1_000_000.0;
                logger.info(String.format(
                    "[TeleportTracker] Player %s completed teleporting (%s, %.2fms)",
                    playerId, success ? "SUCCESS" : "FAILED", durationMs
                ));
            }
        }
    }

    public long getTeleportStartTime(UUID playerId) {
        if (playerId == null) {
            return 0;
        }

        TeleportState state = teleportStates.get(playerId);
        if (state == null || !state.isActive()) {
            return 0;
        }

        return state.getStartTime();
    }

    public boolean shouldSendDuringTeleport(Packet<?> packet) {
        if (packet == null) {
            return false;
        }

        if (!filterNonEssentialPackets) {
            return true;
        }

        if (TeleportPacketDetector.isEssentialDuringTeleport(packet)) {
            return true;
        }

        if (TeleportPacketDetector.isNonEssentialDuringTeleport(packet)) {
            filteredPackets++;
            return false;
        }

        return true;
    }

    public void recordBypassedPacket() {
        bypassedPackets++;
    }

    public int getActiveTeleportCount() {
        return teleportStates.size();
    }

    public String getStatistics() {
        double successRate = totalTeleports > 0 ? 
            (successfulTeleports * 100.0 / totalTeleports) : 0.0;
        double avgDelayMs = successfulTeleports > 0 ? 
            (totalTeleportDelayNanos / successfulTeleports / 1_000_000.0) : 0.0;
        double maxDelayMs = maxTeleportDelayNanos / 1_000_000.0;
        
        return String.format(
            "Active: %d, Total: %d, Success: %d (%.1f%%), Failed: %d, " +
            "Avg delay: %.2fms, Max delay: %.2fms, Bypassed: %d, Filtered: %d",
            getActiveTeleportCount(),
            totalTeleports,
            successfulTeleports,
            successRate,
            failedTeleports,
            avgDelayMs,
            maxDelayMs,
            bypassedPackets,
            filteredPackets
        );
    }

    public String getDetailedStatistics() {
        double successRate = totalTeleports > 0 ? 
            (successfulTeleports * 100.0 / totalTeleports) : 0.0;
        double avgDelayMs = successfulTeleports > 0 ? 
            (totalTeleportDelayNanos / successfulTeleports / 1_000_000.0) : 0.0;
        double maxDelayMs = maxTeleportDelayNanos / 1_000_000.0;
        
        StringBuilder sb = new StringBuilder();
        sb.append("========== Teleport Tracker Statistics ==========\n");
        sb.append(String.format("Active teleports: %d\n", getActiveTeleportCount()));
        sb.append(String.format("Total teleports: %d\n", totalTeleports));
        sb.append(String.format("Successful: %d (%.1f%%)\n", successfulTeleports, successRate));
        sb.append(String.format("Failed: %d\n", failedTeleports));
        sb.append(String.format("Average delay: %.2fms\n", avgDelayMs));
        sb.append(String.format("Maximum delay: %.2fms\n", maxDelayMs));
        sb.append(String.format("Bypassed packets: %d\n", bypassedPackets));
        sb.append(String.format("Filtered packets: %d\n", filteredPackets));
        sb.append(String.format("Boost duration: %ds\n", boostDurationSeconds));
        sb.append(String.format("Filter enabled: %s\n", filterNonEssentialPackets));
        sb.append("================================================");
        
        return sb.toString();
    }

    public void resetStatistics() {
        totalTeleports = 0;
        successfulTeleports = 0;
        failedTeleports = 0;
        totalTeleportDelayNanos = 0;
        maxTeleportDelayNanos = 0;
        bypassedPackets = 0;
        filteredPackets = 0;
        
        if (debugEnabled) {
            logger.info("[TeleportTracker] Statistics reset");
        }
    }

    public void clear() {
        teleportStates.clear();

        if (debugEnabled) {
            logger.info("[TeleportTracker] All teleport states cleared");
        }
    }

    public void shutdown() {
        
        if (isFolia && cleanupTask != null) {
            try {
                java.lang.reflect.Method cancelMethod = cleanupTask.getClass().getMethod("cancel");
                cancelMethod.invoke(cleanupTask);
                if (debugEnabled) {
                    logger.info("[TeleportTracker] Folia cleanup task cancelled");
                }
            } catch (Exception e) {
                logger.warning("[TeleportTracker] Failed to cancel cleanup task: " + e.getMessage());
            }
        }

        clear();

        if (debugEnabled) {
            logger.info("[TeleportTracker] Teleport tracker shut down");
        }
    }
}
