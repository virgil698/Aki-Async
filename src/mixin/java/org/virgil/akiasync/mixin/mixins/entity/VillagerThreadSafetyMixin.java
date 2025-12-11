package org.virgil.akiasync.mixin.mixins.entity;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Villager.class)
public class VillagerThreadSafetyMixin {

    @Unique
    private final Object aki$instanceLock = new Object();

    @WrapMethod(method = "pickUpItem")
    private void aki$pickUpItemSafe(ServerLevel level, ItemEntity entity, Operation<Void> original) {
        synchronized (aki$instanceLock) {
            
            if (!entity.isRemoved()) {
                original.call(level, entity);
            }
        }
    }

    @WrapMethod(method = "spawnGolemIfNeeded")
    private void aki$spawnGolemSafe(ServerLevel world, long time, int requiredCount, Operation<Void> original) {
        synchronized (aki$instanceLock) {
            original.call(world, time, requiredCount);
        }
    }
}
