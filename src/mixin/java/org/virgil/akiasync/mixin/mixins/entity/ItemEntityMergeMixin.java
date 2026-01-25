package org.virgil.akiasync.mixin.mixins.entity;

import java.util.ArrayList;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;

@SuppressWarnings("unused")
@Mixin(ItemEntity.class)
public abstract class ItemEntityMergeMixin {

    @Shadow
    public abstract void tryToMerge(ItemEntity other);

    @Unique
    private static volatile boolean enabled;
    @Unique
    private static volatile double mergeRange;
    @Unique
    private static volatile int maxQueryResults = 20;
    @Unique
    private static volatile boolean initialized = false;

    @Unique
    private List<ItemEntity> aki$cachedNearbyItems = null;
    @Unique
    private int aki$lastCacheTick = 0;

    @Redirect(
        method = "mergeWithNeighbours",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;getEntitiesOfClass(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;"
        ),
        require = 0
    )
    private <T extends net.minecraft.world.entity.Entity> List<T> aki$optimizeEntityQuery(
            net.minecraft.world.level.Level level,
            Class<T> entityClass,
            AABB box,
            java.util.function.Predicate<? super T> predicate) {

        if (!initialized) {
            akiasync$initMergeOptimization();
        }

        if (!enabled) {

            return level.getEntitiesOfClass(entityClass, box, predicate);
        }

        ItemEntity self = (ItemEntity) (Object) this;

        if (akiasync$isVirtualEntity(self)) {
            return level.getEntitiesOfClass(entityClass, box, predicate);
        }

        if (akiasync$isInDangerousEnvironment(self)) {
            return java.util.Collections.emptyList();
        }

        try {
            org.virgil.akiasync.mixin.util.EntitySliceGrid grid =
                org.virgil.akiasync.mixin.util.EntitySliceGridManager.getSliceGrid(level);

            if (grid != null) {
                List<net.minecraft.world.entity.Entity> entities = grid.queryRange(box);

                @SuppressWarnings("unchecked")
                List<T> result = new ArrayList<>(Math.min(entities.size(), maxQueryResults));
                for (net.minecraft.world.entity.Entity entity : entities) {
                    if (entityClass.isInstance(entity)) {
                        @SuppressWarnings("unchecked")
                        T typed = (T) entity;
                        if (predicate.test(typed)) {
                            result.add(typed);
                            if (result.size() >= maxQueryResults) {
                                break;
                            }
                        }
                    }
                }
                return result;
            }
        } catch (Throwable t) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "ItemEntityMerge", "optimizeEntityQuery",
                t instanceof Exception ? (Exception) t : new RuntimeException(t));
        }

        List<T> result = level.getEntitiesOfClass(entityClass, box, predicate);

        if (result.size() > maxQueryResults) {
            return result.subList(0, maxQueryResults);
        }

        return result;
    }

    @Unique
    private boolean akiasync$isInDangerousEnvironment(ItemEntity item) {
        if (item.isInLava() || item.isOnFire() || item.getRemainingFireTicks() > 0) {
            return true;
        }

        if (akiasync$isInPortal(item)) {
            return true;
        }

        net.minecraft.core.BlockPos pos = item.blockPosition();
        if (pos == null) {
            return false;
        }

        net.minecraft.world.level.block.state.BlockState state = item.level().getBlockState(pos);
        if (state == null) {
            return false;
        }

        if (state.getBlock() instanceof net.minecraft.world.level.block.LayeredCauldronBlock ||
            state.getBlock() instanceof net.minecraft.world.level.block.LavaCauldronBlock) {
            return true;
        }

        if (state.getBlock() instanceof net.minecraft.world.level.block.NetherPortalBlock ||
            state.getBlock() instanceof net.minecraft.world.level.block.EndPortalBlock) {
            return true;
        }

        return false;
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
    private static synchronized void akiasync$initMergeOptimization() {
        if (initialized) return;

        org.virgil.akiasync.mixin.bridge.Bridge bridge =
            org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();

        if (bridge != null) {
            enabled = bridge.isItemEntityMergeOptimizationEnabled();
            mergeRange = bridge.getItemEntityMergeRange();

            bridge.debugLog("[AkiAsync] ItemEntityMergeMixin initialized: enabled=" + enabled +
                ", mergeRange=" + mergeRange +
                " (optimizes vanilla mergeWithNeighbours query, no duplicate execution)");

            initialized = true;
        } else {
            enabled = false;
            mergeRange = 2.0;
        }
    }
}
