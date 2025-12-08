package org.virgil.akiasync.mixin.mixins.lighting;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.virgil.akiasync.mixin.lighting.LightingOptimizationManager;
import org.virgil.akiasync.mixin.util.BridgeConfigCache;

@Mixin(ServerLevel.class)
public class ChunkUnloadLightingCleanupMixin {
    
    @Inject(method = "unload", at = @At("HEAD"))
    private void onChunkUnload(LevelChunk chunk, CallbackInfo ci) {
        if (!LightingOptimizationManager.isInitialized()) {
            return;
        }
        
        try {
            ChunkPos chunkPos = chunk.getPos();
            LightingOptimizationManager.cleanupForChunk(chunkPos);
            
            BridgeConfigCache.debugLog("[AkiAsync-LightEngine] Cleaned up lighting data for chunk " + chunkPos);
        } catch (Exception e) {
            
        }
    }
}
