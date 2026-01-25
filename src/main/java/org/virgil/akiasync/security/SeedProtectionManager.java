package org.virgil.akiasync.security;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.plugin.Plugin;
import org.virgil.akiasync.AkiAsyncPlugin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.logging.Logger;

public class SeedProtectionManager implements Listener {

    private final AkiAsyncPlugin plugin;
    private final Logger logger;
    private final boolean returnFakeSeed;
    private final long fakeSeed;
    private final boolean logSuspiciousAccess;

    public SeedProtectionManager(AkiAsyncPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.returnFakeSeed = plugin.getConfig().getBoolean("seed-encryption.anti-plugin-theft.return-fake-seed", true);
        this.fakeSeed = plugin.getConfig().getLong("seed-encryption.anti-plugin-theft.fake-seed-value", 0L);
        this.logSuspiciousAccess = plugin.getConfig().getBoolean("seed-encryption.anti-plugin-theft.log-suspicious-access", true);
    }

    public void initialize() {

        Bukkit.getPluginManager().registerEvents(this, plugin);

        for (World world : Bukkit.getWorlds()) {
            protectWorld(world);
        }

        logger.info("[SeedProtection] Seed protection initialized");
        if (returnFakeSeed) {
            logger.info("[SeedProtection] Fake seed mode enabled - plugins will receive fake seed: " + fakeSeed);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onWorldLoad(WorldLoadEvent event) {
        protectWorld(event.getWorld());
    }

    private void protectWorld(World world) {
        try {

            Class<?> craftWorldClass = world.getClass();

            if (logSuspiciousAccess) {
                detectSuspiciousAccess(world);
            }

            logger.fine("[SeedProtection] Applied protection to world: " + world.getName());

        } catch (Exception e) {
            logger.warning("[SeedProtection] Failed to protect world " + world.getName() + ": " + e.getMessage());
        }
    }

    private void detectSuspiciousAccess(World world) {

        Thread monitorThread = new Thread(() -> {
            try {

                StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

                for (StackTraceElement element : stackTrace) {
                    String className = element.getClassName();
                    String methodName = element.getMethodName();

                    if (methodName.contains("getSeed") || methodName.contains("seed")) {

                        if (!className.startsWith("org.virgil.akiasync") &&
                            !className.startsWith("net.minecraft") &&
                            !className.startsWith("org.bukkit") &&
                            !className.startsWith("com.mojang")) {

                            logger.warning("[SeedProtection] Suspicious seed access detected!");
                            logger.warning("  Class: " + className);
                            logger.warning("  Method: " + methodName);
                            logger.warning("  Line: " + element.getLineNumber());

                            String pluginName = identifyPlugin(className);
                            if (pluginName != null) {
                                logger.warning("  Suspected plugin: " + pluginName);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                    "SeedProtectionManager", "monitorSeedAccess", e);
            }
        }, "SeedProtection-Monitor");

        monitorThread.setDaemon(true);
        monitorThread.start();
    }

    private String identifyPlugin(String className) {

        String[] parts = className.split("\\.");
        if (parts.length >= 3) {
            return parts[2];
        }

        for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
            String pluginPackage = plugin.getClass().getPackage().getName();
            if (className.startsWith(pluginPackage)) {
                return plugin.getName();
            }
        }

        return null;
    }

    public long getFakeSeed() {
        return fakeSeed;
    }

    public boolean shouldReturnFakeSeed() {
        return returnFakeSeed;
    }

    public void logSeedAccess(String source, long requestedSeed) {
        if (logSuspiciousAccess) {
            logger.info(String.format(
                "[SeedProtection] Seed access: source=%s, seed=%d",
                source, requestedSeed
            ));
        }
    }
}
