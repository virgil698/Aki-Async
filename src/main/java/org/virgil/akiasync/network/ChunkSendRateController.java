package org.virgil.akiasync.network;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class ChunkSendRateController {

    private final NetworkCongestionDetector congestionDetector;
    private final Map<UUID, PlayerChunkSendStatus> playerStatus = new ConcurrentHashMap<>();

    private int baseChunkSendRate = 10;
    private int maxChunkSendRate = 20;
    private int minChunkSendRate = 3;
    private int teleportMaxChunkRate = 25;
    private double viewDirectionPriorityWeight = 2.0;
    
    private PlayerTeleportTracker teleportTracker;

    private static class PlayerChunkSendStatus {
        private final UUID playerId;
        private final AtomicInteger currentRate = new AtomicInteger(10);
        private final AtomicLong totalChunksSent = new AtomicLong(0);
        private final AtomicLong priorityChunksSent = new AtomicLong(0);
        private volatile Location lastLocation;
        private volatile Vector lastViewDirection;
        private volatile long lastUpdateTime = System.currentTimeMillis();

        public PlayerChunkSendStatus(UUID playerId) {
            this.playerId = playerId;
        }

        public void updateLocation(Location location) {
            this.lastLocation = location;
            this.lastViewDirection = location.getDirection();
            this.lastUpdateTime = System.currentTimeMillis();
        }

        public void recordChunkSent(boolean isPriority) {
            totalChunksSent.incrementAndGet();
            if (isPriority) {
                priorityChunksSent.incrementAndGet();
            }
        }

        public int getCurrentRate() {
            return currentRate.get();
        }

        public void setCurrentRate(int rate) {
            currentRate.set(rate);
        }

        public Location getLastLocation() {
            return lastLocation;
        }

        public Vector getLastViewDirection() {
            return lastViewDirection;
        }

        public long getTotalChunksSent() {
            return totalChunksSent.get();
        }

        public long getPriorityChunksSent() {
            return priorityChunksSent.get();
        }
    }

    public ChunkSendRateController(NetworkCongestionDetector congestionDetector) {
        this.congestionDetector = congestionDetector;
    }

    public void updatePlayerLocation(Player player) {
        if (player == null) return;

        UUID playerId = player.getUniqueId();
        Location location = player.getLocation();

        playerStatus.computeIfAbsent(playerId, PlayerChunkSendStatus::new)
            .updateLocation(location);
    }

    public int calculateChunkSendRate(Player player) {
        if (player == null) return baseChunkSendRate;

        UUID playerId = player.getUniqueId();
        PlayerChunkSendStatus status = playerStatus.get(playerId);
        if (status == null) return baseChunkSendRate;

        if (teleportTracker != null && teleportTracker.isTeleporting(playerId)) {
            
            int rate = teleportMaxChunkRate;
            status.setCurrentRate(rate);
            return rate;
        }

        NetworkCongestionDetector.CongestionLevel congestion =
            congestionDetector.detectCongestion(player);

        int rate = switch (congestion) {
            case NONE -> maxChunkSendRate;
            case LOW -> baseChunkSendRate;
            case MEDIUM -> (baseChunkSendRate + minChunkSendRate) / 2;
            case HIGH -> minChunkSendRate;
            case SEVERE -> Math.max(1, minChunkSendRate / 2);
        };

        status.setCurrentRate(rate);
        return rate;
    }

    public double calculateChunkPriority(Player player, Chunk chunk) {
        if (player == null || chunk == null) return 0.0;

        PlayerChunkSendStatus status = playerStatus.get(player.getUniqueId());
        if (status == null || status.getLastLocation() == null) return 0.0;

        Location playerLoc = status.getLastLocation();
        Vector viewDirection = status.getLastViewDirection();

        double chunkCenterX = chunk.getX() * 16 + 8;
        double chunkCenterZ = chunk.getZ() * 16 + 8;

        double dx = chunkCenterX - playerLoc.getX();
        double dz = chunkCenterZ - playerLoc.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);

        double distancePriority = 1.0 / (1.0 + distance / 16.0);

        double viewPriority = 1.0;
        if (viewDirection != null) {
            Vector chunkDirection = new Vector(dx, 0, dz).normalize();

            double dotProduct = viewDirection.getX() * chunkDirection.getX() +
                               viewDirection.getZ() * chunkDirection.getZ();

            viewPriority = (dotProduct + 1.0) / 2.0;
        }

        return distancePriority + viewPriority * viewDirectionPriorityWeight;
    }

    public boolean isChunkInViewDirection(Player player, Chunk chunk) {
        if (player == null || chunk == null) return false;

        double priority = calculateChunkPriority(player, chunk);
        return priority > 1.5;
    }

    public void recordChunkSent(UUID playerId, boolean isPriority) {
        PlayerChunkSendStatus status = playerStatus.get(playerId);
        if (status != null) {
            status.recordChunkSent(isPriority);
        }
    }

    public String getPlayerStatistics(UUID playerId) {
        PlayerChunkSendStatus status = playerStatus.get(playerId);
        if (status == null) {
            return "No data";
        }

        return String.format(
            "Rate=%d chunks/tick, TotalSent=%d, PrioritySent=%d (%.1f%%)",
            status.getCurrentRate(),
            status.getTotalChunksSent(),
            status.getPriorityChunksSent(),
            status.getTotalChunksSent() > 0 ?
                (status.getPriorityChunksSent() * 100.0 / status.getTotalChunksSent()) : 0.0
        );
    }

    public void removePlayer(UUID playerId) {
        playerStatus.remove(playerId);
    }

    public void clear() {
        playerStatus.clear();
    }

    public void setBaseChunkSendRate(int rate) {
        this.baseChunkSendRate = rate;
    }

    public void setMaxChunkSendRate(int rate) {
        this.maxChunkSendRate = rate;
    }

    public void setMinChunkSendRate(int rate) {
        this.minChunkSendRate = rate;
    }

    public void setViewDirectionPriorityWeight(double weight) {
        this.viewDirectionPriorityWeight = weight;
    }

    public void setTeleportMaxChunkRate(int rate) {
        this.teleportMaxChunkRate = rate;
    }

    public void setTeleportTracker(PlayerTeleportTracker tracker) {
        this.teleportTracker = tracker;
    }

    public int getBaseChunkSendRate() {
        return baseChunkSendRate;
    }

    public int getMaxChunkSendRate() {
        return maxChunkSendRate;
    }

    public int getMinChunkSendRate() {
        return minChunkSendRate;
    }

    public int getTeleportMaxChunkRate() {
        return teleportMaxChunkRate;
    }
}
