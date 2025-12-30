package org.virgil.akiasync.mixin.mixins.network;

import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@SuppressWarnings("unused")
@Mixin(ClientboundMoveEntityPacket.class)
public abstract class ZeroMovementPacketFilterMixin {
    
    @Shadow public abstract short getXa();
    @Shadow public abstract short getYa();
    @Shadow public abstract short getZa();
    @Shadow public abstract float getYRot();
    @Shadow public abstract float getXRot();
    
    @Unique
    private static volatile boolean initialized = false;
    @Unique
    private static volatile boolean enabled = true;
    @Unique
    private static volatile boolean strictMode = false;
    
    @Unique
    private static volatile long totalPackets = 0;
    @Unique
    private static volatile long filteredPackets = 0;
    @Unique
    private static volatile long zeroMovePackets = 0;
    @Unique
    private static volatile long zeroRotatePackets = 0;
    
    @Inject(
        method = "write(Lnet/minecraft/network/FriendlyByteBuf;)V",
        at = @At("HEAD"),
        cancellable = true,
        require = 0
    )
    private void filterZeroMovement(net.minecraft.network.FriendlyByteBuf buf, CallbackInfo ci) {
        if (!initialized) {
            akiasync$init();
        }
        
        if (!enabled) {
            return;
        }
        
        totalPackets++;
        
        try {
            short xa = this.getXa();
            short ya = this.getYa();
            short za = this.getZa();
            
            float yRot = this.getYRot();
            float xRot = this.getXRot();
            
            boolean isZeroMove = (xa == 0 && ya == 0 && za == 0);
            boolean isZeroRotate = (Math.abs(yRot) < 0.01f && Math.abs(xRot) < 0.01f);
            
            if (strictMode) {
                if (isZeroMove && isZeroRotate) {
                    filteredPackets++;
                    ci.cancel();
                    return;
                }
            } else {
                if (isZeroMove) {
                    zeroMovePackets++;
                    filteredPackets++;
                    ci.cancel();
                    return;
                }
                
                if (isZeroRotate && Math.abs(xa) <= 1 && Math.abs(ya) <= 1 && Math.abs(za) <= 1) {
                    zeroRotatePackets++;
                    filteredPackets++;
                    ci.cancel();
                    return;
                }
            }
            
        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "ZeroMovementPacketFilter", "write", e);
        }
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
                enabled = bridge.isSkipZeroMovementPacketsEnabled();
                strictMode = bridge.isSkipZeroMovementPacketsStrictMode();
                
                bridge.debugLog("[ZeroMovementPacketFilter] Initialized - enabled=%s, strictMode=%s", 
                    enabled, strictMode);
            }
        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "ZeroMovementPacketFilter", "init", e);
        }
        
        initialized = true;
    }
    
    @Unique
    private static String akiasync$getStatistics() {
        if (totalPackets == 0) {
            return "ZeroMovementPacketFilter: No packets processed yet";
        }
        
        double filterRate = (double) filteredPackets / totalPackets * 100.0;
        
        return String.format(
            "ZeroMovementPacketFilter: total=%d, filtered=%d (%.2f%%), zeroMove=%d, zeroRotate=%d",
            totalPackets, filteredPackets, filterRate, zeroMovePackets, zeroRotatePackets
        );
    }
    
    @Unique
    private static void akiasync$resetStatistics() {
        totalPackets = 0;
        filteredPackets = 0;
        zeroMovePackets = 0;
        zeroRotatePackets = 0;
    }
    
    @Unique
    private static void akiasync$reload() {
        initialized = false;
    }
}
