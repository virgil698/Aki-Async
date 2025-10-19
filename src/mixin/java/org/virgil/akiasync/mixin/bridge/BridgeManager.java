package org.virgil.akiasync.mixin.bridge;

public final class BridgeManager {
    
    private static volatile Bridge bridge = null;
    
    private BridgeManager() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }
    
    public static void setBridge(Bridge bridge) {
        if (BridgeManager.bridge != null) {
            throw new IllegalStateException("Bridge has already been set");
        }
        BridgeManager.bridge = bridge;
        System.out.println("[AkiAsync] Bridge implementation registered: " + bridge.getClass().getName());
    }
    
    public static Bridge getBridge() {
        return bridge;
    }
    
    public static boolean isBridgeInitialized() {
        return bridge != null;
    }
    
    public static void clearBridge() {
        bridge = null;
        System.out.println("[AkiAsync] Bridge cleared");
    }
    
    public static void validateAndDisplayConfigurations() {
        if (bridge == null) {
            System.err.println("[AkiAsync] Cannot validate: Bridge not initialized");
            return;
        }
        
        System.out.println("[AkiAsync] ========== Mixin Configuration Status ==========");
        
        try {
            System.out.println("  [Brain] Throttle: enabled=" + bridge.isBrainThrottleEnabled() + 
                ", interval=" + bridge.getBrainThrottleInterval());
            
            System.out.println("  [Entity] TickParallel: enabled=" + bridge.isEntityTickParallel() + 
                ", threads=" + bridge.getEntityTickThreads() + 
                ", minEntities=" + bridge.getMinEntitiesForParallel());
            System.out.println("  [Entity] Collision: enabled=" + bridge.isCollisionOptimizationEnabled());
            System.out.println("  [Entity] Push: enabled=" + bridge.isPushOptimizationEnabled());
            System.out.println("  [Entity] LookupCache: enabled=" + bridge.isEntityLookupCacheEnabled() + 
                ", duration=" + bridge.getEntityLookupCacheDurationMs() + "ms");
            System.out.println("  [Entity] Tracker: enabled=" + bridge.isEntityTrackerEnabled() + 
                ", executor=" + (bridge.getGeneralExecutor() != null));
            
            System.out.println("  [Pathfinding] Budget: " + bridge.getPathfindingTickBudget());
            System.out.println("  [Spawning] Enabled: " + bridge.isMobSpawningEnabled() + 
                ", maxPerChunk=" + bridge.getMaxEntitiesPerChunk());
            
            System.out.println("  [Memory] PredicateCache: " + bridge.isPredicateCacheEnabled());
            System.out.println("  [Memory] BlockPosPool: " + bridge.isBlockPosPoolEnabled());
            System.out.println("  [Memory] ListPrealloc: " + bridge.isListPreallocEnabled() + 
                ", capacity=" + bridge.getListPreallocCapacity());
            
            System.out.println("[AkiAsync] �?All configurations validated successfully");
            System.out.println("[AkiAsync] �?Mixins will initialize on first use (lazy loading)");
        } catch (Exception e) {
            System.err.println("[AkiAsync] �?Configuration validation error: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("[AkiAsync] ========================================================");
    }
}

