package org.virgil.akiasync.mixin.mixins.chunk.processing;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.virgil.akiasync.mixin.util.BridgeConfigCache;

import java.util.concurrent.atomic.AtomicLong;

@Mixin(LevelChunk.class)
public abstract class FluidPostProcessingOptimizationMixin {

    @Unique
    private static final AtomicLong akiasync$redirectedFluids = new AtomicLong(0);

    @Unique
    private static volatile long akiasync$lastLogTime = 0L;

    @Unique
    private static volatile boolean akiasync$initialized = false;

    @Redirect(
        method = "postProcessGeneration",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/material/FluidState;tick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V"
        ),
        require = 0
    )
    private void akiasync$redirectFluidTick(FluidState fluidState, ServerLevel serverLevel, BlockPos pos, BlockState blockState) {
        if (!BridgeConfigCache.isChunkOptimizationEnabled()) {

            fluidState.tick(serverLevel, pos, blockState);
            return;
        }

        akiasync$initIfNeeded();

        try {
            if (!fluidState.isEmpty()) {

                serverLevel.scheduleTick(pos, fluidState.getType(), 1);
                akiasync$redirectedFluids.incrementAndGet();
            } else {

                fluidState.tick(serverLevel, pos, blockState);
            }
        } catch (Exception e) {

            try {
                fluidState.tick(serverLevel, pos, blockState);
            } catch (Exception fallbackEx) {

            }
        }

        akiasync$logStatistics();
    }

    @Unique
    private static void akiasync$initIfNeeded() {
        if (!akiasync$initialized) {
            akiasync$initialized = true;
            BridgeConfigCache.debugLog("[AkiAsync-FluidOpt] C2ME fluid post-processing optimization enabled");
            BridgeConfigCache.debugLog("[AkiAsync-FluidOpt] Fluid ticks will be scheduled instead of immediate execution");
        }
    }

    @Unique
    private static void akiasync$logStatistics() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - akiasync$lastLogTime > 60000) {
            akiasync$lastLogTime = currentTime;
            long redirected = akiasync$redirectedFluids.get();
            if (redirected > 0) {
                BridgeConfigCache.debugLog(String.format(
                    "[AkiAsync-FluidOpt] Stats - Redirected fluid ticks: %d",
                    redirected
                ));
            }
        }
    }
}
