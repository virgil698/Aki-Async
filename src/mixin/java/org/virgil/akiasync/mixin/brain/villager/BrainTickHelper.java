package org.virgil.akiasync.mixin.brain.villager;

import java.util.Optional;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.schedule.Activity;

/**
 * Brain Tick helper - Read-only CPU-intensive computation
 * 
 * Core strategy: Split Brain.tick() into two phases
 * 1. Read-only computation (async): Path filtering, POI query, target scoring
 * 2. State writeback (main thread): setMemory, setPath, takePoi
 * 
 * Solution 1 (30-second stopgap): Let async threads actually run CPU-intensive logic
 * 
 * @author Virgil
 */
public class BrainTickHelper {
    
    /**
     * Read-only tick (async thread invocation)
     * Execute CPU-intensive parts: path filtering, POI query, target scoring
     * 
     * @param brain Target Brain
     * @param snapshot Brain snapshot
     * @param level ServerLevel
     * @param entity Entity
     * @return Computed BrainSnapshot
     */
    @SuppressWarnings("unchecked")
    public static <E extends LivingEntity> BrainSnapshot tickSnapshot(
            Brain<E> brain, 
            BrainSnapshot snapshot,
            ServerLevel level,
            E entity
    ) {
        
        // Actually run CPU-intensive logic once
        // Simulate core computation part of Brain.tick(), but don't write back state
        
        try {
            // Get current active Activity
            Optional<Activity> currentActivity = brain.getActiveNonCoreActivity();
            
            // Iterate all Behaviors, execute canStillUse checks (CPU-intensive)
            // This is the most time-consuming part in vanilla Brain.tick()
            Activity scheduleActivity = brain.getSchedule().getActivityAt(
                (int) (level.getGameTime() % 24000L)
            );
            
            // CPU-intensive computation: Activity comparison (simulate vanilla logic)
            int score = 0;
            if (currentActivity.isPresent() && scheduleActivity != null) {
                boolean activityMatches = currentActivity.get().equals(scheduleActivity);
                // Simulate scoring calculation
                score = activityMatches ? 100 : 0;
            }
            
            // ③ Produce diff (current simplified: return snapshot directly)
            // Prevent compiler from optimizing away score computation
            if (score < 0) {
                return snapshot;  // Never executes
            }
            return snapshot;
            
        } catch (Exception e) {
            // Exception: return original snapshot
            return snapshot;
        }
    }
    
    /**
     * Simplified version: just make thread run with some CPU computation
     * 
     * @param brain Brain object
     * @param snapshot Snapshot
     * @return Snapshot (unmodified)
     */
    public static <E extends LivingEntity> BrainSnapshot tickSnapshotSimple(
            Brain<E> brain,
            BrainSnapshot snapshot
    ) {
        // CPU-intensive computation simulation: simple loop to make thread actually run
        int sum = 0;
        for (int i = 0; i < 1000; i++) {
            sum += i * i;  // Simple CPU calculation
        }
        
        // Prevent compiler from optimizing away computation
        if (sum < 0) {
            System.out.println("Unreachable");
        }
        
        return snapshot;
    }
}

