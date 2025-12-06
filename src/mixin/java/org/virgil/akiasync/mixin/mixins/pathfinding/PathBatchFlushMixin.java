package org.virgil.akiasync.mixin.mixins.pathfinding;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.virgil.akiasync.mixin.pathfinding.AsyncPathProcessor;
import org.virgil.akiasync.mixin.poi.BatchPoiManager;

import net.minecraft.server.level.ServerLevel;

@Mixin(value = ServerLevel.class, priority = 900)
public class PathBatchFlushMixin {
    
    @Inject(method = "tick", at = @At("RETURN"), require = 0)
    private void flushBatchTasks(CallbackInfo ci) {

        AsyncPathProcessor.flushPendingPaths();
        
        ServerLevel level = (ServerLevel) (Object) this;
        BatchPoiManager.clearCache(level);
    }
}
