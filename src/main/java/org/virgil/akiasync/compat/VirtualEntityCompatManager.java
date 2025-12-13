package org.virgil.akiasync.compat;

import org.virgil.akiasync.AkiAsyncPlugin;
import org.virgil.akiasync.config.ConfigManager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class VirtualEntityCompatManager {
    
    private final AkiAsyncPlugin plugin;
    private final ConfigManager configManager;
    private final Logger logger;
    private final PluginDetectorRegistry detectorRegistry;
    
    
    private final Map<String, Boolean> pluginAvailability;
    private boolean enabled;
    
    public VirtualEntityCompatManager(AkiAsyncPlugin plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.logger = plugin.getLogger();
        this.pluginAvailability = new ConcurrentHashMap<>();
        this.detectorRegistry = new PluginDetectorRegistry(logger);
        
        this.enabled = false;
    }
    
    public void initialize() {
        
        try {
            enabled = configManager.isVirtualEntityCompatibilityEnabled();
        } catch (NoSuchMethodError e) {
            
            enabled = true;
            logger.info("[VirtualEntity] Virtual entity compatibility config not found, defaulting to enabled");
        }
        
        if (!enabled) {
            logger.info("[VirtualEntity] Virtual entity compatibility is disabled in config");
            return;
        }
        
        try {
            boolean debugEnabled = configManager.isVirtualEntityDebugEnabled();
            org.virgil.akiasync.util.DebugLogger.updateDebugState(debugEnabled);
            logger.info("[VirtualEntity] Debug logging: " + (debugEnabled ? "enabled" : "disabled"));
        } catch (NoSuchMethodError e) {
            
            org.virgil.akiasync.util.DebugLogger.updateDebugState(false);
            logger.info("[VirtualEntity] Debug logging config not found, defaulting to disabled");
        }
        
        logger.info("[VirtualEntity] Initializing virtual entity compatibility system...");
        
        boolean fancynpcsEnabled = true;
        boolean fancynpcsUseAPI = true;
        int fancynpcsPriority = 90;
        try {
            fancynpcsEnabled = configManager.isFancynpcsCompatEnabled();
            fancynpcsUseAPI = configManager.isFancynpcsUseAPI();
            fancynpcsPriority = configManager.getFancynpcsPriority();
        } catch (NoSuchMethodError e) {
            
            logger.info("[VirtualEntity] FancyNpcs config not found, using defaults (enabled: true, API: true, priority: 90)");
        }
        
        if (fancynpcsEnabled) {
            try {
                FancyNpcsDetector fancyNpcsDetector = new FancyNpcsDetector();
                if (fancyNpcsDetector.isAvailable()) {
                    fancyNpcsDetector.setUseAPI(fancynpcsUseAPI);
                    detectorRegistry.registerDetector(fancyNpcsDetector);
                    pluginAvailability.put("FancyNpcs", true);
                    logger.info("[VirtualEntity] FancyNpcs detected and registered (Priority: " + 
                               fancynpcsPriority + ", API: " + fancynpcsUseAPI + ")");
                } else {
                    pluginAvailability.put("FancyNpcs", false);
                    logger.info("[VirtualEntity] FancyNpcs not found, using fallback detection if needed");
                }
            } catch (Exception e) {
                pluginAvailability.put("FancyNpcs", false);
                logger.warning("[VirtualEntity] Failed to initialize FancyNpcs detector: " + e.getMessage());
                if (org.virgil.akiasync.util.DebugLogger.isDebugEnabled()) {
                    e.printStackTrace();
                }
            }
        } else {
            logger.info("[VirtualEntity] FancyNpcs compatibility disabled in config");
        }
        
        boolean znpcsplusEnabled = true;
        int znpcsplusPriority = 90;
        try {
            znpcsplusEnabled = configManager.isZnpcsplusCompatEnabled();
            znpcsplusPriority = configManager.getZnpcsplusPriority();
        } catch (NoSuchMethodError e) {
            
            logger.info("[VirtualEntity] ZNPCsPlus config not found, using defaults (enabled: true, priority: 90)");
        }
        
        if (znpcsplusEnabled) {
            try {
                ZNPCsPlusDetector znpcsPlusDetector = new ZNPCsPlusDetector();
                if (znpcsPlusDetector.isAvailable()) {
                    detectorRegistry.registerDetector(znpcsPlusDetector);
                    pluginAvailability.put("ZNPCsPlus", true);
                    logger.info("[VirtualEntity] ZNPCsPlus detected and registered (Priority: " + 
                               znpcsplusPriority + ")");
                } else {
                    pluginAvailability.put("ZNPCsPlus", false);
                    logger.info("[VirtualEntity] ZNPCsPlus not found, using fallback detection if needed");
                }
            } catch (Exception e) {
                pluginAvailability.put("ZNPCsPlus", false);
                logger.warning("[VirtualEntity] Failed to initialize ZNPCsPlus detector: " + e.getMessage());
                if (org.virgil.akiasync.util.DebugLogger.isDebugEnabled()) {
                    e.printStackTrace();
                }
            }
        } else {
            logger.info("[VirtualEntity] ZNPCsPlus compatibility disabled in config");
        }
        
        int registeredCount = detectorRegistry.size();
        if (registeredCount > 0) {
            logger.info("[VirtualEntity] Successfully initialized with " + registeredCount + 
                       " plugin detector(s)");
            logger.info("[VirtualEntity] Features enabled:");
            
            boolean bypassQueue = true;
            boolean excludeThrottling = true;
            boolean debugEnabled = false;
            try {
                bypassQueue = configManager.isVirtualEntityBypassPacketQueue();
                excludeThrottling = configManager.isVirtualEntityExcludeFromThrottling();
                debugEnabled = configManager.isVirtualEntityDebugEnabled();
            } catch (NoSuchMethodError e) {
                
            }
            
            logger.info("  - Bypass packet queue: " + bypassQueue);
            logger.info("  - Exclude from throttling: " + excludeThrottling);
            logger.info("  - Debug logging: " + debugEnabled);
        } else {
            logger.info("[VirtualEntity] No virtual entity plugins detected, system will use fallback detection");
        }
    }
    
    public void shutdown() {
        if (!enabled) {
            return;
        }
        
        logger.info("[VirtualEntity] Shutting down virtual entity compatibility system...");
        
        detectorRegistry.clear();
        
        pluginAvailability.clear();
        
        enabled = false;
        
        logger.info("[VirtualEntity] Virtual entity compatibility system shut down successfully");
    }
    
    public boolean isPluginAvailable(String pluginName) {
        return pluginAvailability.getOrDefault(pluginName, false);
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public PluginDetectorRegistry getDetectorRegistry() {
        return detectorRegistry;
    }
    
    
    public Map<String, Boolean> getPluginAvailability() {
        return new ConcurrentHashMap<>(pluginAvailability);
    }
    
    public void reload() {
        logger.info("[VirtualEntity] Reloading virtual entity compatibility configuration...");
        
        for (PluginDetector detector : detectorRegistry.getDetectors()) {
            try {
                if (detector instanceof FancyNpcsDetector) {
                    ((FancyNpcsDetector) detector).clearCache();
                } else if (detector instanceof ZNPCsPlusDetector) {
                    ((ZNPCsPlusDetector) detector).clearCache();
                }
            } catch (Exception e) {
                logger.warning("[VirtualEntity] Failed to clear cache for detector " + 
                             detector.getPluginName() + ": " + e.getMessage());
            }
        }
        
        shutdown();
        
        initialize();
        
        if (enabled) {
            org.virgil.akiasync.util.VirtualEntityDetector.setDetectorRegistry(detectorRegistry);
        }
        
        logger.info("[VirtualEntity] Virtual entity compatibility configuration reloaded successfully");
    }
}
