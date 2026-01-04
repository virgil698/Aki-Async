package org.virgil.akiasync.mixin.mixins.pathfinding;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.virgil.akiasync.mixin.pathfinding.AsyncPathProcessor;
import org.virgil.akiasync.mixin.pathfinding.SharedPathCache;
import org.virgil.akiasync.mixin.util.BridgeConfigCache;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathFinder;

@Mixin(PathNavigation.class)
public abstract class PathNavigationAsyncMixin {

    @Shadow
    protected Path path;
    
    @Shadow
    protected Mob mob;
    
    @Unique
    private volatile boolean akiasync$isComputingPath = false;

    @Redirect(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/pathfinder/PathFinder;" +
                    "findPath(Lnet/minecraft/world/level/pathfinder/PathNavigationRegion;" +
                    "Lnet/minecraft/world/entity/Mob;" +
                    "Ljava/util/Set;FIF)" +
                    "Lnet/minecraft/world/level/pathfinder/Path;"
        ),
        require = 0
    )
    private Path handler$nonBlockingAsyncFindPath(
            PathFinder finder,
            Object region,
            Mob mob,
            Set<BlockPos> targets,
            float maxRange,
            int accuracy,
            float depth) {

        if (mob.level().isClientSide) {
            return akiasync$computePathSync(finder, region, mob, targets, maxRange, accuracy, depth);
        }

        if (!AsyncPathProcessor.isEnabled()) {
            return akiasync$computePathSync(finder, region, mob, targets, maxRange, accuracy, depth);
        }

        Path cachedPath = akiasync$tryGetCachedPath(mob.blockPosition(), targets);
        if (cachedPath != null) {
            AsyncPathProcessor.recordCacheHit();
            return cachedPath;
        }

        Path oldPath = this.path;
        if (oldPath != null && oldPath.canReach() && !oldPath.isDone()) {
            
            akiasync$computePathAsync(finder, region, mob, targets, maxRange, accuracy, depth);
            return oldPath;
        }

        if (akiasync$isComputingPath) {
            if (this.path != null) {
                return this.path;
            }
        }

        return akiasync$computePathSync(finder, region, mob, targets, maxRange, accuracy, depth);
    }

    @Unique
    private void akiasync$computePathAsync(
            PathFinder finder,
            Object region,
            Mob mob,
            Set<BlockPos> targets,
            float maxRange,
            int accuracy,
            float depth) {
        
        if (akiasync$isComputingPath) {
            return;
        }
        
        akiasync$isComputingPath = true;
        BlockPos startPos = mob.blockPosition();

        CompletableFuture.runAsync(() -> {
            try {
                
                Path newPath = akiasync$computePathSync(finder, region, mob, targets, maxRange, accuracy, depth);
                
                if (newPath != null && newPath.canReach()) {
                    
                    akiasync$cacheComputedPath(startPos, targets, newPath);
                    
                    MinecraftServer server = mob.getServer();
                    if (server != null) {
                        try {
                            server.execute(() -> {
                                
                                if (!mob.isRemoved() && newPath.canReach()) {
                                    this.path = newPath;
                                }
                                akiasync$isComputingPath = false;
                            });
                        } catch (UnsupportedOperationException ex) {
                            if (!mob.isRemoved() && newPath.canReach()) {
                                this.path = newPath;
                            }
                            akiasync$isComputingPath = false;
                        }
                    } else {
                        akiasync$isComputingPath = false;
                    }
                } else {
                    akiasync$isComputingPath = false;
                }
            } catch (Exception e) {
                akiasync$isComputingPath = false;
                
                BridgeConfigCache.debugLog("[AkiAsync-PathNav] Async path computation failed: " + e.getMessage());
            }
        }, AsyncPathProcessor.getExecutor());
    }

    @Unique
    private synchronized Path akiasync$computePathSync(
            PathFinder finder,
            Object region,
            Mob mob,
            Set<BlockPos> targets,
            float maxRange,
            int accuracy,
            float depth) {
        try {
            
            return finder.findPath(
                (net.minecraft.world.level.PathNavigationRegion) region,
                mob,
                targets,
                maxRange,
                accuracy,
                depth
            );
        } catch (Exception e) {
            BridgeConfigCache.debugLog("[AkiAsync-PathNav] Sync path computation failed: " + e.getMessage());
            return null;
        }
    }

    @Unique
    private Path akiasync$tryGetCachedPath(BlockPos start, Set<BlockPos> targets) {
        if (targets == null || targets.isEmpty()) {
            return null;
        }
        
        BlockPos target = targets.iterator().next();
        if (target == null || start.equals(target)) {
            return null;
        }
        
        return SharedPathCache.getCachedPath(start, target);
    }

    @Unique
    private void akiasync$cacheComputedPath(BlockPos start, Set<BlockPos> targets, Path path) {
        if (targets == null || targets.isEmpty() || path == null) {
            return;
        }
        
        BlockPos target = targets.iterator().next();
        if (target != null && !start.equals(target)) {
            SharedPathCache.cachePath(start, target, path);
        }
    }
}
