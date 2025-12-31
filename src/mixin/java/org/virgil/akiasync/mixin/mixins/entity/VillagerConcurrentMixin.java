package org.virgil.akiasync.mixin.mixins.entity;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;


@Mixin(value = Villager.class, priority = 900)
public class VillagerConcurrentMixin {

    @Unique
    private static final Object akiasync$villagerLock = new Object();

    @WrapMethod(method = "pickUpItem")
    private void akiasync$pickUpItemSynchronized(ServerLevel level, ItemEntity entity, Operation<Void> original) {
        synchronized (akiasync$villagerLock) {
            if (!entity.isRemoved()) {
                original.call(level, entity);
            }
        }
    }

    @WrapMethod(method = "spawnGolemIfNeeded")
    private void akiasync$spawnGolemIfNeededSynchronized(ServerLevel world, long time, int requiredCount, Operation<Void> original) {
        synchronized (akiasync$villagerLock) {
            original.call(world, time, requiredCount);
        }
    }
}
