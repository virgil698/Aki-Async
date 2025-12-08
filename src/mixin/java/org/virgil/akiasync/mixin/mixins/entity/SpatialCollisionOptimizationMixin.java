package org.virgil.akiasync.mixin.mixins.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.virgil.akiasync.mixin.util.BridgeConfigCache;
import org.virgil.akiasync.mixin.util.EntitySliceGrid;
import org.virgil.akiasync.mixin.util.EntitySliceGridManager;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

@SuppressWarnings("unused")
@Mixin(value = Level.class, priority = 800)
public abstract class SpatialCollisionOptimizationMixin {
    
    @Unique
    private static volatile boolean enabled = true;
    @Unique
    private static volatile boolean initialized = false;
    
    @Inject(
        method = "getEntities(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;",
        at = @At("HEAD"),
        cancellable = true
    )
    private void useSpatialIndexForCollision(
        Entity except,
        AABB box,
        Predicate<? super Entity> predicate,
        CallbackInfoReturnable<List<Entity>> cir
    ) {
        if (!initialized) {
            akiasync$initSpatialOptimization();
        }
        
        if (!enabled) {
            return;
        }
        
        Level level = (Level) (Object) this;
        if (!(level instanceof ServerLevel)) {
            return;
        }
        
        EntitySliceGrid sliceGrid = EntitySliceGridManager.getSliceGrid(level);
        
        org.virgil.akiasync.mixin.bridge.Bridge bridge = BridgeConfigCache.getBridge();
        
        if (sliceGrid == null || sliceGrid.isEmpty()) {
            
            if (bridge != null && bridge.isTNTDebugEnabled()) {
                bridge.debugLog("[AkiAsync-Collision] Slice grid is null or empty, using vanilla logic");
            }
            return;
        }
        
        try {
            
            List<Entity> candidates = sliceGrid.queryRange(box);
            
            if (bridge != null && bridge.isTNTDebugEnabled()) {
                bridge.debugLog("[AkiAsync-Collision] Found %d candidates from slice grid", candidates.size());
            }
            
            if (candidates.isEmpty()) {
                
                if (bridge != null && bridge.isTNTDebugEnabled()) {
                    bridge.debugLog("[AkiAsync-Collision] No candidates (possibly cross-chunk query), falling back to vanilla");
                }
                return;
            }
            
            List<Entity> result = new ArrayList<>(candidates.size());
            for (Entity entity : candidates) {
                
                if (entity == null || entity.isRemoved()) {
                    continue;
                }
                
                if (entity == except) {
                    continue;
                }
                
                if (!entity.getBoundingBox().intersects(box)) {
                    continue;
                }
                
                if (predicate != null && !predicate.test(entity)) {
                    continue;
                }
                
                result.add(entity);
            }
            
            if (bridge != null && bridge.isDebugLoggingEnabled()) {
                BridgeConfigCache.debugLog("[AkiAsync-Collision] Slice grid filtered: " + 
                    result.size() + " / " + candidates.size() + " candidates");
            }
            
            cir.setReturnValue(result);
            
        } catch (Exception e) {
            
            if (bridge != null) {
                bridge.errorLog("[AkiAsync-Collision] Slice grid query failed: " + e.getMessage());
            }
        }
    }
    
    @Unique
    private static synchronized void akiasync$initSpatialOptimization() {
        if (initialized) return;
        
        org.virgil.akiasync.mixin.bridge.Bridge bridge = BridgeConfigCache.getBridge();
        
        if (bridge != null) {
            enabled = bridge.isCollisionOptimizationEnabled();
            BridgeConfigCache.debugLog("[AkiAsync] SpatialCollisionOptimizationMixin initialized: enabled=" + enabled);
        }
        
        initialized = true;
    }
}
