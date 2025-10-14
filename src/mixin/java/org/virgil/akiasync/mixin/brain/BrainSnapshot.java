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
 * Brain快照 - 深拷贝并校正过期时间
 * 
 * 核心问题：Memory 中存储的是 ExpirableValue<T>，而不是直接的 T
 * 解决方案：
 * 1. 只拷贝行为相关内存（WalkTarget、LookTarget、JobSite、MeetingSite）
 * 2. 校正过期时间 → 让 ExpirableValue 在主线程时间轴上继续生效
 * 3. applyTo 在同 tick 内完成 → Behavior 取值时类型匹配
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
     * 捕获Brain快照（主线程调用）
     * 
     * @param brain 目标Brain
     * @param level ServerLevel（获取gameTime）
     * @return Brain快照
     */
    @SuppressWarnings("unchecked")
    public static <E extends LivingEntity> BrainSnapshot capture(Brain<E> brain, ServerLevel level) {
        long gameTime = level.getGameTime();
        Map<MemoryModuleType<?>, Optional<?>> copy = new HashMap<>();
        
        // 只拷贝行为相关内存，避免大块数据
        for (Map.Entry<MemoryModuleType<?>, ? extends Optional<?>> entry : brain.getMemories().entrySet()) {
            @SuppressWarnings("rawtypes")
            MemoryModuleType type = entry.getKey();
            Optional<?> opt = entry.getValue();
            
            if (opt != null && opt.isPresent() && opt.get() instanceof ExpirableValue) {
                // 重新包装：校正过期时间
                ExpirableValue<?> ev = (ExpirableValue<?>) opt.get();
                // 创建新的 ExpirableValue，使用当前时间重新计算过期
                Object value = ev.getValue();
                long ttl = ev.getTimeToLive();
                @SuppressWarnings("unchecked")
                ExpirableValue<?> newEv = new ExpirableValue<>(value, ttl);
                copy.put(type, Optional.of(newEv));
            } else {
                // 非 ExpirableValue 或空值，直接拷贝
                copy.put(type, opt != null ? opt : Optional.empty());
            }
        }
        
        return new BrainSnapshot(copy, gameTime);
    }
    
    /**
     * 应用快照到Brain（主线程调用）
     * 必须在同 tick 内完成，否则 Behavior 判断过期时类型不匹配
     * 
     * @param brain 目标Brain
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
     * 获取快照时间（用于调试）
     */
    public long getGameTime() {
        return gameTime;
    }
}

