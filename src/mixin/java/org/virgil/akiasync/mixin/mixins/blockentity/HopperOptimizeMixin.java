package org.virgil.akiasync.mixin.mixins.blockentity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Mixin(HopperBlockEntity.class)
public class HopperOptimizeMixin {
    
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
    private static void aki$invalidateCache(BlockPos pos) {
        containerCache.remove(pos);
    }
    
    @Unique
    private static void aki$clearCache() {
        containerCache.clear();
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
        } else {
            cached_enabled = false;
        }
        
        initialized = true;
    }
    
    @Unique
    private static String aki$getStatistics() {
        long total = cacheHits + cacheMisses;
        double hitRate = total > 0 ? (double) cacheHits / total * 100 : 0;
        return String.format("HopperCache: Size=%d, Hits=%d, Misses=%d, HitRate=%.2f%%", 
            containerCache.size(), cacheHits, cacheMisses, hitRate);
    }
}
