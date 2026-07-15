package com.akiasync.mixin;

import com.akiasync.mixin.profiler.LagProfilerCollector;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public final class BridgeManager {
    private static final Bridge NO_OP = snapshot -> { };
    public static final BridgeManager INSTANCE = new BridgeManager();
    private final AtomicReference<Bridge> bridge = new AtomicReference<>(NO_OP);
    private final AtomicInteger detailedTicks = new AtomicInteger();

    private BridgeManager() {
    }

    public void install(Bridge newBridge) {
        Objects.requireNonNull(newBridge, "newBridge");
        if (!bridge.compareAndSet(NO_OP, newBridge)) {
            throw new IllegalStateException("A bridge is already installed");
        }
        LagProfilerCollector.startSampler();
    }

    public void uninstall(Bridge expectedBridge) {
        if (bridge.compareAndSet(expectedBridge, NO_OP)) {
            detailedTicks.set(0);
            LagProfilerCollector.stopSampler();
        }
    }

    public boolean isInstalled() {
        return bridge.get() != NO_OP;
    }

    public void publish(Bridge.LagTickSnapshot snapshot) {
        try {
            bridge.get().publish(snapshot);
        } catch (Throwable ignored) {
            // Diagnostics must never take down the server tick.
        }
    }

    public void requestDetailedTicks(int ticks) {
        int bounded = Math.max(0, Math.min(12_000, ticks));
        detailedTicks.accumulateAndGet(bounded, Math::max);
    }

    public boolean consumeDetailedTick() {
        while (true) {
            int remaining = detailedTicks.get();
            if (remaining <= 0) {
                return false;
            }
            if (detailedTicks.compareAndSet(remaining, remaining - 1)) {
                return true;
            }
        }
    }

    public int detailedTicksRemaining() {
        return detailedTicks.get();
    }
}
