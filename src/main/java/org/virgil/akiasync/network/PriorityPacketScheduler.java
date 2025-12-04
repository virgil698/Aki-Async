package org.virgil.akiasync.network;

import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.virgil.akiasync.config.ConfigManager;

import java.util.PriorityQueue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PriorityPacketScheduler {

    private final ConfigManager config;

    public PriorityPacketScheduler(ConfigManager config) {
        this.config = config;
    }

    public static class PrioritizedPacket implements Comparable<PrioritizedPacket> {
        private final Packet<?> packet;
        private final PacketPriority priority;
        private final long timestamp;
        private final long sequence;

        private static final java.util.concurrent.atomic.AtomicLong sequenceCounter =
            new java.util.concurrent.atomic.AtomicLong(0);

        public PrioritizedPacket(Packet<?> packet, PacketPriority priority) {
            this.packet = packet;
            this.priority = priority;
            this.timestamp = System.nanoTime();
            this.sequence = sequenceCounter.getAndIncrement();
        }

        public Packet<?> getPacket() {
            return packet;
        }

        public PacketPriority getPriority() {
            return priority;
        }

        public long getTimestamp() {
            return timestamp;
        }

        @Override
        public int compareTo(@NotNull PrioritizedPacket other) {

            int priorityCompare = Integer.compare(
                other.priority.ordinal(),
                this.priority.ordinal()
            );
            if (priorityCompare != 0) {
                return priorityCompare;
            }

            int timeCompare = Long.compare(this.timestamp, other.timestamp);
            if (timeCompare != 0) {
                return timeCompare;
            }

            return Long.compare(this.sequence, other.sequence);
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            PrioritizedPacket other = (PrioritizedPacket) obj;
            return timestamp == other.timestamp && 
                   sequence == other.sequence &&
                   priority == other.priority &&
                   packet == other.packet;
        }
        
        @Override
        public int hashCode() {
            int result = priority.hashCode();
            result = 31 * result + Long.hashCode(timestamp);
            result = 31 * result + Long.hashCode(sequence);
            result = 31 * result + (packet != null ? System.identityHashCode(packet) : 0);
            return result;
        }
    }

    private static class PlayerPacketQueue {
        private final PriorityQueue<PrioritizedPacket> queue;
        private final ConfigManager config;
        private long lastSendTime = 0;

        public PlayerPacketQueue(UUID playerId, ConfigManager config) {

            this.config = config;
            this.queue = new PriorityQueue<>();
        }

        public synchronized boolean enqueue(Packet<?> packet, PacketPriority priority) {

            int maxQueueSize = config.getQueueLimitMaxTotal();
            int criticalMax = config.getQueueLimitMaxCritical();
            int highMax = config.getQueueLimitMaxHigh();
            int normalMax = config.getQueueLimitMaxNormal();

            boolean cleanupEnabled = config.isCleanupEnabled();
            int criticalCleanup = config.getCleanupCriticalCleanup();
            int normalCleanup = config.getCleanupNormalCleanup();
            int staleThreshold = config.getCleanupStaleThreshold();

            int currentSize = queue.size();

            if (currentSize >= maxQueueSize) {
                if (priority == PacketPriority.LOW) {
                    return false;
                }

                if (cleanupEnabled) {
                    if (priority == PacketPriority.CRITICAL || priority == PacketPriority.HIGH) {
                        removeLowestPriorityPackets(criticalCleanup, staleThreshold);
                    } else {
                        removeLowestPriorityPackets(normalCleanup, staleThreshold);
                    }
                }
            }

            if (priority == PacketPriority.CRITICAL && countByPriority(PacketPriority.CRITICAL) >= criticalMax) {

                if (cleanupEnabled) {
                    removeLowestPriorityPackets(normalCleanup, staleThreshold);
                }
                if (queue.size() >= maxQueueSize) {
                    return false;
                }
            }
            if (priority == PacketPriority.HIGH && countByPriority(PacketPriority.HIGH) >= highMax) {
                return false;
            }
            if (priority == PacketPriority.NORMAL && countByPriority(PacketPriority.NORMAL) >= normalMax) {
                return false;
            }

            queue.offer(new PrioritizedPacket(packet, priority));
            return true;
        }

        private int countByPriority(PacketPriority priority) {
            return (int) queue.stream()
                .filter(p -> p.getPriority() == priority)
                .count();
        }

        private void removeLowestPriorityPackets(int count, int staleThresholdSeconds) {

            int removed = 0;

            java.util.Iterator<PrioritizedPacket> iterator = queue.iterator();
            while (iterator.hasNext() && removed < count) {
                PrioritizedPacket packet = iterator.next();
                if (packet.getPriority() == PacketPriority.LOW) {
                    iterator.remove();
                    removed++;
                }
            }

            if (removed < count) {
                long now = System.nanoTime();
                long staleThresholdNano = staleThresholdSeconds * 1_000_000_000L;

                iterator = queue.iterator();
                while (iterator.hasNext() && removed < count) {
                    PrioritizedPacket packet = iterator.next();
                    if (packet.getPriority() == PacketPriority.NORMAL &&
                        (now - packet.getTimestamp()) > staleThresholdNano) {
                        iterator.remove();
                        removed++;
                    }
                }
            }
        }

        @Nullable
        public synchronized PrioritizedPacket dequeue() {
            return queue.poll();
        }

        @Nullable
        public synchronized PrioritizedPacket peek() {
            return queue.peek();
        }

        public synchronized int size() {
            return queue.size();
        }

        public synchronized boolean isEmpty() {
            return queue.isEmpty();
        }

        public synchronized void updateLastSendTime() {
            this.lastSendTime = System.nanoTime();
        }

        public synchronized long getLastSendTime() {
            return this.lastSendTime;
        }
    }

    private final ConcurrentHashMap<UUID, PlayerPacketQueue> playerQueues = new ConcurrentHashMap<>();

    public boolean enqueuePacket(@NotNull ServerPlayer player, @NotNull Packet<?> packet, @NotNull PacketPriority priority) {
        UUID playerId = player.getUUID();
        PlayerPacketQueue queue = playerQueues.computeIfAbsent(playerId, id -> new PlayerPacketQueue(id, config));
        return queue.enqueue(packet, priority);
    }

    @Nullable
    public PrioritizedPacket dequeuePacket(@NotNull UUID playerId) {
        PlayerPacketQueue queue = playerQueues.get(playerId);
        if (queue == null) {
            return null;
        }

        PrioritizedPacket packet = queue.dequeue();
        if (packet != null) {
            queue.updateLastSendTime();
        }

        return packet;
    }

    @Nullable
    public PrioritizedPacket peekPacket(@NotNull UUID playerId) {
        PlayerPacketQueue queue = playerQueues.get(playerId);
        return queue == null ? null : queue.peek();
    }

    public int getQueueSize(@NotNull UUID playerId) {
        PlayerPacketQueue queue = playerQueues.get(playerId);
        return queue == null ? 0 : queue.size();
    }

    public boolean isQueueEmpty(@NotNull UUID playerId) {
        PlayerPacketQueue queue = playerQueues.get(playerId);
        return queue == null || queue.isEmpty();
    }

    public void clearQueue(@NotNull UUID playerId) {
        playerQueues.remove(playerId);
    }

    public long getLastSendTime(@NotNull UUID playerId) {
        PlayerPacketQueue queue = playerQueues.get(playerId);
        return queue == null ? 0 : queue.getLastSendTime();
    }

    public boolean shouldUseQueue(@NotNull Packet<?> packet) {

        String packetName = packet.getClass().getSimpleName();

        if (packetName.contains("Login") || packetName.contains("Configuration")) {
            return false;
        }

        if (packetName.contains("Disconnect")) {
            return false;
        }

        if (packetName.contains("KeepAlive")) {
            return false;
        }

        return true;
    }
}
