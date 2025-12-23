package org.virgil.akiasync.mixin.mixins.world;

import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.virgil.akiasync.mixin.brain.core.AiSpatialIndexManager;
import org.virgil.akiasync.mixin.poi.PoiSpatialIndexManager;
import org.virgil.akiasync.mixin.poi.BatchPoiManager;
import org.virgil.akiasync.mixin.bridge.Bridge;
import org.virgil.akiasync.mixin.bridge.BridgeManager;
import org.virgil.akiasync.mixin.util.ExceptionHandler;

@Mixin(ServerLevel.class)
public class WorldUnloadCleanupMixin {
    
    @Inject(method = "close", at = @At("HEAD"))
    private void akiasync$cleanupOnWorldClose(CallbackInfo ci) {
        ServerLevel level = (ServerLevel) (Object) this;
        
        try {
            org.virgil.akiasync.mixin.util.GlobalCacheCleanup.cleanupServerLevel(level);
            
            AiSpatialIndexManager.removeIndex(level);
            
            PoiSpatialIndexManager.removeIndex(level);
            
            BatchPoiManager.clearLevelCache(level);
            
            org.virgil.akiasync.mixin.util.EntitySliceGridManager.removeSliceGrid(level);
            
            try {
                org.virgil.akiasync.mixin.async.explosion.density.SakuraBlockDensityCache.clearLevelCache(level);
            } catch (Exception e) {
                ExceptionHandler.handleCleanup("WorldUnloadCleanup", "SakuraBlockDensityCache", e);
            }
            
            try {
                org.virgil.akiasync.mixin.util.collision.CollisionBlockCache.clearLevelCache(level);
            } catch (Exception e) {
                ExceptionHandler.handleCleanup("WorldUnloadCleanup", "CollisionBlockCache", e);
            }
            
            try {
                org.virgil.akiasync.mixin.util.collision.RayCollisionDetectorManager.clearAll();
            } catch (Exception e) {
                ExceptionHandler.handleCleanup("WorldUnloadCleanup", "RayCollisionDetector", e);
            }
            
            Bridge bridge = BridgeManager.getBridge();
            if (bridge != null) {
                bridge.debugLog("[AkiAsync] Cleaned up caches for world: " + level.dimension().location());
            }
        } catch (Exception e) {
            Bridge bridge = BridgeManager.getBridge();
            if (bridge != null) {
                bridge.debugLog("[AkiAsync] Error cleaning up world caches: " + e.getMessage());
            }
        }
    }
}
