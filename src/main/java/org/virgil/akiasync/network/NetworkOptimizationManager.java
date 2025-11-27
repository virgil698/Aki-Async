package org.virgil.akiasync.network;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.virgil.akiasync.AkiAsyncPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NetworkOptimizationManager implements Listener {

    private final AkiAsyncPlugin plugin;
    private final Logger logger;
    private final boolean debugEnabled;

    private final NetworkCongestionDetector congestionDetector;
    private final ChunkSendRateController chunkRateController;
    private final PriorityPacketScheduler packetScheduler;
    private final PacketSendWorker packetSendWorker;
    private final Map<UUID, PriorityPacketQueue> playerQueues = new ConcurrentHashMap<>();

    private boolean packetPriorityEnabled;
    private boolean chunkRateControlEnabled;
    private boolean congestionDetectionEnabled;

    private volatile long lastStatsTime = System.currentTimeMillis();
    private static final long STATS_INTERVAL_MS = 60000;

    public NetworkOptimizationManager(AkiAsyncPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.debugEnabled = plugin.getConfigManager().isDebugLoggingEnabled();

        this.congestionDetector = new NetworkCongestionDetector();
        this.chunkRateController = new ChunkSendRateController(congestionDetector);
        this.packetScheduler = new PriorityPacketScheduler(plugin.getConfigManager());
        this.packetSendWorker = new PacketSendWorker(packetScheduler, plugin.getConfigManager());

        loadConfiguration();

        Bukkit.getPluginManager().registerEvents(this, plugin);

        startPeriodicTasks();

        if (packetPriorityEnabled) {
            packetSendWorker.start();
            logger.info("[NetworkOptimization] Packet send worker started");
        }

        logger.info("[NetworkOptimization] Network optimization manager initialized");
        logger.info("  - Packet Priority: " + (packetPriorityEnabled ? "Enabled" : "Disabled"));
        logger.info("  - Chunk Rate Control: " + (chunkRateControlEnabled ? "Enabled" : "Disabled"));
        logger.info("  - Congestion Detection: " + (congestionDetectionEnabled ? "Enabled" : "Disabled"));
    }

    private void loadConfiguration() {
        packetPriorityEnabled = plugin.getConfigManager().isPacketPriorityEnabled();
        chunkRateControlEnabled = plugin.getConfigManager().isChunkRateControlEnabled();
        congestionDetectionEnabled = plugin.getConfigManager().isCongestionDetectionEnabled();

        congestionDetector.setHighPingThreshold(
            plugin.getConfigManager().getHighPingThreshold()
        );
        congestionDetector.setCriticalPingThreshold(
            plugin.getConfigManager().getCriticalPingThreshold()
        );
        congestionDetector.setHighBandwidthThreshold(
            plugin.getConfigManager().getHighBandwidthThreshold()
        );

        chunkRateController.setBaseChunkSendRate(
            plugin.getConfigManager().getBaseChunkSendRate()
        );
        chunkRateController.setMaxChunkSendRate(
            plugin.getConfigManager().getMaxChunkSendRate()
        );
        chunkRateController.setMinChunkSendRate(
            plugin.getConfigManager().getMinChunkSendRate()
        );
    }

    private void startPeriodicTasks() {

        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!congestionDetectionEnabled) return;

            for (Player player : Bukkit.getOnlinePlayers()) {
                congestionDetector.updatePlayerPing(player);
            }
        }, 20L, 20L);

        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!chunkRateControlEnabled) return;

            for (Player player : Bukkit.getOnlinePlayers()) {
                chunkRateController.updatePlayerLocation(player);
            }
        }, 100L, 100L);

        Bukkit.getScheduler().runTaskTimer(plugin, this::logStatistics, 1200L, 1200L);
    }

    private void logStatistics() {
        if (!debugEnabled) return;

        logger.info("========== Network Optimization Statistics ==========");

        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID playerId = player.getUniqueId();
            String playerName = player.getName();

            String networkStats = congestionDetector.getPlayerStatistics(playerId);
            logger.info(String.format("[%s] Network: %s", playerName, networkStats));

            if (chunkRateControlEnabled) {
                String chunkStats = chunkRateController.getPlayerStatistics(playerId);
                logger.info(String.format("[%s] Chunks: %s", playerName, chunkStats));
            }

            if (packetPriorityEnabled) {
                PriorityPacketQueue queue = playerQueues.get(playerId);
                if (queue != null) {
                    logger.info(String.format("[%s] %s", playerName, queue.getStatistics()));
                }
            }
        }

        logger.info("====================================================");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                if (packetPriorityEnabled) {
                    playerQueues.put(playerId, new PriorityPacketQueue(
                        player.getName(), logger, debugEnabled
                    ));
                }

                if (chunkRateControlEnabled) {
                    chunkRateController.updatePlayerLocation(player);
                }

                if (debugEnabled) {
                    logger.info(String.format(
                        "[NetworkOptimization] Player %s joined, network optimization enabled (async)",
                        player.getName()
                    ));
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, 
                    "[NetworkOptimization] Error in async player join handling", e);
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();

        playerQueues.remove(playerId);
        packetScheduler.clearQueue(playerId);
        congestionDetector.removePlayer(playerId);
        chunkRateController.removePlayer(playerId);

        if (debugEnabled) {
            logger.info(String.format(
                "[NetworkOptimization] Player %s quit, data cleaned",
                event.getPlayer().getName()
            ));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!chunkRateControlEnabled) return;

        if (event.getFrom().getYaw() != event.getTo().getYaw() ||
            event.getFrom().getPitch() != event.getTo().getPitch()) {
            chunkRateController.updatePlayerLocation(event.getPlayer());
        }
    }

    public PriorityPacketQueue getPlayerQueue(UUID playerId) {
        return playerQueues.get(playerId);
    }

    public NetworkCongestionDetector getCongestionDetector() {
        return congestionDetector;
    }

    public ChunkSendRateController getChunkRateController() {
        return chunkRateController;
    }

    public void reloadConfiguration() {
        loadConfiguration();
        logger.info("[NetworkOptimization] Configuration reloaded");
    }

    public void shutdown() {

        if (packetPriorityEnabled) {
            packetSendWorker.stop();
            logger.info("[NetworkOptimization] Packet send worker stopped");
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            packetScheduler.clearQueue(player.getUniqueId());
        }

        playerQueues.clear();
        congestionDetector.clear();
        chunkRateController.clear();
        logger.info("[NetworkOptimization] Network optimization manager shut down");
    }

    public boolean isPacketPriorityEnabled() {
        return packetPriorityEnabled;
    }

    public boolean isChunkRateControlEnabled() {
        return chunkRateControlEnabled;
    }

    public boolean isCongestionDetectionEnabled() {
        return congestionDetectionEnabled;
    }

    public PriorityPacketScheduler getPacketScheduler() {
        return packetScheduler;
    }
}
