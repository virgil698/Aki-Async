package org.virgil.akiasync.mixin.mixins.entity;

import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(GoalSelector.class)
public class GoalSelectorConcurrentMixin {

    @Shadow
    private final Set<WrappedGoal> availableGoals = ConcurrentHashMap.newKeySet();
}
