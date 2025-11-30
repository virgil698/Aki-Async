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
        System.out.println("[AkiAsync] Bridge cleared");
    }

    public static void validateAndDisplayConfigurations() {
        Optional<Bridge> bridgeOpt = AtomicBridge.getBridge();
        if (!bridgeOpt.isPresent()) {
            System.err.println("[AkiAsync] Cannot validate: Bridge not initialized");
            return;
        }

        Bridge bridge = bridgeOpt.get();
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

            System.out.println("  [Spawning] Enabled: " + bridge.isMobSpawningEnabled() +
                ", maxPerChunk=" + bridge.getMaxEntitiesPerChunk());

            System.out.println("  [Memory] PredicateCache: " + bridge.isPredicateCacheEnabled());
            System.out.println("  [Memory] BlockPosPool: " + bridge.isBlockPosPoolEnabled());
            System.out.println("  [Memory] ListPrealloc: " + bridge.isListPreallocEnabled() +
                ", capacity=" + bridge.getListPreallocCapacity());

            System.out.println("[AkiAsync] ✓ All configurations validated successfully");
            System.out.println("[AkiAsync] ✓ Mixins will initialize on first use (lazy loading)");
        } catch (Exception e) {
            System.err.println("[AkiAsync] ✗ Configuration validation error: " + e.getMessage());
            if (bridge != null && bridge.isDebugLoggingEnabled()) {
                java.io.StringWriter sw = new java.io.StringWriter();
                e.printStackTrace(new java.io.PrintWriter(sw));
                bridge.debugLog("[AkiAsync] Stack trace: " + sw.toString());
            }
        }

        System.out.println("[AkiAsync] ========================================================");
    }
}
