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

/**
 * Mob线程安全Mixin
 * 
 * 参考Async模组的MobMixin实现，为关键操作添加线程安全保护
 * 
 * 保护的操作：
 * - 装备物品 (equipItemIfPossible)
 * - 拾取物品 (pickUpItem)
 * - 设置装备槽位 (setItemSlotAndDropWhenKilled)
 * - 设置身体护甲 (setBodyArmorItem)
 * 
 * @author AkiAsync
 */
@Mixin(Mob.class)
public class MobThreadSafetyMixin {

    @Unique
    private static final Object aki$equipLock = new Object();

    /**
     * 装备物品 - 线程安全
     */
    @WrapMethod(method = "equipItemIfPossible")
    private ItemStack aki$equipItemSafe(ServerLevel level, ItemStack stack, Operation<ItemStack> original) {
        synchronized (aki$equipLock) {
            return original.call(level, stack);
        }
    }

    /**
     * 拾取物品 - 线程安全
     */
    @WrapMethod(method = "pickUpItem")
    private void aki$pickUpItemSafe(ServerLevel level, ItemEntity entity, Operation<Void> original) {
        synchronized (aki$equipLock) {
            original.call(level, entity);
        }
    }

    /**
     * 设置装备槽位 - 线程安全
     */
    @WrapMethod(method = "setItemSlotAndDropWhenKilled")
    private void aki$setItemSlotSafe(EquipmentSlot slot, ItemStack stack, Operation<Void> original) {
        synchronized (aki$equipLock) {
            original.call(slot, stack);
        }
    }

    /**
     * 设置身体护甲 - 线程安全
     */
    @WrapMethod(method = "setBodyArmorItem")
    private void aki$setBodyArmorSafe(ItemStack stack, Operation<Void> original) {
        synchronized (aki$equipLock) {
            original.call(stack);
        }
    }
}
