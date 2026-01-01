package org.virgil.akiasync.mixin.mixins.network;

import net.minecraft.network.CompressionEncoder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.atomic.AtomicLong;


@SuppressWarnings("unused")
@Mixin(CompressionEncoder.class)
public class PacketCompressionOptimizationMixin {
    
    @Shadow
    private int threshold;
    
    @Unique
    private static volatile boolean enabled = true;
    @Unique
    private static volatile boolean initialized = false;
    
    @Unique
    private static volatile int adaptiveThresholdMin = 64;
    @Unique
    private static volatile int adaptiveThresholdMax = 512;
    @Unique
    private static volatile boolean useAdaptiveThreshold = true;
    @Unique
    private static volatile boolean skipSmallPackets = true;
    @Unique
    private static volatile int skipThreshold = 32;
    
    @Unique
    private static final AtomicLong totalPackets = new AtomicLong(0);
    @Unique
    private static final AtomicLong compressedPackets = new AtomicLong(0);
    @Unique
    private static final AtomicLong skippedPackets = new AtomicLong(0);
    @Unique
    private static final AtomicLong totalBytesIn = new AtomicLong(0);
    @Unique
    private static final AtomicLong totalBytesOut = new AtomicLong(0);
    
    @Unique
    private int akiasync$adaptiveThreshold = 256;
    @Unique
    private long akiasync$lastAdaptTime = 0;
    @Unique
    private double akiasync$compressionRatio = 0.5;
    
    
    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(int threshold, CallbackInfo ci) {
        if (!initialized) {
            akiasync$init();
        }
        
        if (enabled && useAdaptiveThreshold) {
            
            this.akiasync$adaptiveThreshold = Math.max(adaptiveThresholdMin, 
                Math.min(adaptiveThresholdMax, threshold));
        }
    }
    
    @Unique
    private void akiasync$updateAdaptiveThreshold(int packetSize) {
        long now = System.currentTimeMillis();
        if (now - akiasync$lastAdaptTime < 5000) {
            return;
        }
        akiasync$lastAdaptTime = now;
        
        long totalIn = totalBytesIn.get();
        long totalOut = totalBytesOut.get();
        
        if (totalIn > 0 && totalOut > 0) {
            akiasync$compressionRatio = (double) totalOut / totalIn;
            
            
            if (akiasync$compressionRatio > 0.9) {
                
                akiasync$adaptiveThreshold = Math.min(adaptiveThresholdMax, akiasync$adaptiveThreshold + 32);
            } else if (akiasync$compressionRatio < 0.5) {
                
                akiasync$adaptiveThreshold = Math.max(adaptiveThresholdMin, akiasync$adaptiveThreshold - 32);
            }
        }
    }
    
    @Unique
    private void akiasync$init() {
        if (initialized) {
            return;
        }
        
        try {
            org.virgil.akiasync.mixin.bridge.Bridge bridge =
                org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
            
            if (bridge != null) {
                enabled = bridge.isPacketCompressionOptimizationEnabled();
                useAdaptiveThreshold = bridge.isAdaptiveCompressionThresholdEnabled();
                skipSmallPackets = bridge.isSkipSmallPacketsEnabled();
                skipThreshold = bridge.getSkipSmallPacketsThreshold();
                
                bridge.debugLog("[PacketCompressionOptimization] Initialized: enabled=%s, adaptive=%s, skipSmall=%s",
                    enabled, useAdaptiveThreshold, skipSmallPackets);
            
                    initialized = true;
                }
        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "PacketCompressionOptimization", "init", e);
        }
    }
    
    @Unique
    private void akiasync$recordPacketStats(int bytesIn, int bytesOut, boolean wasCompressed) {
        totalPackets.incrementAndGet();
        totalBytesIn.addAndGet(bytesIn);
        totalBytesOut.addAndGet(bytesOut);
        if (wasCompressed) {
            compressedPackets.incrementAndGet();
        }
    }
    
    @Unique
    private void akiasync$recordSkippedPacket() {
        skippedPackets.incrementAndGet();
    }
    
    @Unique
    private boolean akiasync$shouldSkipSmallPacket(int size) {
        if (!initialized) {
            akiasync$init();
        }
        return enabled && skipSmallPackets && size < skipThreshold;
    }
    
    @Unique
    private int akiasync$getAdaptiveThreshold() {
        return adaptiveThresholdMin + (adaptiveThresholdMax - adaptiveThresholdMin) / 2;
    }
    
    @Unique
    private String akiasync$getStatistics() {
        long total = totalPackets.get();
        if (total == 0) {
            return "PacketCompressionOptimization: No packets processed";
        }
        
        long compressed = compressedPackets.get();
        long skipped = skippedPackets.get();
        long bytesIn = totalBytesIn.get();
        long bytesOut = totalBytesOut.get();
        
        double compressionRate = bytesIn > 0 ? (1.0 - (double) bytesOut / bytesIn) * 100 : 0;
        double skipRate = (double) skipped / total * 100;
        
        return String.format(
            "PacketCompressionOptimization: total=%d, compressed=%d, skipped=%d (%.1f%%), compression=%.1f%%",
            total, compressed, skipped, skipRate, compressionRate
        );
    }
}
