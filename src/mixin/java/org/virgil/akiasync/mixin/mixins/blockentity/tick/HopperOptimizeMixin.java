package org.virgil.akiasync.mixin.mixins.blockentity.tick;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Mixin(HopperBlockEntity.class)
public abstract class HopperOptimizeMixin {

    @Shadow
    private NonNullList<ItemStack> items;

    @Unique
    private static final ConcurrentHashMap<BlockPos, Object[]> containerCache = new ConcurrentHashMap<>();

    @Unique
    private static volatile boolean cached_enabled = false;

    @Unique
    private static volatile boolean initialized = false;

    @Unique
    private static volatile int cached_cacheExpireTime = 100;

    @Unique
    private static volatile long cacheHits = 0;

    @Unique
    private static volatile long cacheMisses = 0;

    @Unique
    private static volatile long skipEmptyCount = 0;

    @Unique
    private static volatile long skipFullCount = 0;

    @Unique
    private int aki$cachedOccupiedSlots = -1;

    @Unique
    private int aki$cachedFullSlots = -1;

    @Unique
    private long aki$lastModCount = -1;

    @Unique
    private long aki$modCount = 0;

    @Inject(method = "setItem", at = @At("RETURN"))
    private void aki$onSetItem(int index, ItemStack stack, CallbackInfo ci) {
        aki$modCount++;
    }

    @Unique
    private boolean aki$isHopperEmpty() {
        aki$updateSlotCounts();
        return aki$cachedOccupiedSlots == 0;
    }

    @Unique
    private boolean aki$isHopperFull() {
        aki$updateSlotCounts();
        return aki$cachedFullSlots == items.size();
    }

    @Unique
    private boolean aki$hasItemsToEject() {
        aki$updateSlotCounts();
        return aki$cachedOccupiedSlots > 0;
    }

    @Unique
    private boolean aki$hasSpaceToReceive() {
        aki$updateSlotCounts();
        return aki$cachedFullSlots < items.size();
    }

    @Unique
    private void aki$updateSlotCounts() {
        if (aki$lastModCount == aki$modCount && aki$cachedOccupiedSlots >= 0) {
            return;
        }

        int occupied = 0;
        int full = 0;

        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            if (!stack.isEmpty()) {
                occupied++;
                if (stack.getCount() >= stack.getMaxStackSize()) {
                    full++;
                }
            }
        }

        aki$cachedOccupiedSlots = occupied;
        aki$cachedFullSlots = full;
        aki$lastModCount = aki$modCount;
    }

    @Inject(method = "getContainerAt(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/Container;",
            at = @At("HEAD"), cancellable = true, require = 0)
    private static void cacheGetContainerAt(Level level, BlockPos pos, CallbackInfoReturnable<Container> cir) {
        if (!initialized) {
            aki$initHopperOptimization();
        }

        if (!cached_enabled) {
            return;
        }

        Object[] cache = containerCache.get(pos);
        if (cache != null) {
            long cacheTime = (Long) cache[1];
            if (System.currentTimeMillis() - cacheTime <= cached_cacheExpireTime) {
                cacheHits++;
                cir.setReturnValue((Container) cache[0]);
                return;
            }
        }
        cacheMisses++;
    }

    @Inject(method = "getContainerAt(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/Container;",
            at = @At("RETURN"), require = 0)
    private static void cacheGetContainerAtReturn(Level level, BlockPos pos, CallbackInfoReturnable<Container> cir) {
        if (!cached_enabled) {
            return;
        }

        Container result = cir.getReturnValue();
        if (result != null) {
            Object[] cacheEntry = new Object[]{result, System.currentTimeMillis()};
            containerCache.put(pos.immutable(), cacheEntry);

            if (containerCache.size() > 1000) {
                aki$cleanExpiredCache();
            }
        }
    }

    @Unique
    private static boolean aki$isContainerFull(Container container, Direction direction) {
        if (container == null) return true;

        int size = container.getContainerSize();
        for (int i = 0; i < size; i++) {
            ItemStack stack = container.getItem(i);
            if (stack.isEmpty() || stack.getCount() < Math.min(stack.getMaxStackSize(), container.getMaxStackSize())) {
                return false;
            }
        }
        return true;
    }

    @Unique
    private static boolean aki$isContainerEmpty(Container container) {
        if (container == null) return true;

        int size = container.getContainerSize();
        for (int i = 0; i < size; i++) {
            if (!container.getItem(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Unique
    private static void aki$cleanExpiredCache() {
        long now = System.currentTimeMillis();
        containerCache.entrySet().removeIf(entry -> {
            Object[] value = entry.getValue();
            if (value == null || value.length < 2 || value[1] == null) {
                return true;
            }
            long cacheTime = (Long) value[1];
            return now - cacheTime > cached_cacheExpireTime;
        });
    }

    @Unique
    private static void aki$initHopperOptimization() {
        org.virgil.akiasync.mixin.bridge.Bridge bridge =
            org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();

        if (bridge != null) {
            cached_enabled = bridge.isHopperOptimizationEnabled();
            cached_cacheExpireTime = bridge.getHopperCacheExpireTime();

            bridge.debugLog("[AkiAsync] HopperOptimizeMixin initialized: enabled=" +
                cached_enabled + " | expireTime=" + cached_cacheExpireTime + "ms");

            initialized = true;
        } else {
            cached_enabled = false;
        }
    }

    @Unique
    private static String aki$getStatistics() {
        long total = cacheHits + cacheMisses;
        double hitRate = total > 0 ? (double) cacheHits / total * 100 : 0;
        return String.format("HopperCache: Size=%d, Hits=%d, Misses=%d, HitRate=%.2f%%, SkipEmpty=%d, SkipFull=%d",
            containerCache.size(), cacheHits, cacheMisses, hitRate, skipEmptyCount, skipFullCount);
    }
}
