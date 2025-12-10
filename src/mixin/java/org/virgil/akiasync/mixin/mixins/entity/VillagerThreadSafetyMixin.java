package org.virgil.akiasync.mixin.mixins.entity;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Villager线程安全Mixin
 * 
 * 参考Async模组的VillagerMixin实现，为村民特有操作添加线程安全保护
 * 
 * 保护的操作：
 * - 拾取物品 (pickUpItem) - 额外检查实体是否已移除
 * - 生成铁傀儡 (spawnGolemIfNeeded)
 * 
 * @author AkiAsync
 */
@Mixin(Villager.class)
public class VillagerThreadSafetyMixin {

    @Unique
    private static final Object aki$villagerLock = new Object();

    /**
     * 拾取物品 - 线程安全 + 实体检查
     */
    @WrapMethod(method = "pickUpItem")
    private void aki$pickUpItemSafe(ServerLevel level, ItemEntity entity, Operation<Void> original) {
        synchronized (aki$villagerLock) {
            
            if (!entity.isRemoved()) {
                original.call(level, entity);
            }
        }
    }

    /**
     * 生成铁傀儡 - 线程安全
     */
    @WrapMethod(method = "spawnGolemIfNeeded")
    private void aki$spawnGolemSafe(ServerLevel world, long time, int requiredCount, Operation<Void> original) {
        synchronized (aki$villagerLock) {
            original.call(world, time, requiredCount);
        }
    }
}
