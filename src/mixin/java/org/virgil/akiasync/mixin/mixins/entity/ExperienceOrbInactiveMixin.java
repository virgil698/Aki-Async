package org.virgil.akiasync.mixin.mixins.entity;

import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@SuppressWarnings("unused")
@Mixin(ExperienceOrb.class)
public abstract class ExperienceOrbInactiveMixin {

    @Shadow
    public int age;

    @Unique
    private static volatile boolean enabled;
    @Unique
    private static volatile double inactiveRange;
    @Unique
    private static volatile int mergeInterval;
    @Unique
    private static volatile boolean initialized = false;

    @Unique
    private static final int LIFETIME = 6000; 

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void inactiveTick(CallbackInfo ci) {
        if (!initialized) {
            akiasync$initInactiveTick();
        }
        if (!enabled) return;

        ExperienceOrb self = (ExperienceOrb) (Object) this;

        if (akiasync$shouldUseInactiveTick(self)) {
            akiasync$performInactiveTick(self);
            ci.cancel(); 
        }
    }

    @Unique
    private boolean akiasync$shouldUseInactiveTick(ExperienceOrb self) {
        
        return !akiasync$hasNearbyPlayer(self, inactiveRange);
    }

    @Unique
    private void akiasync$performInactiveTick(ExperienceOrb self) {
        
        age++;

        if (age >= LIFETIME) {
            ((net.minecraft.world.entity.Entity) self).discard();
            return;
        }

        if (age % mergeInterval == 0) {
            akiasync$tryQuickMerge(self);
        }
    }

    @Unique
    private void akiasync$tryQuickMerge(ExperienceOrb self) {
        try {
            
            AABB box = self.getBoundingBox().inflate(1.5);
            List<ExperienceOrb> nearby = self.level().getEntitiesOfClass(
                ExperienceOrb.class,
                box,
                e -> e != self && !e.isRemoved()
            );

            for (ExperienceOrb other : nearby) {
                if (akiasync$canMerge(self, other)) {
                    
                    int selfValue = akiasync$getValue(self);
                    int otherValue = akiasync$getValue(other);
                    akiasync$setValue(self, selfValue + otherValue);
                    ((net.minecraft.world.entity.Entity) other).discard();
                    
                    if (akiasync$getValue(self) > 2477) { 
                        break;
                    }
                }
            }
        } catch (Throwable t) {
            
        }
    }

    @Unique
    private boolean akiasync$canMerge(ExperienceOrb self, ExperienceOrb other) {
        
        return other != null && !other.isRemoved() && akiasync$getValue(other) > 0;
    }

    @Unique
    private int akiasync$getValue(ExperienceOrb orb) {
        try {
            
            return orb.getValue();
        } catch (Exception e) {
            return 0;
        }
    }

    @Unique
    private void akiasync$setValue(ExperienceOrb orb, int newValue) {
        try {
            
            orb.setValue(newValue);
        } catch (Exception e) {
            
        }
    }

    @Unique
    private boolean akiasync$hasNearbyPlayer(ExperienceOrb self, double range) {
        try {
            AABB searchBox = self.getBoundingBox().inflate(range);
            List<Player> nearbyPlayers = self.level().getEntitiesOfClass(
                Player.class,
                searchBox
            );
            return !nearbyPlayers.isEmpty();
        } catch (Throwable t) {
            return true; 
        }
    }

    @Unique
    private static synchronized void akiasync$initInactiveTick() {
        if (initialized) return;

        org.virgil.akiasync.mixin.bridge.Bridge bridge =
            org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();

        if (bridge != null) {
            enabled = bridge.isExperienceOrbInactiveTickEnabled();
            inactiveRange = bridge.getExperienceOrbInactiveRange();
            mergeInterval = bridge.getExperienceOrbInactiveMergeInterval();
        } else {
            enabled = true;
            inactiveRange = 32.0;
            mergeInterval = 100;
        }

        initialized = true;

        if (bridge != null) {
            bridge.debugLog("[AkiAsync] ExperienceOrbInactiveMixin initialized: enabled=" + enabled +
                ", inactiveRange=" + inactiveRange +
                ", mergeInterval=" + mergeInterval);
        }
    }
}
