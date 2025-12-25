package org.virgil.akiasync.mixin.mixins.world;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.virgil.akiasync.mixin.accessor.LevelChunkAccessor;


@Mixin(ServerLevel.class)
public abstract class ServerLevelLightningMixin {
    
    @ModifyVariable(
        method = "tickThunder",
        at = @At(
            value = "INVOKE_ASSIGN",
            target = "Lnet/minecraft/util/RandomSource;nextInt(I)I",
            ordinal = 0
        ),
        ordinal = 0
    )
    private int modifyLightningRandomResult(int originalResult, LevelChunk chunk) {
        RandomSource random = ((ServerLevel)(Object)this).random;
        LevelChunkAccessor accessor = (LevelChunkAccessor) chunk;
        return accessor.invokeShouldDoLightning(random) ? 0 : 1;
    }
}
