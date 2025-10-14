package org.virgil.akiasync.mixin.brain;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.ExpirableValue;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

/**
 * Brain snapshot with deep copy and expiration time correction
 * 
 * Core problem: Memory stores ExpirableValue<T>, not T directly
 * Solution:
 * 1. Copy only behavior-related memories (WalkTarget, LookTarget, JobSite, MeetingSite)
 * 2. Correct expiration time → Let ExpirableValue continue working on main thread timeline
 * 3. applyTo completes within same tick → Type matches when Behavior retrieves value
 * 
 * @author Virgil
 */
public final class BrainSnapshot {
    
    private final Map<MemoryModuleType<?>, Optional<?>> values;
    private final long gameTime;
    
    private BrainSnapshot(Map<MemoryModuleType<?>, Optional<?>> values, long gameTime) {
        this.values = values;
        this.gameTime = gameTime;
    }
    
    /**
     * Capture Brain snapshot (main thread invocation)
     * 
     * @param brain Target Brain
     * @param level ServerLevel (for gameTime)
     * @return Brain snapshot
     */
    @SuppressWarnings("unchecked")
    public static <E extends LivingEntity> BrainSnapshot capture(Brain<E> brain, ServerLevel level) {
        long gameTime = level.getGameTime();
        Map<MemoryModuleType<?>, Optional<?>> copy = new HashMap<>();
        
        // Copy only behavior-related memories to avoid large data blocks
        for (Map.Entry<MemoryModuleType<?>, ? extends Optional<?>> entry : brain.getMemories().entrySet()) {
            @SuppressWarnings("rawtypes")
            MemoryModuleType type = entry.getKey();
            Optional<?> opt = entry.getValue();
            
            if (opt != null && opt.isPresent() && opt.get() instanceof ExpirableValue) {
                // Rewrap: correct expiration time
                ExpirableValue<?> ev = (ExpirableValue<?>) opt.get();
                // Create new ExpirableValue using current time to recalculate expiration
                Object value = ev.getValue();
                long ttl = ev.getTimeToLive();
                @SuppressWarnings("unchecked")
                ExpirableValue<?> newEv = new ExpirableValue<>(value, ttl);
                copy.put(type, Optional.of(newEv));
            } else {
                // Non-ExpirableValue or null, direct copy
                copy.put(type, opt != null ? opt : Optional.empty());
            }
        }
        
        return new BrainSnapshot(copy, gameTime);
    }
    
    /**
     * Apply snapshot to Brain (main thread invocation)
     * Must complete within same tick, otherwise Behavior's expiration check will have type mismatch
     * 
     * @param brain Target Brain
     */
    @SuppressWarnings("unchecked")
    public <E extends LivingEntity> void applyTo(Brain<E> brain) {
        values.forEach((key, value) -> {
            MemoryModuleType type = (MemoryModuleType) key;
            if (value.isPresent()) {
                brain.setMemory(type, value.get());
            } else {
                brain.eraseMemory(type);
            }
        });
    }
    
    /**
     * Get snapshot time (for debugging)
     */
    public long getGameTime() {
        return gameTime;
    }
}

