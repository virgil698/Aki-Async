package org.virgil.akiasync.network;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

import net.minecraft.network.protocol.Packet;

public class PriorityPacketQueue {

    private final PriorityBlockingQueue<PacketInfo> queue;
    private final String playerName;
    private final Logger logger;
    private final boolean debugEnabled;

    private final AtomicLong totalPackets = new AtomicLong(0);
    private final AtomicLong criticalPackets = new AtomicLong(0);
    private final AtomicLong highPackets = new AtomicLong(0);
    private final AtomicLong normalPackets = new AtomicLong(0);
    private final AtomicLong lowPackets = new AtomicLong(0);
    private final AtomicLong droppedPackets = new AtomicLong(0);

    private static final int MAX_QUEUE_SIZE = 1000;
    private static final int CRITICAL_QUEUE_LIMIT = 500;
    private static final int HIGH_QUEUE_LIMIT = 300;
    private static final int NORMAL_QUEUE_LIMIT = 150;
    private static final int LOW_QUEUE_LIMIT = 50;

    public PriorityPacketQueue(String playerName, Logger logger, boolean debugEnabled) {
        this.queue = new PriorityBlockingQueue<>(256);
        this.playerName = playerName;
        this.logger = logger;
        this.debugEnabled = debugEnabled;
    }

    public boolean offer(Packet<?> packet, PacketPriority priority) {

        if (queue.size() >= MAX_QUEUE_SIZE) {
            droppedPackets.incrementAndGet();
            if (debugEnabled) {
                logger.warning(String.format(
                    "[PacketQueue] Queue full for player %s, dropping %s packet",
                    playerName, priority.name()
                ));
            }
            return false;
        }

        if (!checkPriorityLimit(priority)) {
            droppedPackets.incrementAndGet();
            if (debugEnabled) {
                logger.warning(String.format(
                    "[PacketQueue] Priority limit reached for %s packets (player: %s)",
                    priority.name(), playerName
                ));
            }
            return false;
        }

        PacketInfo packetInfo = new PacketInfo(packet, priority, playerName);
        boolean added = queue.offer(packetInfo);

        if (added) {
            totalPackets.incrementAndGet();
            switch (priority) {
                case CRITICAL -> criticalPackets.incrementAndGet();
                case HIGH -> highPackets.incrementAndGet();
                case NORMAL -> normalPackets.incrementAndGet();
                case LOW -> lowPackets.incrementAndGet();
            }

            if (debugEnabled && totalPackets.get() % 100 == 0) {
                logger.info(String.format(
                    "[PacketQueue] Player %s: Total=%d, Queue=%d, C=%d, H=%d, N=%d, L=%d",
                    playerName, totalPackets.get(), queue.size(),
                    criticalPackets.get(), highPackets.get(), normalPackets.get(), lowPackets.get()
                ));
            }
        }

        return added;
    }

    private boolean checkPriorityLimit(PacketPriority priority) {
        long count = countByPriority(priority);
        return switch (priority) {
            case CRITICAL -> count < CRITICAL_QUEUE_LIMIT;
            case HIGH -> count < HIGH_QUEUE_LIMIT;
            case NORMAL -> count < NORMAL_QUEUE_LIMIT;
            case LOW -> count < LOW_QUEUE_LIMIT;
        };
    }

    private long countByPriority(PacketPriority priority) {
        return queue.stream()
            .filter(info -> info.getPriority() == priority)
            .count();
    }

    public PacketInfo take() throws InterruptedException {
        return queue.take();
    }

    public PacketInfo poll() {
        return queue.poll();
    }

    public PacketInfo poll(long timeout, TimeUnit unit) throws InterruptedException {
        return queue.poll(timeout, unit);
    }

    public int size() {
        return queue.size();
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public void clear() {
        queue.clear();
    }

    public String getStatistics() {
        return String.format(
            "Queue[%s]: Size=%d, Total=%d, C=%d, H=%d, N=%d, L=%d, Dropped=%d",
            playerName, queue.size(), totalPackets.get(),
            criticalPackets.get(), highPackets.get(), normalPackets.get(), lowPackets.get(),
            droppedPackets.get()
        );
    }

    public void resetStatistics() {
        totalPackets.set(0);
        criticalPackets.set(0);
        highPackets.set(0);
        normalPackets.set(0);
        lowPackets.set(0);
        droppedPackets.set(0);
    }

    public String getPlayerName() {
        return playerName;
    }
}
