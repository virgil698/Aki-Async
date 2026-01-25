package org.virgil.akiasync.mixin.mixins.network.connection;

import net.minecraft.server.network.ServerConnectionListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.concurrent.atomic.AtomicLong;

@SuppressWarnings("unused")
@Mixin(ServerConnectionListener.class)
public class FlushConsolidationMixin {

    @Unique
    private static volatile boolean enabled = true;
    @Unique
    private static volatile boolean initialized = false;
    @Unique
    private static volatile int explicitFlushAfterFlushes = 256;
    @Unique
    private static volatile boolean consolidateWhenNoReadInProgress = true;

    @Unique
    private static final AtomicLong channelsWithConsolidation = new AtomicLong(0);

    @Unique
    private static void akiasync$addFlushConsolidation(Object channel) {
        if (!initialized) {
            akiasync$init();
        }

        if (!enabled) {
            return;
        }

        try {
            org.virgil.akiasync.mixin.bridge.Bridge bridge =
                org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
            if (bridge != null) {
                boolean added = bridge.addFlushConsolidationHandler(channel, explicitFlushAfterFlushes, consolidateWhenNoReadInProgress);
                if (added) {
                    channelsWithConsolidation.incrementAndGet();
                }
            }
        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "FlushConsolidation", "addFlushConsolidation", e);
        }
    }

    @Unique
    private static synchronized void akiasync$init() {
        if (initialized) {
            return;
        }

        try {
            org.virgil.akiasync.mixin.bridge.Bridge bridge =
                org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();

            if (bridge != null) {
                enabled = bridge.isFlushConsolidationEnabled();
                explicitFlushAfterFlushes = bridge.getFlushConsolidationExplicitFlushAfterFlushes();
                consolidateWhenNoReadInProgress = bridge.isFlushConsolidationConsolidateWhenNoReadInProgress();

                bridge.debugLog("[FlushConsolidation] Initialized: enabled=%s, explicitFlushAfter=%d, consolidateWhenNoRead=%s",
                    enabled, explicitFlushAfterFlushes, consolidateWhenNoReadInProgress);

                initialized = true;
            }
        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "FlushConsolidation", "init", e);
        }
    }

    @Unique
    private static boolean akiasync$isEnabled() {
        if (!initialized) {
            akiasync$init();
        }
        return enabled;
    }

    @Unique
    private static long akiasync$getChannelsWithConsolidation() {
        return channelsWithConsolidation.get();
    }

    @Unique
    private static String akiasync$getStatistics() {
        return String.format(
            "FlushConsolidation: enabled=%s, channels=%d, explicitFlushAfter=%d",
            enabled, channelsWithConsolidation.get(), explicitFlushAfterFlushes
        );
    }

    @Unique
    private static void akiasync$reload() {
        initialized = false;
    }
}
