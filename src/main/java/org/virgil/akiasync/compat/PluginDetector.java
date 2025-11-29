package org.virgil.akiasync.compat;

import org.bukkit.entity.Entity;

public interface PluginDetector {
    
    String getPluginName();
    
    int getPriority();
    
    boolean isAvailable();
    
    boolean isVirtualEntity(Entity entity);
    
    boolean detectViaAPI(Entity entity);
    
    boolean detectViaFallback(Entity entity);
}
