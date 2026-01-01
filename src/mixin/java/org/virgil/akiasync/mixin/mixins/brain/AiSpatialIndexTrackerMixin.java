package org.virgil.akiasync.mixin.mixins.brain;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.virgil.akiasync.mixin.brain.core.AiSpatialIndex;
import org.virgil.akiasync.mixin.brain.core.AiSpatialIndexManager;
import org.virgil.akiasync.mixin.bridge.Bridge;
import org.virgil.akiasync.mixin.bridge.BridgeManager;
import org.virgil.akiasync.mixin.util.ExceptionHandler;

@Mixin(value = LivingEntity.class, priority = 800)
public abstract class AiSpatialIndexTrackerMixin {
    
    @Unique
    private static volatile boolean initialized = false;
    
    @Unique
    private static volatile boolean enabled = true;
    
    @Unique
    private BlockPos akiasync$lastIndexedPos = null;
    
    @Inject(method = "tick", at = @At("HEAD"))
    private void akiasync$updateSpatialIndex(CallbackInfo ci) {
        if (!initialized) {
            akiasync$init();
        }
        
        if (!enabled) return;
        
        LivingEntity entity = (LivingEntity) (Object) this;
        
        if (entity.level().isClientSide) return;
        
        ServerLevel level = (ServerLevel) entity.level();
        AiSpatialIndex index = AiSpatialIndexManager.getIndex(level);
        
        if (index == null || !index.isEnabled()) return;
        
        BlockPos currentPos = entity.blockPosition();
        
        if (akiasync$lastIndexedPos == null) {
            index.addEntity(entity);
            akiasync$lastIndexedPos = currentPos;
            return;
        }
        
        if (!currentPos.equals(akiasync$lastIndexedPos)) {
            
            index.updateEntity(entity, akiasync$lastIndexedPos, currentPos);
            akiasync$lastIndexedPos = currentPos;
        }
    }
    
    @Inject(method = "remove", at = @At("TAIL"))
    private void akiasync$removeFromSpatialIndex(CallbackInfo ci) {
        akiasync$removeFromSpatialIndexCommon();
    }
    
    @Unique
    private void akiasync$removeFromSpatialIndexCommon() {
        if (!enabled) return;
        
        LivingEntity entity = (LivingEntity) (Object) this;
        
        if (entity.level().isClientSide) return;
        
        ServerLevel level = (ServerLevel) entity.level();
        AiSpatialIndex index = AiSpatialIndexManager.getIndex(level);
        
        if (index != null && index.isEnabled()) {
            index.removeEntity(entity);
        }
        
        try {
            Bridge bridge = BridgeManager.getBridge();
            if (bridge != null) {
                bridge.clearEntityThrottleCache(entity.getId());
            
                initialized = true;
            }
        } catch (Exception e) {
            ExceptionHandler.handleCleanup("AiSpatialIndexTracker", "entityThrottleCache", e);
        }
        
        akiasync$lastIndexedPos = null;
    }
    
    @Unique
    private static synchronized void akiasync$init() {
        if (initialized) return;
        
        Bridge bridge = BridgeManager.getBridge();
        if (bridge != null) {
            
            enabled = bridge.isAiSpatialIndexEnabled();
            
            if (enabled) {
                bridge.debugLog("[AkiAsync] AI Spatial Index Tracker initialized: enabled=true");
            
                initialized = true;
            }
        } else {
            enabled = false; 
        }
    }
}
