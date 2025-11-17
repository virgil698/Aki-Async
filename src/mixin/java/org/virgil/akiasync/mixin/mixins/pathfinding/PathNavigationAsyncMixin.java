package org.virgil.akiasync.mixin.mixins.pathfinding;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.virgil.akiasync.mixin.optimization.cache.BlockPosIterationCache;
import org.virgil.akiasync.mixin.optimization.thread.VirtualThreadService;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathFinder;

@Mixin(PathNavigation.class)
public class PathNavigationAsyncMixin {
    
    private static java.lang.reflect.Method cachedFindPathMethod;
    private static boolean reflectionInitialized = false;
    
    private static VirtualThreadService.VirtualThreadExecutor virtualExecutor;
    private static boolean virtualThreadInitialized = false;
    
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
            return invokeFindPathSafely(finder, region, mob, targets, maxRange, accuracy, depth);
        }
        
        long chunkKey = getChunkKey(mob.blockPosition());
        CompletableFuture<Path> future = new CompletableFuture<>();
        
        try {
            if (!virtualThreadInitialized) {
                initializeVirtualThread();
            }
            
            if (virtualExecutor != null) {
                virtualExecutor.execute(() -> {
                    Path result = invokeFindPathSafely(finder, region, mob, targets, maxRange, accuracy, depth);
                    future.complete(result);
                });
            } else {
                Object level = mob.level();
                java.lang.reflect.Method getSchedulerMethod = 
                    level.getClass().getMethod("moonrise$getChunkTaskScheduler");
                Object scheduler = getSchedulerMethod.invoke(level);
                
                java.lang.reflect.Field radiusSchedulerField = 
                    scheduler.getClass().getDeclaredField("radiusAwareScheduler");
                radiusSchedulerField.setAccessible(true);
                Object radiusScheduler = radiusSchedulerField.get(scheduler);
                
                if (radiusScheduler == null) {
                    return invokeFindPathSafely(finder, region, mob, targets, maxRange, accuracy, depth);
                }
                
                java.lang.reflect.Method executeMethod = radiusScheduler.getClass().getMethod("execute",
                    Object.class, long.class, Runnable.class);
                
                executeMethod.invoke(radiusScheduler, level, chunkKey, (Runnable) () -> {
                    Path result = invokeFindPathSafely(finder, region, mob, targets, maxRange, accuracy, depth);
                    future.complete(result);
                });
            }
            
        } catch (Exception e) {
            return invokeFindPathSafely(finder, region, mob, targets, maxRange, accuracy, depth);
        }
        
        try {
            return future.get(50, java.util.concurrent.TimeUnit.MICROSECONDS);
        } catch (Exception e) {
            return invokeFindPathSafely(finder, region, mob, targets, maxRange, accuracy, depth);
        }
    }
    
    private long getChunkKey(BlockPos pos) {
        return ((long)(pos.getX() >> 4) << 32) | ((pos.getZ() >> 4) & 0xFFFFFFFFL);
    }
    
    private Path invokeFindPathSafely(PathFinder finder, Object region, Mob mob, 
                                     Set<BlockPos> targets, float maxRange, int accuracy, float depth) {
        try {
            if (!reflectionInitialized) {
                initializeReflection();
            }
            
            if (cachedFindPathMethod != null) {
                return (Path) cachedFindPathMethod.invoke(finder, region, mob, targets, maxRange, accuracy, depth);
            }
        } catch (Exception e) {
            if (System.currentTimeMillis() % 10000 < 100) {
                System.err.println("[AkiAsync-PathNav] Reflection error: " + e.getMessage());
            }
        }
        return null;
    }
    
    private static synchronized void initializeReflection() {
        if (reflectionInitialized) return;
        
        try {
            cachedFindPathMethod = PathFinder.class.getMethod("findPath",
                Class.forName("net.minecraft.world.level.pathfinder.PathNavigationRegion"),
                Mob.class, Set.class, float.class, int.class, float.class);
            cachedFindPathMethod.setAccessible(true);
        } catch (Exception e) {
            System.err.println("[AkiAsync-PathNav] Failed to initialize reflection: " + e.getMessage());
            cachedFindPathMethod = null;
        }
        
        reflectionInitialized = true;
    }
    
    private static synchronized void initializeVirtualThread() {
        if (virtualThreadInitialized) return;
        
        try {
            virtualExecutor = VirtualThreadService.VirtualThreadExecutor.create();
            if (virtualExecutor != null) {
                org.virgil.akiasync.mixin.bridge.Bridge bridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
                if (bridge != null) {
                    bridge.debugLog("[AkiAsync-PathNav] Virtual Thread support enabled (Java " + 
                        VirtualThreadService.getJavaMajorVersion() + ")");
                }
            } else {
                org.virgil.akiasync.mixin.bridge.Bridge bridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
                if (bridge != null) {
                    bridge.debugLog("[AkiAsync-PathNav] Virtual Thread not supported, using traditional threads");
                }
            }
        } catch (Exception e) {
            System.err.println("[AkiAsync-PathNav] Failed to initialize Virtual Thread: " + e.getMessage());
            virtualExecutor = null;
        }
        
        virtualThreadInitialized = true;
    }
}

