package org.virgil.akiasync.mixin.brain.villager;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.ExpirableValue;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

public final class BrainSnapshot {
    private final Map<MemoryModuleType<?>, MemoryEntry<?>> memories;
    private final long gameTime;
    
    private BrainSnapshot(Map<MemoryModuleType<?>, MemoryEntry<?>> memories, long gameTime) {
        this.memories = Collections.unmodifiableMap(new HashMap<>(memories));
        this.gameTime = gameTime;
    }
    
    private static final class MemoryEntry<U> {
        private final U value;
        private final long ttl;
        private final boolean isExpirable;
        
        MemoryEntry(U value, long ttl, boolean isExpirable) {
            this.value = value;
            this.ttl = ttl;
            this.isExpirable = isExpirable;
        }
        
        U getValue() { 
            return value; 
        }
        
        long getTtl() { 
            return ttl; 
        }
        
        boolean isExpirable() {
            return isExpirable;
        }
    }
    
    @SuppressWarnings("deprecation") 
    public static <E extends LivingEntity> BrainSnapshot capture(Brain<E> brain, ServerLevel level) {
        long gameTime = level.getGameTime();
        Map<MemoryModuleType<?>, MemoryEntry<?>> copy = new HashMap<>();
        
        for (Map.Entry<MemoryModuleType<?>, ? extends Optional<?>> entry : brain.getMemories().entrySet()) {
            MemoryModuleType<?> memoryType = entry.getKey();
            Optional<?> optValue = entry.getValue();
            
            if (optValue != null && optValue.isPresent()) {
                Object rawValue = optValue.get();
                
                if (rawValue instanceof ExpirableValue<?>) {
                    ExpirableValue<?> expirable = (ExpirableValue<?>) rawValue;
                    Object innerValue = expirable.getValue();
                    long ttl = expirable.getTimeToLive();
                    
                    MemoryEntry<?> entry2 = new MemoryEntry<>(innerValue, ttl, true);
                    copy.put(memoryType, entry2);
                } else {
                    
                    MemoryEntry<?> entry2 = new MemoryEntry<>(rawValue, 0, false);
                    copy.put(memoryType, entry2);
                }
            }
        }
        
        return new BrainSnapshot(copy, gameTime);
    }
    
    @SuppressWarnings("unchecked") 
    public <E extends LivingEntity> void applyTo(Brain<E> brain) {
        if (brain == null) {
            return;
        }
        
        memories.forEach((memoryType, entry) -> {
            if (entry != null && memoryType != null) {
                Object value = entry.getValue();
                
                if (value != null) {
                    try {
                        if (entry.isExpirable()) {
                            
                            ExpirableValue<Object> expirable = new ExpirableValue<>(value, entry.getTtl());
                            
                            MemoryModuleType<Object> typedMemory = (MemoryModuleType<Object>) memoryType;
                            brain.setMemory(typedMemory, expirable);
                        } else {
                            
                            MemoryModuleType<Object> typedMemory = (MemoryModuleType<Object>) memoryType;
                            brain.setMemory(typedMemory, value);
                        }
                    } catch (Exception e) {
                        org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                            "BrainSnapshot", "setMemory", e);
                    }
                }
            } else if (memoryType != null) {
                
                try {
                    brain.eraseMemory(memoryType);
                } catch (Exception e) {
                    org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                        "BrainSnapshot", "eraseMemory", e);
                }
            }
        });
    }
    
    public long getGameTime() {
        return gameTime;
    }
}
