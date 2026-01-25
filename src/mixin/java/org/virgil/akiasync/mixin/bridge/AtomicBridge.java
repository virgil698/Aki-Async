package org.virgil.akiasync.mixin.bridge;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public final class AtomicBridge {

    private static final AtomicReference<Bridge> BRIDGE_REF = new AtomicReference<>();

    private AtomicBridge() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static boolean setBridge(Bridge bridge) {
        if (bridge == null) {
            throw new IllegalArgumentException("Bridge cannot be null");
        }

        Bridge oldBridge = BRIDGE_REF.getAndSet(bridge);

        if (oldBridge == null) {
            if (bridge.isDebugLoggingEnabled()) {
                bridge.debugLog("[AkiAsync] Bridge implementation registered: " + bridge.getClass().getName());
            }
            return true;
        } else if (oldBridge != bridge) {
            if (bridge.isDebugLoggingEnabled()) {
                bridge.debugLog("[AkiAsync] Bridge implementation replaced: " + bridge.getClass().getName());
            }
            return true;
        }

        return false;
    }

    public static Optional<Bridge> getBridge() {
        return Optional.ofNullable(BRIDGE_REF.get());
    }

    public static void clearBridge() {
        Bridge oldBridge = BRIDGE_REF.getAndSet(null);
        if (oldBridge != null && oldBridge.isDebugLoggingEnabled()) {
            oldBridge.debugLog("[AkiAsync] Bridge cleared");
        }
    }

    public static boolean isInitialized() {
        return BRIDGE_REF.get() != null;
    }

    public static boolean compareAndSet(Bridge expected, Bridge newBridge) {
        if (newBridge == null) {
            throw new IllegalArgumentException("New bridge cannot be null");
        }

        return BRIDGE_REF.compareAndSet(expected, newBridge);
    }
}
