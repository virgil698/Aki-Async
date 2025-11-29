package org.virgil.akiasync.network;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class NetworkCongestionDetector {

    private final Map<UUID, PlayerNetworkStatus> playerStatus = new ConcurrentHashMap<>();

    private int highPingThreshold = 200;
    private int criticalPingThreshold = 500;
    private long highBandwidthThreshold = 1024 * 1024;

    private static class PlayerNetworkStatus {
        private final UUID playerId;
        private volatile int currentPing;
        private final AtomicLong totalBytesSent = new AtomicLong(0);
        private final AtomicLong totalPacketsSent = new AtomicLong(0);
        private volatile long lastUpdateTime = System.currentTimeMillis();
        private volatile long lastBytesSent = 0;
        private volatile double currentBandwidth = 0;

        public PlayerNetworkStatus(UUID playerId) {
            this.playerId = playerId;
        }

        public void updatePing(int ping) {
            this.currentPing = ping;
        }

        public void recordPacketSent(int bytes) {
            totalBytesSent.addAndGet(bytes);
            totalPacketsSent.incrementAndGet();

            long now = System.currentTimeMillis();
            long timeDiff = now - lastUpdateTime;
            if (timeDiff >= 1000) {
                long bytesDiff = totalBytesSent.get() - lastBytesSent;
                currentBandwidth = (bytesDiff * 1000.0) / timeDiff;
                lastBytesSent = totalBytesSent.get();
                lastUpdateTime = now;
            }
        }

        public int getCurrentPing() {
            return currentPing;
        }

        public double getCurrentBandwidth() {
            return currentBandwidth;
        }

        public long getTotalBytesSent() {
            return totalBytesSent.get();
        }

        public long getTotalPacketsSent() {
            return totalPacketsSent.get();
        }
    }

    public enum CongestionLevel {
        NONE(0, "无拥塞", "No Congestion"),
        LOW(1, "轻微拥塞", "Low Congestion"),
        MEDIUM(2, "中度拥塞", "Medium Congestion"),
        HIGH(3, "高度拥塞", "High Congestion"),
        SEVERE(4, "严重拥塞", "Severe Congestion");

        private final int level;
        private final String nameCn;
        private final String nameEn;

        CongestionLevel(int level, String nameCn, String nameEn) {
            this.level = level;
            this.nameCn = nameCn;
            this.nameEn = nameEn;
        }

        public int getLevel() {
            return level;
        }

        public String getNameCn() {
            return nameCn;
        }

        public String getNameEn() {
            return nameEn;
        }
    }

    public void updatePlayerPing(Player player) {
        if (player == null) return;

        UUID playerId = player.getUniqueId();
        int ping = player.getPing();

        playerStatus.computeIfAbsent(playerId, PlayerNetworkStatus::new)
            .updatePing(ping);
    }

    public void recordPacketSent(UUID playerId, int bytes) {
        if (playerId == null) return;

        playerStatus.computeIfAbsent(playerId, PlayerNetworkStatus::new)
            .recordPacketSent(bytes);
    }

    public CongestionLevel getCongestionLevel(UUID playerId) {
        if (playerId == null) return CongestionLevel.NONE;

        PlayerNetworkStatus status = playerStatus.get(playerId);
        if (status == null) return CongestionLevel.NONE;

        int ping = status.getCurrentPing();
        double bandwidth = status.getCurrentBandwidth();

        if (ping >= criticalPingThreshold * 2 || bandwidth >= highBandwidthThreshold * 2.0) {
            return CongestionLevel.SEVERE;
        } else if (ping >= criticalPingThreshold || bandwidth >= highBandwidthThreshold * 1.5) {
            return CongestionLevel.HIGH;
        } else if (ping >= highPingThreshold || bandwidth >= highBandwidthThreshold) {
            return CongestionLevel.MEDIUM;
        } else if (ping >= highPingThreshold / 2 || bandwidth >= highBandwidthThreshold / 2) {
            return CongestionLevel.LOW;
        }

        return CongestionLevel.NONE;
    }

    public CongestionLevel detectCongestion(Player player) {
        if (player == null) return CongestionLevel.NONE;

        PlayerNetworkStatus status = playerStatus.get(player.getUniqueId());
        if (status == null) return CongestionLevel.NONE;

        int ping = status.getCurrentPing();
        double bandwidth = status.getCurrentBandwidth();

        if (ping >= criticalPingThreshold * 2 || bandwidth >= highBandwidthThreshold * 2.0) {
            return CongestionLevel.SEVERE;
        } else if (ping >= criticalPingThreshold || bandwidth >= highBandwidthThreshold * 1.5) {
            return CongestionLevel.HIGH;
        } else if (ping >= highPingThreshold || bandwidth >= highBandwidthThreshold) {
            return CongestionLevel.MEDIUM;
        } else if (ping >= highPingThreshold / 2 || bandwidth >= highBandwidthThreshold / 2) {
            return CongestionLevel.LOW;
        }

        return CongestionLevel.NONE;
    }

    public boolean isCongested(Player player) {
        CongestionLevel level = detectCongestion(player);
        return level.getLevel() >= CongestionLevel.MEDIUM.getLevel();
    }

    public String getPlayerStatistics(UUID playerId) {
        PlayerNetworkStatus status = playerStatus.get(playerId);
        if (status == null) {
            return "No data";
        }

        return String.format(
            "Ping=%dms, Bandwidth=%.2fKB/s, TotalSent=%dKB, Packets=%d",
            status.getCurrentPing(),
            status.getCurrentBandwidth() / 1024.0,
            status.getTotalBytesSent() / 1024,
            status.getTotalPacketsSent()
        );
    }

    public void removePlayer(UUID playerId) {
        playerStatus.remove(playerId);
    }

    public void clear() {
        playerStatus.clear();
    }

    public void setHighPingThreshold(int threshold) {
        this.highPingThreshold = threshold;
    }

    public void setCriticalPingThreshold(int threshold) {
        this.criticalPingThreshold = threshold;
    }

    public void setHighBandwidthThreshold(long threshold) {
        this.highBandwidthThreshold = threshold;
    }

    public Map<UUID, PlayerNetworkStatus> getAllPlayerStatus() {
        return new ConcurrentHashMap<>(playerStatus);
    }
}
