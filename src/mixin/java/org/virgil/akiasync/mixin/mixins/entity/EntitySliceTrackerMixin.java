package org.virgil.akiasync.mixin.mixins.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.virgil.akiasync.mixin.util.BridgeConfigCache;
import org.virgil.akiasync.mixin.util.EntitySliceGrid;
import org.virgil.akiasync.mixin.util.EntitySliceGridManager;

@SuppressWarnings("unused")
@Mixin(value = Entity.class, priority = 800)
public abstract class EntitySliceTrackerMixin {
    
    @Unique
    private static volatile boolean enabled = true;
    @Unique
    private static volatile boolean initialized = false;
    
    @Unique
    private int akiasync$lastIntXYZ = -1;
    
    @Inject(method = "move", at = @At("RETURN"))
    private void updateEntitySliceOnMove(CallbackInfo ci) {
        if (!initialized) {
            akiasync$initSliceTracker();
        }
        
        if (!enabled) {
            return;
        }
        
        Entity entity = (Entity) (Object) this;
        Level level = entity.level();
        
        if (!(level instanceof ServerLevel)) {
            return;
        }
        
        EntitySliceGrid sliceGrid = EntitySliceGridManager.getOrCreateSliceGrid(level);
        
        int newIntXYZ = sliceGrid.updateEntitySlice(entity, akiasync$lastIntXYZ);
        
        if (newIntXYZ != -1) {
            akiasync$lastIntXYZ = newIntXYZ;
        }
    }
    
    @Inject(method = "setLevel", at = @At("RETURN"))
    private void initEntitySliceOnAdd(Level level, CallbackInfo ci) {
        if (!initialized) {
            akiasync$initSliceTracker();
        }
        
        if (!enabled || !(level instanceof ServerLevel)) {
            return;
        }
        
        Entity entity = (Entity) (Object) this;
        
        EntitySliceGrid sliceGrid = EntitySliceGridManager.getOrCreateSliceGrid(level);
        
        sliceGrid.addEntity(entity);
        
        akiasync$lastIntXYZ = EntitySliceGrid.calculateIntXYZ(entity);
    }
    
    @Inject(method = "remove", at = @At("HEAD"))
    private void removeEntitySliceOnRemove(CallbackInfo ci) {
        if (!enabled) {
            return;
        }
        
        Entity entity = (Entity) (Object) this;
        Level level = entity.level();
        
        if (!(level instanceof ServerLevel)) {
            return;
        }
        
        EntitySliceGrid sliceGrid = EntitySliceGridManager.getSliceGrid(level);
        if (sliceGrid != null && akiasync$lastIntXYZ != -1) {
            sliceGrid.removeEntity(entity, akiasync$lastIntXYZ);
        }
    }
    
    @Unique
    private static synchronized void akiasync$initSliceTracker() {
        if (initialized) return;
        
        org.virgil.akiasync.mixin.bridge.Bridge bridge = BridgeConfigCache.getBridge();
        
        if (bridge != null) {
            boolean isFolia = bridge.isFoliaEnvironment();
            
            if (isFolia) {
                enabled = false;
                BridgeConfigCache.debugLog("[AkiAsync] EntitySliceTrackerMixin disabled in Folia environment");
            } else {
                enabled = bridge.isCollisionOptimizationEnabled();
                BridgeConfigCache.debugLog("[AkiAsync] EntitySliceTrackerMixin initialized: enabled=" + enabled);
            }
        }
        
        initialized = true;
    }
}
