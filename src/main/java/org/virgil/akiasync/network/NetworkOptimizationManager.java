package org.virgil.akiasync.network;

import org.virgil.akiasync.mixin.bridge.Bridge;
import org.virgil.akiasync.mixin.bridge.BridgeManager;

import java.util.concurrent.atomic.AtomicLong;

public class NetworkOptimizationManager {

    private static boolean initialized = false;
    private static boolean enabled = false;

    private static boolean fastVarIntEnabled = true;
    private static boolean eventLoopAffinityEnabled = true;
    private static boolean byteBufOptimizerEnabled = true;

    private static boolean strictEventLoopChecking = false;
    private static boolean pooledByteBufAllocator = true;
    private static boolean directByteBufPreferred = true;

    private static final AtomicLong varIntEncoded = new AtomicLong(0);
    private static final AtomicLong varIntDecoded = new AtomicLong(0);
    private static final AtomicLong eventLoopExecutions = new AtomicLong(0);
    private static final AtomicLong bufferAllocations = new AtomicLong(0);

    public static void initialize(boolean enableOptimization,
                                   boolean enableFastVarInt,
                                   boolean enableEventLoopAffinity,
                                   boolean enableByteBufOptimizer,
                                   boolean strictChecking,
                                   boolean pooledAllocator,
                                   boolean directBuffer) {
        if (initialized) {
            Bridge bridge = BridgeManager.getBridge();
            if (bridge != null) {
                bridge.debugLog("[NetworkOptimization] Already initialized");
            }
            return;
        }

        enabled = enableOptimization;
        fastVarIntEnabled = enableFastVarInt;
        eventLoopAffinityEnabled = enableEventLoopAffinity;
        byteBufOptimizerEnabled = enableByteBufOptimizer;
        strictEventLoopChecking = strictChecking;
        pooledByteBufAllocator = pooledAllocator;
        directByteBufPreferred = directBuffer;

        if (enabled) {
            if (eventLoopAffinityEnabled) {
                EventLoopAffinity.initialize(strictEventLoopChecking, false);
            }

            if (byteBufOptimizerEnabled) {
                ByteBufOptimizer.initialize(pooledByteBufAllocator, directByteBufPreferred);
            }
        }

        initialized = true;

        Bridge bridge = BridgeManager.getBridge();
        if (bridge != null) {
            bridge.debugLog(
                "[NetworkOptimization] Initialized - enabled=%s, fastVarInt=%s, eventLoop=%s, byteBuf=%s",
                enabled, fastVarIntEnabled, eventLoopAffinityEnabled, byteBufOptimizerEnabled
            );
        }
    }

    public static void shutdown() {
        if (!initialized) {
            return;
        }

        enabled = false;
        initialized = false;

        Bridge bridge = BridgeManager.getBridge();
        if (bridge != null) {
            bridge.debugLog("[NetworkOptimization] Shutdown completed");

            if (varIntEncoded.get() > 0 || varIntDecoded.get() > 0) {
                bridge.debugLog(
                    "[NetworkOptimization] Stats - VarInt: encoded=%d, decoded=%d, eventLoop=%d, buffers=%d",
                    varIntEncoded.get(), varIntDecoded.get(),
                    eventLoopExecutions.get(), bufferAllocations.get()
                );
            }
        }

        varIntEncoded.set(0);
        varIntDecoded.set(0);
        eventLoopExecutions.set(0);
        bufferAllocations.set(0);
    }

    public static boolean isEnabled() {
        return initialized && enabled;
    }

    public static boolean isFastVarIntEnabled() {
        return isEnabled() && fastVarIntEnabled;
    }

    public static boolean isEventLoopAffinityEnabled() {
        return isEnabled() && eventLoopAffinityEnabled;
    }

    public static boolean isByteBufOptimizerEnabled() {
        return isEnabled() && byteBufOptimizerEnabled;
    }

    public static void recordVarIntEncoded() {
        varIntEncoded.incrementAndGet();
    }

    public static void recordVarIntDecoded() {
        varIntDecoded.incrementAndGet();
    }

    public static void recordEventLoopExecution() {
        eventLoopExecutions.incrementAndGet();
    }

    public static void recordBufferAllocation() {
        bufferAllocations.incrementAndGet();
    }

    public static String getStatistics() {
        if (!initialized) {
            return "Network optimization not initialized";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=== Network Optimization Statistics ===\n");
        sb.append(String.format("Status: %s\n", enabled ? "ENABLED" : "DISABLED"));
        sb.append(String.format("FastVarInt: %s (encoded=%d, decoded=%d)\n",
            fastVarIntEnabled ? "ON" : "OFF", varIntEncoded.get(), varIntDecoded.get()));
        sb.append(String.format("EventLoop Affinity: %s (executions=%d, strict=%s)\n",
            eventLoopAffinityEnabled ? "ON" : "OFF",
            eventLoopExecutions.get(), strictEventLoopChecking));
        sb.append(String.format("ByteBuf Optimizer: %s (allocations=%d, pooled=%s, direct=%s)\n",
            byteBufOptimizerEnabled ? "ON" : "OFF",
            bufferAllocations.get(), pooledByteBufAllocator, directByteBufPreferred));

        if (byteBufOptimizerEnabled) {
            sb.append(ByteBufOptimizer.getStats()).append("\n");
        }

        return sb.toString();
    }

    public static void reload(boolean enableOptimization,
                              boolean enableFastVarInt,
                              boolean enableEventLoopAffinity,
                              boolean enableByteBufOptimizer,
                              boolean strictChecking,
                              boolean pooledAllocator,
                              boolean directBuffer) {
        if (!initialized) {
            initialize(enableOptimization, enableFastVarInt, enableEventLoopAffinity,
                enableByteBufOptimizer, strictChecking, pooledAllocator, directBuffer);
            return;
        }

        enabled = enableOptimization;
        fastVarIntEnabled = enableFastVarInt;
        eventLoopAffinityEnabled = enableEventLoopAffinity;
        byteBufOptimizerEnabled = enableByteBufOptimizer;
        strictEventLoopChecking = strictChecking;
        pooledByteBufAllocator = pooledAllocator;
        directByteBufPreferred = directBuffer;

        if (enabled) {
            if (eventLoopAffinityEnabled) {
                EventLoopAffinity.initialize(strictEventLoopChecking, false);
            }

            if (byteBufOptimizerEnabled) {
                ByteBufOptimizer.initialize(pooledByteBufAllocator, directByteBufPreferred);
            }
        }

        Bridge bridge = BridgeManager.getBridge();
        if (bridge != null) {
            bridge.debugLog("[NetworkOptimization] Configuration reloaded");
        }
    }
}
