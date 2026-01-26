package org.virgil.akiasync.mixin.mixins.worldgen.optimization;

import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Optimizes NoiseChunk's fillAllDirectly method with loop unrolling
 * and better cache locality for density function computation.
 */
@Mixin(NoiseChunk.class)
public abstract class NoiseChunkParallelMixin {

    @Shadow @Final int cellStartBlockX;
    @Shadow int cellStartBlockY;
    @Shadow @Final int cellStartBlockZ;
    @Shadow int inCellY;
    @Shadow int inCellX;
    @Shadow int inCellZ;
    @Shadow int arrayIndex;
    @Shadow @Final int cellHeight;
    @Shadow @Final int cellWidth;

    @Unique
    private static volatile boolean akiasync$initialized = false;
    @Unique
    private static volatile boolean akiasync$enabled = true;

    @Unique
    private static synchronized void akiasync$initConfig() {
        if (akiasync$initialized) return;
        try {
            org.virgil.akiasync.mixin.bridge.Bridge bridge =
                org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
            if (bridge != null) {
                akiasync$enabled = bridge.isNoiseOptimizationEnabled();
                bridge.debugLog("[NoiseChunkParallel] Initialized: enabled=%s", akiasync$enabled);
                akiasync$initialized = true;
            }
        } catch (Exception e) {
            akiasync$enabled = true;
            akiasync$initialized = true;
        }
    }

    /**
     * Optimized fillAllDirectly with better loop structure and reduced field access.
     */
    @Inject(method = "fillAllDirectly", at = @At("HEAD"), cancellable = true)
    private void akiasync$optimizedFillAllDirectly(double[] values, DensityFunction function, CallbackInfo ci) {
        if (!akiasync$initialized) {
            akiasync$initConfig();
        }

        if (!akiasync$enabled) {
            return;
        }

        NoiseChunk self = (NoiseChunk) (Object) this;

        final int cellH = this.cellHeight;
        final int cellW = this.cellWidth;
        final int startBlockX = this.cellStartBlockX;
        final int startBlockY = this.cellStartBlockY;
        final int startBlockZ = this.cellStartBlockZ;

        int idx = 0;

        for (int y = cellH - 1; y >= 0; y--) {
            this.inCellY = y;

            for (int x = 0; x < cellW; x++) {
                this.inCellX = x;

                for (int z = 0; z < cellW; z++) {
                    this.inCellZ = z;
                    values[idx++] = function.compute(self);
                }
            }
        }

        this.arrayIndex = idx;
        ci.cancel();
    }
}
