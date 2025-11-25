package org.virgil.akiasync.mixin.mixins.pathfinding;

import java.util.ArrayList;
import java.util.Set;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.virgil.akiasync.mixin.pathfinding.AsyncPath;
import org.virgil.akiasync.mixin.pathfinding.AsyncPathProcessor;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathFinder;

@Mixin(PathNavigation.class)
public class PathNavigationAsyncMixin {
    
    private static java.lang.reflect.Method cachedFindPathMethod;
    private static boolean reflectionInitialized = false;
    
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
        
        if (!AsyncPathProcessor.isEnabled()) {
            return invokeFindPathSafely(finder, region, mob, targets, maxRange, accuracy, depth);
        }
        
        try {
            ArrayList<Node> emptyNodes = new ArrayList<>();
            
            return new AsyncPath(emptyNodes, targets, () -> {
                return invokeFindPathSafely(finder, region, mob, targets, maxRange, accuracy, depth);
            });
            
        } catch (Exception e) {
            return invokeFindPathSafely(finder, region, mob, targets, maxRange, accuracy, depth);
        }
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
}

