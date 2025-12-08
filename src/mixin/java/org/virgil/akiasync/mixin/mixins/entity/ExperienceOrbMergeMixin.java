package org.virgil.akiasync.mixin.mixins.entity;

import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.virgil.akiasync.mixin.util.BridgeConfigCache;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ExperienceOrb.class)
public abstract class ExperienceOrbMergeMixin {

    @Shadow
    public int count;

    @Shadow
    public abstract int getValue();

    @Unique
    private static volatile boolean fixEnabled = true;
    @Unique
    private static volatile int maxAllowedCount = 100; 
    @Unique
    private static volatile boolean initialized = false;
    
    @Unique
    private int aki$lastValidCount = 1;

    @Inject(method = "playerTouch", at = @At("HEAD"))
    private void aki$validateCountBeforePickup(Player entity, CallbackInfo ci) {
        if (!initialized) {
            aki$initMergeFix();
        }

        if (!fixEnabled) {
            return;
        }

        if (count < 0) {
            
            if (aki$isDebugEnabled()) {
                aki$debugLog("[ExperienceOrbFix] Detected negative count: " + count + ", resetting to 1");
            }
            count = 1;
        } else if (count > maxAllowedCount) {
            
            if (aki$isDebugEnabled()) {
                aki$debugLog("[ExperienceOrbFix] Detected excessive count: " + count + ", limiting to " + maxAllowedCount);
            }
            count = maxAllowedCount;
        }
        
        aki$lastValidCount = count;
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void aki$monitorCountChanges(CallbackInfo ci) {
        if (!initialized) {
            aki$initMergeFix();
        }

        if (!fixEnabled) {
            return;
        }

        ExperienceOrb self = (ExperienceOrb) (Object) this;

        if (self.tickCount % 5 == 0) {
            aki$tryMergeNearby(self);
        }

        if (self.tickCount % 20 == 0) {
            
            if (count > aki$lastValidCount * 2 && count > 10) {
                
                if (aki$isDebugEnabled()) {
                    aki$debugLog("[ExperienceOrbFix] Detected rapid count increase: " + aki$lastValidCount + " -> " + count + ", value: " + getValue());
                }
                
                count = Math.min(count, aki$lastValidCount * 2);
            }
            
            if (count > 0 && count <= maxAllowedCount) {
                aki$lastValidCount = count;
            }
        }

        if (count > maxAllowedCount) {
            if (aki$isDebugEnabled()) {
                aki$debugLog("[ExperienceOrbFix] Force limiting count from " + count + " to " + maxAllowedCount);
            }
            count = maxAllowedCount;
        }
    }
    
    @Unique
    private void aki$tryMergeNearby(ExperienceOrb self) {
        
        if (!self.level().isClientSide && !Thread.currentThread().getName().contains("Server thread")) {
            return;
        }
        
        try {
            java.util.List<ExperienceOrb> nearby = self.level().getEntitiesOfClass(
                ExperienceOrb.class,
                self.getBoundingBox().inflate(1.5),
                orb -> orb != self && !orb.isRemoved()
            );
            
            if (nearby.size() >= 5) {
                int mergedCount = 0;
                for (ExperienceOrb other : nearby) {
                    if (self.isRemoved()) break;
                    if (mergedCount >= 3) break; 
                    
                    if (other.isRemoved()) continue;
                    
                    int selfValue = self.getValue();
                    int otherValue = other.getValue();
                    
                    if (selfValue == otherValue && count + other.count <= maxAllowedCount) {
                        count += other.count;
                        other.discard();
                        mergedCount++;
                    }
                }
            }
        } catch (Exception e) {
            
        }
    }

    @Unique
    private static synchronized void aki$initMergeFix() {
        if (initialized) return;

        org.virgil.akiasync.mixin.bridge.Bridge bridge =
            org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();

        if (bridge != null) {
            
            fixEnabled = true;
            
            maxAllowedCount = 100;

            BridgeConfigCache.debugLog("[AkiAsync] ExperienceOrbMergeMixin initialized: fixEnabled=" + fixEnabled + ", maxAllowedCount=" + maxAllowedCount);
        }

        initialized = true;
    }

    @Unique
    private boolean aki$isDebugEnabled() {
        return BridgeConfigCache.isDebugLoggingEnabled();
    }

    @Unique
    private void aki$debugLog(String message) {
        BridgeConfigCache.debugLog(message);
    }
}
