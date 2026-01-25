package org.virgil.akiasync.mixin.mixins.entity;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Shadow
    public abstract Level level();

    @Shadow
    public abstract BlockPos blockPosition();

    @Unique
    private static final Object akiasync$lock = new Object();

    @WrapMethod(method = "getInBlockState")
    private BlockState akiasync$wrapGetInBlockState(Operation<BlockState> original) {
        BlockState blockState = original.call();

        if (blockState == null && this.level() instanceof ServerLevel serverLevel) {
            BlockPos pos = this.blockPosition();

            LevelChunk chunk = serverLevel.getChunkSource()
                    .getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);

            if (chunk != null) {
                return chunk.getBlockState(pos);
            }

            ChunkAccess access = serverLevel.getChunkSource()
                    .getChunk(pos.getX() >> 4, pos.getZ() >> 4, ChunkStatus.FULL, true);

            if (access instanceof LevelChunk levelChunk) {
                return levelChunk.getBlockState(pos);
            }

            return Blocks.AIR.defaultBlockState();
        }

        return blockState;
    }

    @WrapMethod(method = "addPassenger")
    private void akiasync$addPassenger(Entity passenger, Operation<Void> original) {
        synchronized (akiasync$lock) {
            original.call(passenger);
        }
    }

    @WrapMethod(method = "removePassenger")
    private boolean akiasync$removePassenger(Entity passenger, Operation<Boolean> original) {
        synchronized (akiasync$lock) {
            return original.call(passenger);
        }
    }

    @WrapMethod(method = "ejectPassengers")
    private void akiasync$ejectPassengers(Operation<Void> original) {
        synchronized (akiasync$lock) {
            original.call();
        }
    }
}
