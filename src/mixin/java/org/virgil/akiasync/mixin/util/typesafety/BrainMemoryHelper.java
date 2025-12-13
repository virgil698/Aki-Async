package org.virgil.akiasync.mixin.util.typesafety;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

import java.util.Optional;

public final class BrainMemoryHelper {

    private BrainMemoryHelper() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static <U> Optional<U> getMemory(
            Brain<?> brain, 
            MemoryModuleType<U> memoryType) {
        
        if (brain == null) {
            return Optional.empty();
        }
        
        try {
            return brain.getMemory(memoryType);
        } catch (Exception e) {
            
            return Optional.empty();
        }
    }

    public static <U> void setMemory(
            Brain<?> brain, 
            MemoryModuleType<U> memoryType, 
            U value) {
        
        if (brain == null) {
            return;
        }
        
        try {
            brain.setMemory(memoryType, value);
        } catch (Exception e) {

        }
    }

    @SuppressWarnings("unchecked") 
    public static void setMemoryRaw(
            Brain<?> brain, 
            MemoryModuleType<Object> memoryType, 
            Object value) {
        
        if (brain == null || value == null) {
            return;
        }
        
        try {
            brain.setMemory(memoryType, value);
        } catch (Exception e) {

        }
    }

    public static void eraseMemory(
            Brain<?> brain, 
            MemoryModuleType<?> memoryType) {
        
        if (brain == null) {
            return;
        }
        
        try {
            brain.eraseMemory(memoryType);
        } catch (Exception e) {

        }
    }

    public static <E extends LivingEntity> BrainMemorySnapshot createSnapshot(
            Brain<E> brain, 
            ServerLevel level) {
        
        return BrainMemorySnapshot.capture(brain, level);
    }

    public static <E extends LivingEntity> void applySnapshot(
            Brain<E> brain, 
            BrainMemorySnapshot snapshot) {
        
        snapshot.applyTo(brain);
    }
}
