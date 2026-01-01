package org.virgil.akiasync.mixin.mixins.network;

import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;


@SuppressWarnings("unused")
@Mixin(value = Connection.class, priority = 900) 
public class ConnectionFlushOptimizationMixin {
    
    @Unique
    private static volatile boolean enabled = true;
    @Unique
    private static volatile boolean initialized = false;
    
    @Unique
    private static volatile int flushConsolidationThreshold = 5;
    @Unique
    private static volatile long flushConsolidationTimeoutNs = 1_000_000;
    @Unique
    private static volatile boolean useExplicitFlush = false;
    
    @Unique
    private final AtomicInteger pendingWrites = new AtomicInteger(0);
    @Unique
    private volatile long lastFlushTime = System.nanoTime();
    
    @Unique
    private static final AtomicLong totalFlushes = new AtomicLong(0);
    @Unique
    private static final AtomicLong consolidatedFlushes = new AtomicLong(0);
    @Unique
    private static final AtomicLong totalWrites = new AtomicLong(0);
    
    
    @Inject(method = "flushChannel", at = @At("HEAD"), cancellable = true, require = 0)
    private void optimizeFlush(CallbackInfo ci) {
        if (!initialized) {
            akiasync$init();
        }
        
        if (!enabled) {
            return;
        }
        
        try {
            int pending = pendingWrites.get();
            long now = System.nanoTime();
            long elapsed = now - lastFlushTime;
            
            
            if (pending < flushConsolidationThreshold && elapsed < flushConsolidationTimeoutNs) {
                consolidatedFlushes.incrementAndGet();
                ci.cancel();
                return;
            }
            
            
            pendingWrites.set(0);
            lastFlushTime = now;
            totalFlushes.incrementAndGet();
            
        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "ConnectionFlushOptimization", "flushChannel", e);
        }
    }
    
    
    @Inject(
        method = "send(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketSendListener;Z)V",
        at = @At("HEAD"),
        require = 0
    )
    private void trackWrite(Packet<?> packet, Object listener, boolean flush, CallbackInfo ci) {
        if (!initialized) {
            akiasync$init();
        }
        
        if (!enabled) {
            return;
        }
        
        pendingWrites.incrementAndGet();
        totalWrites.incrementAndGet();
    }
    
    @Unique
    private static void akiasync$init() {
        if (initialized) {
            return;
        }
        
        try {
            org.virgil.akiasync.mixin.bridge.Bridge bridge =
                org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
            
            if (bridge != null) {
                enabled = bridge.isConnectionFlushOptimizationEnabled();
                flushConsolidationThreshold = bridge.getFlushConsolidationThreshold();
                flushConsolidationTimeoutNs = bridge.getFlushConsolidationTimeoutNs();
                useExplicitFlush = bridge.isUseExplicitFlush();
                
                bridge.debugLog("[ConnectionFlushOptimization] Initialized: enabled=%s, threshold=%d, timeout=%dns",
                    enabled, flushConsolidationThreshold, flushConsolidationTimeoutNs);
            
                    initialized = true;
                }
        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "ConnectionFlushOptimization", "init", e);
        }
    }
    
    @Unique
    private static String akiasync$getStatistics() {
        long total = totalFlushes.get();
        long consolidated = consolidatedFlushes.get();
        long writes = totalWrites.get();
        
        if (total == 0 && consolidated == 0) {
            return "ConnectionFlushOptimization: No flushes processed";
        }
        
        double consolidationRate = (double) consolidated / (total + consolidated) * 100;
        double writesPerFlush = total > 0 ? (double) writes / total : 0;
        
        return String.format(
            "ConnectionFlushOptimization: flushes=%d, consolidated=%d (%.1f%%), writes=%d, writes/flush=%.1f",
            total, consolidated, consolidationRate, writes, writesPerFlush
        );
    }
}
