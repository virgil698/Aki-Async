package org.virgil.akiasync.mixin.mixins.entity;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Mob.class)
public class MobThreadSafetyMixin {

    @Unique
    private static final Object aki$equipLock = new Object();

    @WrapMethod(method = "equipItemIfPossible")
    private ItemStack aki$equipItemSafe(ServerLevel level, ItemStack stack, Operation<ItemStack> original) {
        synchronized (aki$equipLock) {
            return original.call(level, stack);
        }
    }

    @WrapMethod(method = "pickUpItem")
    private void aki$pickUpItemSafe(ServerLevel level, ItemEntity entity, Operation<Void> original) {
        synchronized (aki$equipLock) {
            original.call(level, entity);
        }
    }

    @WrapMethod(method = "setItemSlotAndDropWhenKilled")
    private void aki$setItemSlotSafe(EquipmentSlot slot, ItemStack stack, Operation<Void> original) {
        synchronized (aki$equipLock) {
            original.call(slot, stack);
        }
    }

    @WrapMethod(method = "setBodyArmorItem")
    private void aki$setBodyArmorSafe(ItemStack stack, Operation<Void> original) {
        synchronized (aki$equipLock) {
            original.call(stack);
        }
    }
}
