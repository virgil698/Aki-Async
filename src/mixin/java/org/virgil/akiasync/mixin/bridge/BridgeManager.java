package org.virgil.akiasync.mixin.bridge;

/**
 * Bridge manager for AkiAsync plugin.
 * Manages the bridge instance that allows mixins to access plugin configuration.
 * 
 * Following Leaves plugin template pattern:
 * - The plugin registers its implementation via setBridge()
 * - Mixins access the bridge via getBridge()
 * 
 * @author Virgil
 */
public final class BridgeManager {
    
    private static volatile Bridge bridge = null;
    
    private BridgeManager() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }
    
    /**
     * Set the bridge implementation.
     * This method is called by the plugin during initialization.
     * 
     * @param bridge the bridge implementation
     */
    public static void setBridge(Bridge bridge) {
        if (BridgeManager.bridge != null) {
            throw new IllegalStateException("Bridge has already been set");
        }
        BridgeManager.bridge = bridge;
        System.out.println("[AkiAsync] Bridge implementation registered: " + bridge.getClass().getName());
    }
    
    /**
     * Get the bridge implementation.
     * This method is called by mixins to access plugin configuration.
     * 
     * @return the bridge implementation, or null if not yet initialized
     */
    public static Bridge getBridge() {
        return bridge;
    }
    
    /**
     * Check if the bridge is initialized.
     * 
     * @return true if bridge is set, false otherwise
     */
    public static boolean isBridgeInitialized() {
        return bridge != null;
    }
    
    /**
     * Clear the bridge implementation.
     * This method is called by the plugin during shutdown.
     */
    public static void clearBridge() {
        bridge = null;
        System.out.println("[AkiAsync] Bridge cleared");
    }
    
    /**
     * Validate and display all mixin configurations at startup.
     * Does not load Mixin classes (Mixin framework forbids Class.forName).
     * Mixins will initialize lazily on first use, reading from this bridge.
     */
    public static void validateAndDisplayConfigurations() {
        if (bridge == null) {
            System.err.println("[AkiAsync] Cannot validate: Bridge not initialized");
            return;
        }
        
        System.out.println("[AkiAsync] ========== Mixin Configuration Status ==========");
        
        try {
            // Brain mixins
            System.out.println("  [Brain] Throttle: enabled=" + bridge.isBrainThrottleEnabled() + 
                ", interval=" + bridge.getBrainThrottleInterval());
            
            // Entity mixins
            System.out.println("  [Entity] TickParallel: enabled=" + bridge.isEntityTickParallel() + 
                ", threads=" + bridge.getEntityTickThreads() + 
                ", minEntities=" + bridge.getMinEntitiesForParallel());
            System.out.println("  [Entity] Collision: enabled=" + bridge.isCollisionOptimizationEnabled());
            System.out.println("  [Entity] Push: enabled=" + bridge.isPushOptimizationEnabled());
            System.out.println("  [Entity] LookupCache: enabled=" + bridge.isEntityLookupCacheEnabled() + 
                ", duration=" + bridge.getEntityLookupCacheDurationMs() + "ms");
            System.out.println("  [Entity] Tracker: enabled=" + bridge.isEntityTrackerEnabled() + 
                ", executor=" + (bridge.getGeneralExecutor() != null));
            
            // Pathfinding & Spawning
            System.out.println("  [Pathfinding] Budget: " + bridge.getPathfindingTickBudget());
            System.out.println("  [Spawning] Enabled: " + bridge.isMobSpawningEnabled() + 
                ", maxPerChunk=" + bridge.getMaxEntitiesPerChunk());
            
            // Memory optimizations
            System.out.println("  [Memory] PredicateCache: " + bridge.isPredicateCacheEnabled());
            System.out.println("  [Memory] BlockPosPool: " + bridge.isBlockPosPoolEnabled());
            System.out.println("  [Memory] ListPrealloc: " + bridge.isListPreallocEnabled() + 
                ", capacity=" + bridge.getListPreallocCapacity());
            
            System.out.println("[AkiAsync] ✓ All configurations validated successfully");
            System.out.println("[AkiAsync] ⏳ Mixins will initialize on first use (lazy loading)");
        } catch (Exception e) {
            System.err.println("[AkiAsync] ✗ Configuration validation error: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("[AkiAsync] ========================================================");
    }
}

