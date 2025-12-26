package org.virgil.akiasync.mixin.mixins.entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

@Mixin(Mob.class)
public class MobDespawnOptimizeMixin {
    
    @Unique
    private static volatile boolean cached_enabled = false;
    
    @Unique
    private static volatile boolean initialized = false;
    
    @Unique
    private static volatile int cached_checkInterval = 20;
    
    @Unique
    private int aki$despawnCheckCounter = 0;
    
    @Inject(method = "checkDespawn", at = @At("HEAD"), cancellable = true, require = 0)
    private void optimizeCheckDespawn(CallbackInfo ci) {
        if (!initialized) {
            aki$initDespawnOptimization();
        }
        
        if (!cached_enabled) {
            return;
        }
        
        Mob mob = (Mob) (Object) this;
        
        if (mob.isPersistenceRequired() || mob.requiresCustomPersistence()) {
            return;
        }
        
        if (mob.hasCustomName()) {
            return;
        }
        
        aki$despawnCheckCounter++;
        if (aki$despawnCheckCounter % cached_checkInterval != 0) {
            ci.cancel();
            return;
        }
        
        ServerLevel level = (ServerLevel) mob.level();
        Player nearestPlayer = level.getNearestPlayer(mob, 32.0);
        if (nearestPlayer != null) {

            if (aki$despawnCheckCounter % (cached_checkInterval * 2) != 0) {
                ci.cancel();
            }
        }
    }
    
    @Unique
    private static void aki$initDespawnOptimization() {
        org.virgil.akiasync.mixin.bridge.Bridge bridge =
            org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        
        if (bridge != null) {
            cached_enabled = bridge.isMobDespawnOptimizationEnabled();
            cached_checkInterval = bridge.getMobDespawnCheckInterval();
            
            bridge.debugLog("[AkiAsync] MobDespawnOptimizeMixin initialized: enabled=" + 
                cached_enabled + " | interval=" + cached_checkInterval);
        } else {
            cached_enabled = false;
        }
        
        initialized = true;
    }
}
