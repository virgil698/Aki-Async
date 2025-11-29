package org.virgil.akiasync.network;

import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.virgil.akiasync.config.ConfigManager;

import java.util.UUID;
import java.util.logging.Level;

public class PacketSendWorker implements Runnable {

    private final PriorityPacketScheduler scheduler;
    private final ConfigManager config;
    private volatile boolean running = false;
    private Thread workerThread;

    private static final long SLEEP_TIME_MS = 50;

    public PacketSendWorker(PriorityPacketScheduler scheduler, ConfigManager config) {
        this.scheduler = scheduler;
        this.config = config;
    }

    public void start() {
        if (running) {
            return;
        }

        running = true;
        workerThread = new Thread(this, "AkiAsync-PacketSendWorker");
        workerThread.setDaemon(true);
        workerThread.start();

        Bukkit.getLogger().info("[AkiAsync] Packet send worker started");
    }

    public void stop() {
        if (!running) {
            return;
        }

        running = false;
        if (workerThread != null) {
            workerThread.interrupt();
            try {
                workerThread.join(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        Bukkit.getLogger().info("[AkiAsync] Packet send worker stopped");
    }

    @Override
    public void run() {
        while (running) {
            try {

                processAllPlayerQueues();

                Thread.sleep(SLEEP_TIME_MS);

            } catch (InterruptedException e) {

                break;
            } catch (Exception e) {

                Bukkit.getLogger().log(Level.WARNING,
                    "[AkiAsync] Error in packet send worker", e);
            }
        }
    }

    private void processAllPlayerQueues() {

        java.util.Collection<? extends Player> onlinePlayers;
        try {
            onlinePlayers = new java.util.ArrayList<>(Bukkit.getOnlinePlayers());
        } catch (Exception e) {

            return;
        }

        for (Player player : onlinePlayers) {
            try {

                if (player == null || !player.isOnline()) {
                    continue;
                }
                processPlayerQueue(player);
            } catch (Exception e) {
                Bukkit.getLogger().log(Level.WARNING,
                    "[AkiAsync] Error processing queue for player " + player.getName(), e);
            }
        }
    }

    private void processPlayerQueue(Player player) {
        UUID playerId = player.getUniqueId();

        if (scheduler.isQueueEmpty(playerId)) {
            return;
        }

        Connection connection = getConnection(player);
        if (connection == null) {
            return;
        }

        int queueSize = scheduler.getQueueSize(playerId);

        int baseRate = config.getPacketSendRateBase();
        int mediumRate = config.getPacketSendRateMedium();
        int heavyRate = config.getPacketSendRateHeavy();
        int extremeRate = config.getPacketSendRateExtreme();

        int mediumThreshold = config.getAccelerationThresholdMedium();
        int heavyThreshold = config.getAccelerationThresholdHeavy();
        int extremeThreshold = config.getAccelerationThresholdExtreme();

        int sendLimit = baseRate;

        if (queueSize > extremeThreshold) {

            sendLimit = extremeRate;
        } else if (queueSize > heavyThreshold) {

            sendLimit = heavyRate;
        } else if (queueSize > mediumThreshold) {

            sendLimit = mediumRate;
        }

        int packetsSent = 0;
        while (packetsSent < sendLimit && !scheduler.isQueueEmpty(playerId)) {
            PriorityPacketScheduler.PrioritizedPacket prioritizedPacket =
                scheduler.dequeuePacket(playerId);

            if (prioritizedPacket == null) {
                break;
            }

            try {

                sendPacketDirectly(connection, prioritizedPacket.getPacket());
                packetsSent++;

            } catch (Exception e) {
                Bukkit.getLogger().log(Level.WARNING,
                    "[AkiAsync] Error sending packet to " + player.getName(), e);
            }
        }

        if (packetsSent > 0) {
            int remainingSize = scheduler.getQueueSize(playerId);
            if (remainingSize > 100) {
                Bukkit.getLogger().fine(String.format(
                    "[AkiAsync] Sent %d packets to %s, %d remaining in queue",
                    packetsSent, player.getName(), remainingSize
                ));
            }
        }
    }

    private void sendPacketDirectly(Connection connection, net.minecraft.network.protocol.Packet<?> packet) {
        try {

            Class<?> mixinClass = Class.forName("org.virgil.akiasync.mixin.mixins.network.PacketSendMixin");
            java.lang.reflect.Field field = mixinClass.getDeclaredField("SENDING_FROM_WORKER");
            field.setAccessible(true);

            @SuppressWarnings("unchecked")
            ThreadLocal<Boolean> sendingFlag = (ThreadLocal<Boolean>) field.get(null);

            sendingFlag.set(true);
            try {

                connection.send(packet);
            } finally {

                sendingFlag.set(false);
            }
        } catch (Exception e) {

            Bukkit.getLogger().log(Level.WARNING,
                "[AkiAsync] Failed to access ThreadLocal, sending directly", e);
            connection.send(packet);
        }
    }

    private Connection getConnection(Player player) {
        try {
            if (player instanceof CraftPlayer craftPlayer) {
                ServerPlayer nmsPlayer = craftPlayer.getHandle();
                if (nmsPlayer == null || nmsPlayer.connection == null) {
                    return null;
                }
                return nmsPlayer.connection.connection;
            }
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.WARNING,
                "[AkiAsync] Failed to get connection for " + player.getName(), e);
        }
        return null;
    }
}
