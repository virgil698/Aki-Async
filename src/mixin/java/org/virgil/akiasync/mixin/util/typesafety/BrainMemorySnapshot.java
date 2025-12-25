package org.virgil.akiasync.mixin.util.typesafety;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.ExpirableValue;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class BrainMemorySnapshot {
    
    private final Map<MemoryModuleType<?>, MemoryEntry<?>> memories;
    private final long gameTime;
    
    private BrainMemorySnapshot(
            Map<MemoryModuleType<?>, MemoryEntry<?>> memories, 
            long gameTime) {
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
    
    public static <E extends LivingEntity> BrainMemorySnapshot capture(
            Brain<E> brain, 
            ServerLevel level) {
        
        long gameTime = level.getGameTime();
        Map<MemoryModuleType<?>, MemoryEntry<?>> copy = new HashMap<>();
        
        @SuppressWarnings({"deprecation", "unchecked"})
        Map<MemoryModuleType<?>, Optional<?>> memories = (Map<MemoryModuleType<?>, Optional<?>>) (Map<?, ?>) brain.getMemories();
        
        for (Map.Entry<MemoryModuleType<?>, Optional<? extends Object>> entry : memories.entrySet()) {
            MemoryModuleType<?> type = entry.getKey();
            Optional<? extends Object> opt = entry.getValue();
            
            if (opt != null && opt.isPresent()) {
                Object value = opt.get();
                
                if (value instanceof ExpirableValue<?>) {
                    ExpirableValue<?> ev = (ExpirableValue<?>) value;
                    Object innerValue = ev.getValue();
                    long ttl = ev.getTimeToLive();
                    
                    copy.put(type, new MemoryEntry<>(innerValue, ttl, true));
                } else {
                    copy.put(type, new MemoryEntry<>(value, 0, false));
                }
            }
        }
        
        return new BrainMemorySnapshot(copy, gameTime);
    }
    
    @SuppressWarnings("unchecked")
    public <E extends LivingEntity> void applyTo(Brain<E> brain) {
        for (Map.Entry<MemoryModuleType<?>, MemoryEntry<?>> entry : memories.entrySet()) {
            MemoryModuleType type = (MemoryModuleType) entry.getKey();
            MemoryEntry<?> memEntry = entry.getValue();
            
            try {
                Object value = memEntry.getValue();
                
                if (memEntry.isExpirable()) {
                    
                    ExpirableValue<?> expirableValue = new ExpirableValue<>(value, memEntry.getTtl());
                    brain.setMemory(type, expirableValue);
                } else {
                    brain.setMemory(type, value);
                }
            } catch (ClassCastException e) {

                org.virgil.akiasync.mixin.util.BridgeConfigCache.errorLog("[BrainSnapshot] ClassCastException restoring memory type " + type + 
                    ": expected compatible type but got " + 
                    memEntry.getValue().getClass().getName());
            } catch (Exception e) {
                
                org.virgil.akiasync.mixin.util.BridgeConfigCache.errorLog("[BrainSnapshot] Error restoring memory type " + type + ": " + 
                    e.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }
    }
    
    public long getGameTime() {
        return gameTime;
    }
    
    public int size() {
        return memories.size();
    }
}
