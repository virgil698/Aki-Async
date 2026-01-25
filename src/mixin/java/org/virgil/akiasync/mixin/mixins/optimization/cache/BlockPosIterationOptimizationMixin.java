package org.virgil.akiasync.mixin.mixins.optimization.cache;

import it.unimi.dsi.fastutil.longs.LongList;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.virgil.akiasync.mixin.util.BlockPosIterationCache;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Mixin(BlockPos.class)
public class BlockPosIterationOptimizationMixin {

    @Unique
    private static volatile boolean initialized = false;
    @Unique
    private static volatile boolean enabled = true;
    @Unique
    private static BlockPosIterationCache cache;

    @Inject(
        method = "withinManhattan",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void optimizeWithinManhattan(
        BlockPos center,
        int xRange,
        int yRange,
        int zRange,
        CallbackInfoReturnable<Iterable<BlockPos>> cir
    ) {

        if (BlockPosIterationCache.isFillingCache()) {
            return;
        }

        if (!initialized) {
            akiasync$initCache();
        }

        if (!enabled) {
            return;
        }

        try {

            LongList cachedPositions = cache.getOrCompute(xRange, yRange, zRange);

            List<BlockPos> positions = new ArrayList<>(cachedPositions.size());

            for (int i = 0; i < cachedPositions.size(); i++) {
                long relativePos = cachedPositions.getLong(i);
                BlockPos relative = BlockPos.of(relativePos);

                BlockPos absolute = center.offset(relative.getX(), relative.getY(), relative.getZ());
                positions.add(absolute);
            }

            cir.setReturnValue(positions);

        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "BlockPosIterationOptimization", "optimizeWithinManhattan", e);
        }
    }

    @Unique
    private static synchronized void akiasync$initCache() {
        if (initialized) {
            return;
        }

        try {
            org.virgil.akiasync.mixin.bridge.Bridge bridge =
                org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();

            if (bridge != null) {
                enabled = true;

                cache = new BlockPosIterationCache(256);

                bridge.debugLog("[BlockPosIterationOptimization] Initialized: enabled=%s, capacity=256",
                    enabled);

                    initialized = true;
                } else {

                cache = new BlockPosIterationCache(256);
            }
        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "BlockPosIterationOptimization", "initCache", e);

            cache = new BlockPosIterationCache(256);
        }
    }

    @Unique
    private static String akiasync$getCacheStats() {
        if (cache == null) {
            return "Cache not initialized";
        }
        return String.format("BlockPos Cache: size=%d", cache.size());
    }
}
