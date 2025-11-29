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
    private final PlayerTeleportTracker teleportTracker;
    private final ViewFrustumPacketFilter viewFrustumFilter;
    private final Map<UUID, PriorityPacketQueue> playerQueues = new ConcurrentHashMap<>();

    private boolean packetPriorityEnabled;
    private boolean chunkRateControlEnabled;
    private boolean congestionDetectionEnabled;
    private boolean teleportOptimizationEnabled;
    private boolean viewFrustumFilterEnabled;
    
    private boolean isFolia = false;
    private Object pingUpdateTask = null;
    private Object chunkUpdateTask = null;
    private Object statsTask = null;

    public NetworkOptimizationManager(AkiAsyncPlugin plugin) {
        
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            isFolia = true;
        } catch (ClassNotFoundException e) {
            isFolia = false;
        }
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.debugEnabled = plugin.getConfigManager().isDebugLoggingEnabled();

        this.congestionDetector = new NetworkCongestionDetector();
        this.chunkRateController = new ChunkSendRateController(congestionDetector);
        this.packetScheduler = new PriorityPacketScheduler(plugin.getConfigManager());
        this.packetSendWorker = new PacketSendWorker(packetScheduler, plugin.getConfigManager());
        this.viewFrustumFilter = new ViewFrustumPacketFilter();

        loadConfiguration();

        this.teleportTracker = new PlayerTeleportTracker(
            plugin,
            logger,
            debugEnabled,
            plugin.getConfigManager().getTeleportBoostDurationSeconds(),
            plugin.getConfigManager().isTeleportFilterNonEssentialPackets()
        );

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
        logger.info("  - Teleport Optimization: " + (teleportOptimizationEnabled ? "Enabled" : "Disabled"));
        logger.info("  - View Frustum Filter: " + (viewFrustumFilterEnabled ? "Enabled" : "Disabled"));
    }

    private void loadConfiguration() {
        packetPriorityEnabled = plugin.getConfigManager().isPacketPriorityEnabled();
        chunkRateControlEnabled = plugin.getConfigManager().isChunkRateControlEnabled();
        congestionDetectionEnabled = plugin.getConfigManager().isCongestionDetectionEnabled();
        teleportOptimizationEnabled = plugin.getConfigManager().isTeleportOptimizationEnabled();
        
        try {
            viewFrustumFilterEnabled = plugin.getConfigManager().isViewFrustumFilterEnabled();
            viewFrustumFilter.setEnabled(viewFrustumFilterEnabled);
            viewFrustumFilter.setFilterEntities(plugin.getConfigManager().isViewFrustumFilterEntities());
            viewFrustumFilter.setFilterBlocks(plugin.getConfigManager().isViewFrustumFilterBlocks());
            viewFrustumFilter.setFilterParticles(plugin.getConfigManager().isViewFrustumFilterParticles());
            viewFrustumFilter.setDebugEnabled(plugin.getConfigManager().isDebugLoggingEnabled());
        } catch (NoSuchMethodError e) {
            
            viewFrustumFilterEnabled = false;
            viewFrustumFilter.setEnabled(false);
        }

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
        chunkRateController.setTeleportMaxChunkRate(
            plugin.getConfigManager().getTeleportMaxChunkRate()
        );
        
        if (teleportTracker != null) {
            chunkRateController.setTeleportTracker(teleportTracker);
        }
    }

    private void startPeriodicTasks() {
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
                
                pingUpdateTask = runAtFixedRateMethod.invoke(
                    globalScheduler, plugin,
                    (java.util.function.Consumer<Object>) task -> {
                        if (!congestionDetectionEnabled) return;
                        for (Player player : Bukkit.getOnlinePlayers()) {
                            congestionDetector.updatePlayerPing(player);
                        }
                    },
                    20L, 20L
                );
                
                chunkUpdateTask = runAtFixedRateMethod.invoke(
                    globalScheduler, plugin,
                    (java.util.function.Consumer<Object>) task -> {
                        if (!chunkRateControlEnabled) return;
                        for (Player player : Bukkit.getOnlinePlayers()) {
                            chunkRateController.updatePlayerLocation(player);
                        }
                    },
                    100L, 100L
                );
                
                statsTask = runAtFixedRateMethod.invoke(
                    globalScheduler, plugin,
                    (java.util.function.Consumer<Object>) task -> logStatistics(),
                    1200L, 1200L
                );
                
                logger.info("[NetworkOptimization] Using Folia GlobalRegionScheduler");
            } catch (Exception e) {
                logger.severe("[NetworkOptimization] Failed to start Folia tasks: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            
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
    }

    private void logStatistics() {
        if (!debugEnabled) return;

        logger.info("========== Network Optimization Statistics ==========");

        if (teleportOptimizationEnabled && teleportTracker != null) {
            logger.info(String.format("[Teleport] %s", teleportTracker.getStatistics()));
        }

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
                    "[NetworkOptimization] Player %s joined, network optimization enabled",
                    player.getName()
                ));
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, 
                "[NetworkOptimization] Error in player join handling", e);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();

        playerQueues.remove(playerId);
        packetScheduler.clearQueue(playerId);
        congestionDetector.removePlayer(playerId);
        chunkRateController.removePlayer(playerId);
        viewFrustumFilter.removePlayer(playerId);
        
        if (teleportTracker != null) {
            teleportTracker.markTeleportComplete(playerId);
        }

        if (debugEnabled) {
            logger.info(String.format(
                "[NetworkOptimization] Player %s quit, data cleaned",
                event.getPlayer().getName()
            ));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        boolean viewChanged = event.getFrom().getYaw() != event.getTo().getYaw() ||
                             event.getFrom().getPitch() != event.getTo().getPitch();
        
        if (chunkRateControlEnabled && viewChanged) {
            chunkRateController.updatePlayerLocation(event.getPlayer());
        }
        
        if (viewFrustumFilterEnabled && viewChanged) {
            try {
                net.minecraft.server.level.ServerPlayer nmsPlayer = 
                    ((org.bukkit.craftbukkit.entity.CraftPlayer) event.getPlayer()).getHandle();
                viewFrustumFilter.updatePlayerView(nmsPlayer);
            } catch (Exception e) {
                
            }
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

    public PlayerTeleportTracker getTeleportTracker() {
        return teleportTracker;
    }

    public void reloadConfiguration() {
        loadConfiguration();
        logger.info("[NetworkOptimization] Configuration reloaded");
    }

    public void shutdown() {
        
        if (isFolia) {
            try {
                if (pingUpdateTask != null) {
                    cancelFoliaTask(pingUpdateTask, "PingUpdate");
                }
                if (chunkUpdateTask != null) {
                    cancelFoliaTask(chunkUpdateTask, "ChunkUpdate");
                }
                if (statsTask != null) {
                    cancelFoliaTask(statsTask, "Stats");
                }
            } catch (Exception e) {
                logger.warning("[NetworkOptimization] Failed to cancel Folia tasks: " + e.getMessage());
            }
        }

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
        viewFrustumFilter.clear();
        
        if (teleportTracker != null) {
            teleportTracker.shutdown();
        }
        
        logger.info("[NetworkOptimization] Network optimization manager shut down");
    }
    
    public ViewFrustumPacketFilter getViewFrustumFilter() {
        return viewFrustumFilter;
    }
    
    public boolean isViewFrustumFilterEnabled() {
        return viewFrustumFilterEnabled;
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

    public boolean isTeleportOptimizationEnabled() {
        return teleportOptimizationEnabled;
    }

    public PriorityPacketScheduler getPacketScheduler() {
        return packetScheduler;
    }
    
    private void cancelFoliaTask(Object task, String taskName) {
        try {
            java.lang.reflect.Method cancelMethod = null;
            try {
                cancelMethod = task.getClass().getDeclaredMethod("cancel");
                cancelMethod.setAccessible(true);
            } catch (NoSuchMethodException e) {
                cancelMethod = task.getClass().getMethod("cancel");
            }
            cancelMethod.invoke(task);
            logger.info("[NetworkOptimization] Folia " + taskName + " task cancelled successfully");
        } catch (Exception e) {
            logger.warning("[NetworkOptimization] Failed to cancel " + taskName + " task: " + e.getClass().getName());
        }
    }
}
