package org.virgil.akiasync.mixin.mixins.chunk.loading;

import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkMap.class)
public class VMPChunkMapMixin {

    @Shadow
    private ServerLevel level;

    @Unique
    private static final Object akiasync$chunkMapLock = new Object();

    @Inject(method = "addEntity", at = @At("HEAD"))
    private void akiasync$addEntityHead(CallbackInfo ci) {

    }

    @Inject(method = "removeEntity", at = @At("HEAD"))
    private void akiasync$removeEntityHead(CallbackInfo ci) {

    }
}
