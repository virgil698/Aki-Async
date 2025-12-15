package org.virgil.akiasync.mixin.mixins.world;

import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.virgil.akiasync.mixin.util.TPSTracker;

@Mixin(ServerLevel.class)
public class ServerLevelTimeCompensationMixin {
    
    @Unique
    private static volatile boolean initialized = false;
    @Unique
    private static volatile boolean enabled = false;
    @Unique
    private static volatile double tpsThreshold = 18.0;
    
    @ModifyVariable(
        method = "tickTime",
        at = @At(value = "STORE", ordinal = 0),
        ordinal = 0
    )
    private long compensateTime(long timeOfDay) {
        if (!initialized) {
            akiasync$init();
        }
        
        if (!enabled) return timeOfDay;
        
        try {
            TPSTracker tracker = TPSTracker.getInstance();
            double currentTPS = tracker.getMostAccurateTPS();
            
            if (currentTPS < tpsThreshold) {
                int missedTicks = tracker.getApplicableMissedTicks();
                if (missedTicks > 0) {
                    
                    return timeOfDay + missedTicks;
                }
            }
        } catch (Throwable t) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "ServerLevelTimeCompensation", "compensateTime",
                t instanceof Exception ? (Exception) t : new RuntimeException(t));
        }
        
        return timeOfDay;
    }
    
    @Unique
    private static synchronized void akiasync$init() {
        if (initialized) return;
        
        org.virgil.akiasync.mixin.bridge.Bridge bridge = 
            org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        
        if (bridge != null) {
            enabled = bridge.isSmartLagTimeAccelerationEnabled();
            tpsThreshold = bridge.getSmartLagTPSThreshold();
            
            bridge.debugLog("[AkiAsync] ServerLevelTimeCompensationMixin initialized: enabled=%s, threshold=%.1f",
                enabled, tpsThreshold);
        } else {
            enabled = false;
        }
        
        initialized = true;
    }
}
