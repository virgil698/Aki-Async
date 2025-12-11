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

    @Unique
    private boolean akiasync$canMerge(ItemEntity self, ItemEntity other) {
        ItemStack selfStack = self.getItem();
        ItemStack otherStack = other.getItem();
        return ItemStack.isSameItemSameComponents(selfStack, otherStack);
    }

    @Unique
    private boolean akiasync$isMergable(ItemEntity self) {
        try {
            ItemStack stack = self.getItem();
            return stack != null && !stack.isEmpty() && stack.getCount() < stack.getMaxStackSize();
        } catch (Throwable t) {
            return false;
        }
    }

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

    @Unique
    private boolean akiasync$isInDangerousEnvironment(ItemEntity item) {
        try {
            
            if (item.isInLava() || item.isOnFire() || item.getRemainingFireTicks() > 0) {
                return true;
            }

            if (item.isInWater()) {
                return true;
            }

            if (akiasync$isInPortal(item)) {
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

            if (state.getBlock() instanceof net.minecraft.world.level.block.NetherPortalBlock ||
                state.getBlock() instanceof net.minecraft.world.level.block.EndPortalBlock) {
                return true;
            }

            return false;
        } catch (Throwable t) {
            return true; 
        }
    }

    @Unique
    private boolean akiasync$isInPortal(ItemEntity item) {
        try {
            net.minecraft.core.BlockPos pos = item.blockPosition();
            if (pos == null) return false;

            net.minecraft.world.level.block.state.BlockState state = item.level().getBlockState(pos);
            if (state == null) return false;

            return state.getBlock() instanceof net.minecraft.world.level.block.NetherPortalBlock ||
                   state.getBlock() instanceof net.minecraft.world.level.block.EndPortalBlock;
        } catch (Throwable t) {
            return false;
        }
    }

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
