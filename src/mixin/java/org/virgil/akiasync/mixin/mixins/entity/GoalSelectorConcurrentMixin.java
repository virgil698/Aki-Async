package org.virgil.akiasync.mixin.mixins.entity;

import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * GoalSelector并发集合Mixin
 * 
 * 参考Async模组的GoalSelectorMixin实现，将availableGoals替换为并发集合
 * 
 * 这样可以安全地在多线程环境中访问和修改目标列表
 * 
 * @author AkiAsync
 */
@Mixin(GoalSelector.class)
public class GoalSelectorConcurrentMixin {

    /**
     * 将availableGoals替换为ConcurrentHashMap.newKeySet()
     * 
     * 这是一个线程安全的Set实现，支持并发读写
     */
    @Shadow
    private final Set<WrappedGoal> availableGoals = ConcurrentHashMap.newKeySet();
}
