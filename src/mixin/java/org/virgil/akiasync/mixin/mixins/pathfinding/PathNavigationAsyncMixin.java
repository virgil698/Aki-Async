package org.virgil.akiasync.mixin.mixins.pathfinding;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathFinder;

@Mixin(PathNavigation.class)
public class PathNavigationAsyncMixin {
    
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
    private Path handler$zzi000$asyncFindPath(
            PathFinder finder,
            Object region,
            Mob mob,
            Set<BlockPos> targets,
            float maxRange,
            int accuracy,
            float depth) {
        
        if (mob.level().isClientSide) {
            try {
                java.lang.reflect.Method findPathMethod = finder.getClass().getMethod("findPath",
                    Class.forName("net.minecraft.world.level.pathfinder.PathNavigationRegion"),
                    Mob.class, Set.class, float.class, int.class, float.class);
                return (Path) findPathMethod.invoke(finder, region, mob, targets, maxRange, accuracy, depth);
            } catch (Exception e) {
                return null;
            }
        }
        
        long chunkKey = getChunkKey(mob.blockPosition());
        CompletableFuture<Path> future = new CompletableFuture<>();
        
        try {
            Object level = mob.level();
            java.lang.reflect.Method getSchedulerMethod = 
                level.getClass().getMethod("moonrise$getChunkTaskScheduler");
            Object scheduler = getSchedulerMethod.invoke(level);
            
            java.lang.reflect.Field radiusSchedulerField = 
                scheduler.getClass().getDeclaredField("radiusAwareScheduler");
            radiusSchedulerField.setAccessible(true);
            Object radiusScheduler = radiusSchedulerField.get(scheduler);
            
            if (radiusScheduler == null) {
                try {
                    java.lang.reflect.Method findPathMethod = finder.getClass().getMethod("findPath",
                        Class.forName("net.minecraft.world.level.pathfinder.PathNavigationRegion"),
                        Mob.class, Set.class, float.class, int.class, float.class);
                    return (Path) findPathMethod.invoke(finder, region, mob, targets, maxRange, accuracy, depth);
                } catch (Exception e) {
                    return null;
                }
            }
            
            java.lang.reflect.Method executeMethod = radiusScheduler.getClass().getMethod("execute",
                Object.class, long.class, Runnable.class);
            
            executeMethod.invoke(radiusScheduler, level, chunkKey, (Runnable) () -> {
                try {
                    java.lang.reflect.Method findPathMethod = finder.getClass().getMethod("findPath",
                        Class.forName("net.minecraft.world.level.pathfinder.PathNavigationRegion"),
                        Mob.class, Set.class, float.class, int.class, float.class);
                    Path result = (Path) findPathMethod.invoke(finder, region, mob, targets, maxRange, accuracy, depth);
                    future.complete(result);
                } catch (Exception ex) {
                    future.complete(null);
                }
            });
            
        } catch (Exception e) {
            try {
                java.lang.reflect.Method findPathMethod = finder.getClass().getMethod("findPath",
                    Class.forName("net.minecraft.world.level.pathfinder.PathNavigationRegion"),
                    Mob.class, Set.class, float.class, int.class, float.class);
                return (Path) findPathMethod.invoke(finder, region, mob, targets, maxRange, accuracy, depth);
            } catch (Exception ex) {
                return null;
            }
        }
        
        return future.getNow(null);
    }
    
    private long getChunkKey(BlockPos pos) {
        return ((long)(pos.getX() >> 4) << 32) | ((pos.getZ() >> 4) & 0xFFFFFFFFL);
    }
}

