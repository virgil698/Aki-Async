package org.virgil.akiasync.mixin.mixins.brain;

import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.ExpirableValue;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Brain并发集合Mixin
 * 
 * 参考Async模组的BrainMixin实现，将memories替换为并发集合
 * 
 * 这样可以安全地在多线程环境中访问和修改Brain内存
 * 
 * 注意：这是一个可选的优化，默认情况下我们使用快照模式避免并发问题
 * 但在某些极端并发场景下，并发集合可以提供额外的安全保障
 * 
 * @author AkiAsync
 */
@Mixin(Brain.class)
public class BrainConcurrentMixin {

    /**
     * 将memories替换为ConcurrentHashMap
     * 
     * 这是一个线程安全的Map实现，支持并发读写
     * 
     * 注意：这个修改是可选的，因为我们主要使用快照模式
     * 但它可以作为额外的安全保障
     */
    @Shadow
    private final Map<MemoryModuleType<?>, Optional<? extends ExpirableValue<?>>> memories = 
        new ConcurrentHashMap<>();
}
