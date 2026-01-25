package org.virgil.akiasync.mixin.mixins.entity.parallel;

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

@Mixin(value = Entity.class, priority = 900)
public abstract class EntityConcurrentMixin {

    @Shadow public abstract Level level();
    @Shadow public abstract BlockPos blockPosition();

    @Unique private static final Object aki$lock = new Object();

    @WrapMethod(method = "getInBlockState")
    private BlockState aki$safeGetInBlockState(Operation<BlockState> original) {
        BlockState state = original.call();

        if (state == null && this.level() instanceof ServerLevel serverLevel) {
            BlockPos pos = this.blockPosition();

            LevelChunk chunk = serverLevel.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
            if (chunk != null) {
                return chunk.getBlockState(pos);
            }

            ChunkAccess access = serverLevel.getChunkSource().getChunk(pos.getX() >> 4, pos.getZ() >> 4, ChunkStatus.FULL, true);
            if (access instanceof LevelChunk levelChunk) {
                return levelChunk.getBlockState(pos);
            }

            return Blocks.AIR.defaultBlockState();
        }

        return state;
    }

    @WrapMethod(method = "addPassenger")
    private void aki$syncAddPassenger(Entity passenger, Operation<Void> original) {
        synchronized (aki$lock) {
            original.call(passenger);
        }
    }

    @WrapMethod(method = "removePassenger")
    private boolean aki$syncRemovePassenger(Entity passenger, Operation<Boolean> original) {
        synchronized (aki$lock) {
            return original.call(passenger);
        }
    }

    @WrapMethod(method = "ejectPassengers")
    private void aki$syncEjectPassengers(Operation<Void> original) {
        synchronized (aki$lock) {
            original.call();
        }
    }
}
