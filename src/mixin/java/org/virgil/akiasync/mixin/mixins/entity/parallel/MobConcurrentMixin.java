package org.virgil.akiasync.mixin.mixins.entity.parallel;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = Mob.class, priority = 900)
public class MobConcurrentMixin {

    @Unique
    private static final Object akiasync$mobLock = new Object();

    @WrapMethod(method = "equipItemIfPossible")
    private ItemStack akiasync$tryEquipSynchronized(ServerLevel level, ItemStack stack, Operation<ItemStack> original) {
        synchronized (akiasync$mobLock) {
            return original.call(level, stack);
        }
    }

    @WrapMethod(method = "pickUpItem")
    private void akiasync$pickUpItemSynchronized(ServerLevel level, ItemEntity entity, Operation<Void> original) {
        synchronized (akiasync$mobLock) {
            if (!entity.isRemoved()) {
                original.call(level, entity);
            }
        }
    }

    @WrapMethod(method = "setItemSlotAndDropWhenKilled")
    private void akiasync$equipLootStackSynchronized(EquipmentSlot slot, ItemStack stack, Operation<Void> original) {
        synchronized (akiasync$mobLock) {
            original.call(slot, stack);
        }
    }

    @WrapMethod(method = "setBodyArmorItem")
    private void akiasync$setBodyArmorSynchronized(ItemStack stack, Operation<Void> original) {
        synchronized (akiasync$mobLock) {
            original.call(stack);
        }
    }
}

