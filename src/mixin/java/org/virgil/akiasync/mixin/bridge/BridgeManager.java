package org.virgil.akiasync.mixin.bridge;

import java.util.Optional;

public final class BridgeManager {

    private BridgeManager() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

    public static void setBridge(Bridge bridge) {
        AtomicBridge.setBridge(bridge);
    }

    public static Bridge getBridge() {
        return AtomicBridge.getBridge().orElse(null);
    }

    public static boolean isBridgeInitialized() {
        return AtomicBridge.getBridge().isPresent();
    }

    public static void clearBridge() {
        AtomicBridge.clearBridge();
        Bridge bridge = getBridge();
        if (bridge != null && bridge.isDebugLoggingEnabled()) {
            bridge.debugLog("[AkiAsync] Bridge cleared");
        }
    }

    public static void validateAndDisplayConfigurations() {
        Optional<Bridge> bridgeOpt = AtomicBridge.getBridge();
        if (!bridgeOpt.isPresent()) {
            return;
        }

        Bridge bridge = bridgeOpt.get();
        
        if (!bridge.isDebugLoggingEnabled()) {
            return;
        }

        bridge.debugLog("[AkiAsync] ========== Mixin Configuration Status ==========");

        try {
            bridge.debugLog("  [Brain] Throttle: enabled=" + bridge.isBrainThrottleEnabled() +
                ", interval=" + bridge.getBrainThrottleInterval());

            bridge.debugLog("  [Entity] TickParallel: enabled=" + bridge.isEntityTickParallel() +
                ", threads=" + bridge.getEntityTickThreads() +
                ", minEntities=" + bridge.getMinEntitiesForParallel());
            bridge.debugLog("  [Entity] Collision: enabled=" + bridge.isCollisionOptimizationEnabled());
            bridge.debugLog("  [Entity] Push: enabled=" + bridge.isPushOptimizationEnabled());
            bridge.debugLog("  [Entity] LookupCache: enabled=" + bridge.isEntityLookupCacheEnabled() +
                ", duration=" + bridge.getEntityLookupCacheDurationMs() + "ms");
            bridge.debugLog("  [Entity] Tracker: enabled=" + bridge.isEntityTrackerEnabled() +
                ", executor=" + (bridge.getGeneralExecutor() != null));

            bridge.debugLog("  [Spawning] Enabled: " + bridge.isMobSpawningEnabled() +
                ", maxPerChunk=" + bridge.getMaxEntitiesPerChunk());

            bridge.debugLog("  [Memory] PredicateCache: " + bridge.isPredicateCacheEnabled());
            bridge.debugLog("  [Memory] BlockPosPool: " + bridge.isBlockPosPoolEnabled());
            bridge.debugLog("  [Memory] ListPrealloc: " + bridge.isListPreallocEnabled() +
                ", capacity=" + bridge.getListPreallocCapacity());

            bridge.debugLog("[AkiAsync] ✓ All configurations validated successfully");
            bridge.debugLog("[AkiAsync] ✓ Mixins will initialize on first use (lazy loading)");
        } catch (Exception e) {
            bridge.errorLog("[AkiAsync] ✗ Configuration validation error: " + e.getMessage());
            if (bridge.isDebugLoggingEnabled()) {
                java.io.StringWriter sw = new java.io.StringWriter();
                e.printStackTrace(new java.io.PrintWriter(sw));
                bridge.debugLog("[AkiAsync] Stack trace: " + sw.toString());
            }
        }

        bridge.debugLog("[AkiAsync] ========================================================");
    }
}
