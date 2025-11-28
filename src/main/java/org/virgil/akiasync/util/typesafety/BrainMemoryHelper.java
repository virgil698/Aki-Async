package org.virgil.akiasync.util.typesafety;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

public final class BrainMemoryHelper {

    private BrainMemoryHelper() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    @Nonnull
    public static <U> Optional<U> getMemory(
            @Nullable Brain<?> brain, 
            @Nonnull MemoryModuleType<U> memoryType) {
        
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
            @Nullable Brain<?> brain, 
            @Nonnull MemoryModuleType<U> memoryType, 
            @Nonnull U value) {
        
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
            @Nullable Brain<?> brain, 
            @Nonnull MemoryModuleType<Object> memoryType, 
            @Nullable Object value) {
        
        if (brain == null || value == null) {
            return;
        }
        
        try {
            brain.setMemory(memoryType, value);
        } catch (Exception e) {
            
        }
    }

    public static void eraseMemory(
            @Nullable Brain<?> brain, 
            @Nonnull MemoryModuleType<?> memoryType) {
        
        if (brain == null) {
            return;
        }
        
        try {
            brain.eraseMemory(memoryType);
        } catch (Exception e) {
            
        }
    }

    @Nonnull
    public static <E extends LivingEntity> BrainMemorySnapshot createSnapshot(
            @Nonnull Brain<E> brain, 
            @Nonnull ServerLevel level) {
        
        return BrainMemorySnapshot.capture(brain, level);
    }

    public static <E extends LivingEntity> void applySnapshot(
            @Nonnull Brain<E> brain, 
            @Nonnull BrainMemorySnapshot snapshot) {
        
        snapshot.applyTo(brain);
    }
}
