package org.virgil.akiasync.compat;

import org.virgil.akiasync.config.ConfigManager;

import java.util.logging.Logger;

public class VirtualEntityPacketHandler {

    private final PluginDetectorRegistry detectorRegistry;
    private final ConfigManager configManager;
    private final Logger logger;
    private volatile boolean enabled;

    public VirtualEntityPacketHandler(PluginDetectorRegistry detectorRegistry, 
                                     ConfigManager configManager, 
                                     Logger logger) {
        this.detectorRegistry = detectorRegistry;
        this.configManager = configManager;
        this.logger = logger;
        this.enabled = false;
    }

    public void initialize() {
        try {
            enabled = configManager.isVirtualEntityCompatibilityEnabled();
            if (enabled) {
                logger.info("[VirtualEntity] Packet handler initialized");
            }
        } catch (Exception e) {
            logger.warning("[VirtualEntity] Failed to initialize packet handler: " + e.getMessage());
            enabled = false;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void shutdown() {
        enabled = false;
        logger.info("[VirtualEntity] Packet handler shutdown");
    }
}
