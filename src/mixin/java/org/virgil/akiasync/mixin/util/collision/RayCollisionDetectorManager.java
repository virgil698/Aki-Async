package org.virgil.akiasync.mixin.util.collision;

import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class RayCollisionDetectorManager {
    
    private static final Map<Integer, RayCollisionDetector> DETECTORS = new ConcurrentHashMap<>();
    
    private RayCollisionDetectorManager() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    public static RayCollisionDetector getDetector(Level level) {
        int levelId = System.identityHashCode(level);
        return DETECTORS.computeIfAbsent(levelId, k -> new RayCollisionDetector(level));
    }
    
    public static void clearAll() {
        DETECTORS.clear();
    }
    
    public static void clearForLevel(Level level) {
        int levelId = System.identityHashCode(level);
        DETECTORS.remove(levelId);
    }
}
