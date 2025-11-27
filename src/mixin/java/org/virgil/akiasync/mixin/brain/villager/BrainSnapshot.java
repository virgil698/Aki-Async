package org.virgil.akiasync.mixin.brain.villager;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.ExpirableValue;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
public final class BrainSnapshot {
    private final Map<MemoryModuleType<?>, Optional<?>> values;
    private final long gameTime;
    private BrainSnapshot(Map<MemoryModuleType<?>, Optional<?>> values, long gameTime) {
        this.values = values;
        this.gameTime = gameTime;
    }
    @SuppressWarnings("unchecked")
    public static <E extends LivingEntity> BrainSnapshot capture(Brain<E> brain, ServerLevel level) {
        long gameTime = level.getGameTime();
        Map<MemoryModuleType<?>, Optional<?>> copy = new HashMap<>();
        for (Map.Entry<MemoryModuleType<?>, ? extends Optional<?>> entry : brain.getMemories().entrySet()) {
            @SuppressWarnings("rawtypes")
            MemoryModuleType type = entry.getKey();
            Optional<?> opt = entry.getValue();
            if (opt != null && opt.isPresent() && opt.get() instanceof ExpirableValue) {
                ExpirableValue<?> ev = (ExpirableValue<?>) opt.get();
                Object value = ev.getValue();
                long ttl = ev.getTimeToLive();
                @SuppressWarnings("unchecked")
                ExpirableValue<?> newEv = new ExpirableValue<>(value, ttl);
                copy.put(type, Optional.of(newEv));
            } else {
                copy.put(type, opt != null ? opt : Optional.empty());
            }
        }
        return new BrainSnapshot(copy, gameTime);
    }
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
    public long getGameTime() {
        return gameTime;
    }
}
