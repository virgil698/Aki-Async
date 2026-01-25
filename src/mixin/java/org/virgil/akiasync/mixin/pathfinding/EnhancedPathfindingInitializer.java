package org.virgil.akiasync.mixin.pathfinding;

import net.minecraft.server.MinecraftServer;
import org.virgil.akiasync.mixin.bridge.Bridge;
import org.virgil.akiasync.mixin.bridge.BridgeManager;
import org.virgil.akiasync.mixin.util.BridgeConfigCache;

public class EnhancedPathfindingInitializer {

    private static volatile boolean initialized = false;
    private static volatile boolean enabled = false;

    public static synchronized void initialize() {
        if (initialized) return;

        Bridge bridge = BridgeManager.getBridge();
        if (bridge == null) {
            BridgeConfigCache.debugLog("[EnhancedPathfinding] Bridge not available, skipping initialization");
            return;
        }

        enabled = bridge.isAsyncPathfindingEnabled() && bridge.isEnhancedPathfindingEnabled();

        if (!enabled) {
            BridgeConfigCache.debugLog("[EnhancedPathfinding] Enhanced pathfinding is disabled");
            initialized = true;
            return;
        }

        int maxConcurrent = bridge.getEnhancedPathfindingMaxConcurrentRequests();
        int maxPerTick = bridge.getEnhancedPathfindingMaxRequestsPerTick();
        int highPriorityDist = bridge.getEnhancedPathfindingHighPriorityDistance();
        int mediumPriorityDist = bridge.getEnhancedPathfindingMediumPriorityDistance();

        EnhancedPathfindingSystem.updateConfig(
            enabled,
            maxConcurrent,
            maxPerTick,
            highPriorityDist,
            mediumPriorityDist
        );

        BridgeConfigCache.debugLog(String.format(
            "[EnhancedPathfinding] Initialized: maxConcurrent=%d, maxPerTick=%d, highPriority=%d, mediumPriority=%d",
            maxConcurrent, maxPerTick, highPriorityDist, mediumPriorityDist
        ));

        initialized = true;
    }

    public static void tick() {
        if (!initialized) {
            initialize();
        }

        if (enabled) {
            EnhancedPathfindingSystem.processTick();
        }
    }

    public static void shutdown() {
        if (enabled) {
            EnhancedPathfindingSystem.clear();
            BridgeConfigCache.debugLog("[EnhancedPathfinding] Shutdown complete");
        }
        initialized = false;
        enabled = false;
    }

    public static String getStatistics() {
        if (!enabled) {
            return "EnhancedPathfinding: Disabled";
        }
        return EnhancedPathfindingSystem.getStatistics();
    }

    public static boolean isEnabled() {
        return enabled;
    }
}
