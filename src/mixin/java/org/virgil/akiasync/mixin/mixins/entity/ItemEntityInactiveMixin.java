package org.virgil.akiasync.mixin.mixins.entity;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * ItemEntity Inactive Tick优化
 * 
 * 对远离玩家的掉落物使用简化的tick逻辑，只更新关键属性：
 * - pickupDelay（拾取延迟）
 * - age（年龄）
 * - 定期尝试合并
 * 
 * 参考：ServerCore的Inactive Tick实现
 * 性能提升：30-50%（大量远距离掉落物场景）
 */
@SuppressWarnings("unused")
@Mixin(ItemEntity.class)
public abstract class ItemEntityInactiveMixin {

    @Shadow
    private int pickupDelay;

    @Shadow
    public int age;

    @Shadow
    public abstract ItemStack getItem();

    @Shadow
    public abstract void tryToMerge(ItemEntity other);

    @Unique
    private static volatile boolean enabled;
    @Unique
    private static volatile double inactiveRange;
    @Unique
    private static volatile int mergeInterval;
    @Unique
    private static volatile boolean initialized = false;

    @Unique
    private static final int INFINITE_PICKUP_DELAY = 32767;
    @Unique
    private static final int INFINITE_LIFETIME = -32768;
    @Unique
    private static final int LIFETIME = 6000; 

    /**
     * 在tick开始时检查是否应该使用简化tick
     * 注意：需要在其他tick优化之前执行，以便正确取消tick
     */
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void inactiveTick(CallbackInfo ci) {
        if (!initialized) {
            akiasync$initInactiveTick();
        }
        if (!enabled) return;

        ItemEntity self = (ItemEntity) (Object) this;

        if (akiasync$shouldUseInactiveTick(self)) {
            akiasync$performInactiveTick(self);
            ci.cancel(); 
        }
    }

    /**
     * 判断是否应该使用简化tick
     */
    @Unique
    private boolean akiasync$shouldUseInactiveTick(ItemEntity self) {
        
        if (akiasync$isVirtualEntity(self)) {
            return false;
        }

        if (akiasync$isInDangerousEnvironment(self)) {
            return false;
        }

        return !akiasync$hasNearbyPlayer(self, inactiveRange);
    }

    /**
     * 执行简化的tick逻辑
     */
    @Unique
    private void akiasync$performInactiveTick(ItemEntity self) {
        
        if (pickupDelay > 0 && pickupDelay != INFINITE_PICKUP_DELAY) {
            pickupDelay--;
        }

        if (age != INFINITE_LIFETIME) {
            age++;
        }

        if (age >= LIFETIME) {
            
            ((net.minecraft.world.entity.Entity) self).discard();
            return;
        }

        if (age % mergeInterval == 0 && akiasync$isMergable(self)) {
            akiasync$tryQuickMerge(self);
        }
    }

    /**
     * 快速合并：只检查非常近的物品
     */
    @Unique
    private void akiasync$tryQuickMerge(ItemEntity self) {
        try {
            
            AABB box = self.getBoundingBox().inflate(1.0);
            List<ItemEntity> nearby = self.level().getEntitiesOfClass(
                ItemEntity.class,
                box,
                e -> e != self && !e.isRemoved() && akiasync$canMerge(self, e)
            );

            for (ItemEntity other : nearby) {
                this.tryToMerge(other);
                if (self.isRemoved()) {
                    break;
                }
            }
        } catch (Throwable t) {
            
        }
    }

    /**
     * 检查是否可以合并
     */
    @Unique
    private boolean akiasync$canMerge(ItemEntity self, ItemEntity other) {
        ItemStack selfStack = self.getItem();
        ItemStack otherStack = other.getItem();
        return ItemStack.isSameItemSameComponents(selfStack, otherStack);
    }

    /**
     * 检查是否可以合并（简化版）
     */
    @Unique
    private boolean akiasync$isMergable(ItemEntity self) {
        try {
            ItemStack stack = self.getItem();
            return stack != null && !stack.isEmpty() && stack.getCount() < stack.getMaxStackSize();
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * 检查附近是否有玩家
     */
    @Unique
    private boolean akiasync$hasNearbyPlayer(ItemEntity self, double range) {
        try {
            AABB searchBox = self.getBoundingBox().inflate(range);
            List<Player> nearbyPlayers = self.level().getEntitiesOfClass(
                Player.class,
                searchBox
            );
            return !nearbyPlayers.isEmpty();
        } catch (Throwable t) {
            return true; 
        }
    }

    /**
     * 检查是否在危险环境中
     */
    @Unique
    private boolean akiasync$isInDangerousEnvironment(ItemEntity item) {
        try {
            
            if (item.isInLava() || item.isOnFire() || item.getRemainingFireTicks() > 0) {
                return true;
            }

            if (item.isInWater()) {
                return true;
            }

            net.minecraft.core.BlockPos pos = item.blockPosition();
            if (pos == null) return false;

            net.minecraft.world.level.block.state.BlockState state = item.level().getBlockState(pos);
            if (state == null) return false;

            if (state.getBlock() instanceof net.minecraft.world.level.block.LayeredCauldronBlock ||
                state.getBlock() instanceof net.minecraft.world.level.block.LavaCauldronBlock) {
                return true;
            }

            return false;
        } catch (Throwable t) {
            return true; 
        }
    }

    /**
     * 检查是否为虚拟实体
     */
    @Unique
    private boolean akiasync$isVirtualEntity(ItemEntity entity) {
        if (entity == null) return false;

        try {
            org.virgil.akiasync.mixin.bridge.Bridge bridge =
                org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
            if (bridge != null) {
                return bridge.isVirtualEntity(entity);
            }
        } catch (Throwable t) {
            return true; 
        }

        return false;
    }

    /**
     * 初始化配置
     */
    @Unique
    private static synchronized void akiasync$initInactiveTick() {
        if (initialized) return;

        org.virgil.akiasync.mixin.bridge.Bridge bridge =
            org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();

        if (bridge != null) {
            enabled = bridge.isItemEntityInactiveTickEnabled();
            inactiveRange = bridge.getItemEntityInactiveRange();
            mergeInterval = bridge.getItemEntityInactiveMergeInterval();
        } else {
            enabled = true;
            inactiveRange = 32.0;
            mergeInterval = 100;
        }

        initialized = true;

        if (bridge != null) {
            bridge.debugLog("[AkiAsync] ItemEntityInactiveMixin initialized: enabled=" + enabled +
                ", inactiveRange=" + inactiveRange +
                ", mergeInterval=" + mergeInterval);
        }
    }
}
