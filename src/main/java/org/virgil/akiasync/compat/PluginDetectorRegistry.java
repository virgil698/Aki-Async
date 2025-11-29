package org.virgil.akiasync.compat;

import org.bukkit.entity.Entity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class PluginDetectorRegistry {
    
    private final List<PluginDetector> detectors;
    private final Map<String, PluginDetector> detectorMap;
    private final Logger logger;
    
    public PluginDetectorRegistry(Logger logger) {
        this.detectors = new ArrayList<>();
        this.detectorMap = new ConcurrentHashMap<>();
        this.logger = logger;
    }
    
    public synchronized void registerDetector(PluginDetector detector) {
        if (detector == null) {
            throw new IllegalArgumentException("Detector cannot be null");
        }
        
        String pluginName = detector.getPluginName();
        if (pluginName == null || pluginName.isEmpty()) {
            throw new IllegalArgumentException("Detector plugin name cannot be null or empty");
        }
        
        if (detectorMap.containsKey(pluginName)) {
            unregisterDetector(pluginName);
        }
        
        detectors.add(detector);
        detectorMap.put(pluginName, detector);
        sortDetectorsByPriority();
        
        logger.info("[VirtualEntity] Registered detector for " + pluginName + 
                   " with priority " + detector.getPriority());
    }
    
    public synchronized void unregisterDetector(String pluginName) {
        PluginDetector detector = detectorMap.remove(pluginName);
        if (detector != null) {
            detectors.remove(detector);
            logger.info("[VirtualEntity] Unregistered detector for " + pluginName);
        }
    }
    
    public synchronized void sortDetectorsByPriority() {
        detectors.sort(Comparator.comparingInt(PluginDetector::getPriority).reversed());
    }
    
    public boolean isVirtualEntity(Entity entity) {
        if (entity == null) {
            return false;
        }
        
        for (PluginDetector detector : detectors) {
            try {
                
                if (!detector.isAvailable()) {
                    continue;
                }
                
                if (detector.isVirtualEntity(entity)) {
                    return true;
                }
            } catch (Exception e) {
                
                logger.warning("[VirtualEntity] Detector " + detector.getPluginName() + 
                             " threw exception while checking entity " + entity.getUniqueId() + 
                             ": " + e.getMessage());
                if (logger.isLoggable(java.util.logging.Level.FINE)) {
                    e.printStackTrace();
                }
            }
        }
        
        return false;
    }
    
    public String getEntitySource(Entity entity) {
        if (entity == null) {
            return null;
        }
        
        for (PluginDetector detector : detectors) {
            try {
                
                if (!detector.isAvailable()) {
                    continue;
                }
                
                if (detector.isVirtualEntity(entity)) {
                    return detector.getPluginName();
                }
            } catch (Exception e) {
                
                logger.warning("[VirtualEntity] Detector " + detector.getPluginName() + 
                             " threw exception while checking entity " + entity.getUniqueId() + 
                             ": " + e.getMessage());
                if (logger.isLoggable(java.util.logging.Level.FINE)) {
                    e.printStackTrace();
                }
            }
        }
        
        return null;
    }
    
    public synchronized List<PluginDetector> getDetectors() {
        return new ArrayList<>(detectors);
    }
    
    public PluginDetector getDetector(String pluginName) {
        return detectorMap.get(pluginName);
    }
    
    public synchronized void clear() {
        detectors.clear();
        detectorMap.clear();
        logger.info("[VirtualEntity] Cleared all detectors");
    }
    
    public int size() {
        return detectors.size();
    }
}
