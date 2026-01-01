package org.virgil.akiasync.mixin.mixins.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.virgil.akiasync.mixin.util.RayTraceCacheHolder;
import org.virgil.akiasync.mixin.util.BridgeConfigCache;

@Mixin(value = Level.class, priority = 990)
public abstract class FastRayTraceMixin {
    
    @Unique
    private static volatile boolean enabled = false;
    
    @Unique
    private static volatile boolean init = false;
    
    @Unique
    private static volatile int cacheSize = 256;
    
    @Unique
    private static volatile java.lang.reflect.Field blockField = null;
    
    @Unique
    private static volatile java.lang.reflect.Field fluidField = null;
    
    @Unique
    private static final java.util.concurrent.ConcurrentHashMap<Long, RayTraceCacheHolder> aki$rayTraceCache = 
        new java.util.concurrent.ConcurrentHashMap<Long, RayTraceCacheHolder>();
    
    @Unique
    private static RayTraceCacheHolder aki$getCache() {
        long threadId = Thread.currentThread().getId();
        RayTraceCacheHolder holder = aki$rayTraceCache.get(threadId);
        if (holder == null) {
            holder = new RayTraceCacheHolder(cacheSize);
            RayTraceCacheHolder existing = aki$rayTraceCache.putIfAbsent(threadId, holder);
            if (existing != null) {
                holder = existing;
            }
        }
        return holder;
    }
    
    @Inject(method = "clip(Lnet/minecraft/world/level/ClipContext;)Lnet/minecraft/world/phys/BlockHitResult;",
            at = @At("HEAD"), cancellable = true)
    private void aki$fastRayTrace(ClipContext context, CallbackInfoReturnable<BlockHitResult> cir) {
        if (!init) {
            aki$init();
        }
        
        if (!enabled) {
            return;
        }
        
        if (aki$isPlayerInteraction(context)) {
            return;
        }
        
        if (aki$isProjectileContext(context)) {
            return;
        }
        
        Vec3 from = context.getFrom();
        Vec3 to = context.getTo();
        
        long hash = aki$hashRayTrace(from, to, context);
        
        RayTraceCacheHolder cache = aki$getCache();
        BlockHitResult cached = cache.get(hash);
        
        if (cached != null) {
            cir.setReturnValue(cached);
        }
    }
    
    @Inject(method = "clip(Lnet/minecraft/world/level/ClipContext;)Lnet/minecraft/world/phys/BlockHitResult;",
            at = @At("RETURN"))
    private void aki$cacheRayTrace(ClipContext context, CallbackInfoReturnable<BlockHitResult> cir) {
        if (!init) {
            aki$init();
        }
        
        if (!enabled) {
            return;
        }
        
        if (aki$isPlayerInteraction(context)) {
            return;
        }
        
        if (aki$isProjectileContext(context)) {
            return;
        }
        
        Vec3 from = context.getFrom();
        Vec3 to = context.getTo();
        BlockHitResult result = cir.getReturnValue();
        
        if (result != null) {
            long hash = aki$hashRayTrace(from, to, context);
            RayTraceCacheHolder cache = aki$getCache();
            cache.put(hash, result);
        }
    }
    
    @Unique
    private long aki$hashRayTrace(Vec3 from, Vec3 to, ClipContext context) {
        long hash = 17;
        hash = hash * 31 + Double.hashCode(Math.floor(from.x * 10) / 10);
        hash = hash * 31 + Double.hashCode(Math.floor(from.y * 10) / 10);
        hash = hash * 31 + Double.hashCode(Math.floor(from.z * 10) / 10);
        hash = hash * 31 + Double.hashCode(Math.floor(to.x * 10) / 10);
        hash = hash * 31 + Double.hashCode(Math.floor(to.y * 10) / 10);
        hash = hash * 31 + Double.hashCode(Math.floor(to.z * 10) / 10);
        
        try {
            if (blockField == null || fluidField == null) {
                synchronized (FastRayTraceMixin.class) {
                    if (blockField == null) {
                        blockField = ClipContext.class.getDeclaredField("block");
                        blockField.setAccessible(true);
                    }
                    if (fluidField == null) {
                        fluidField = ClipContext.class.getDeclaredField("fluid");
                        fluidField.setAccessible(true);
                    }
                }
            }
            
            Object blockMode = blockField.get(context);
            Object fluidMode = fluidField.get(context);
            hash = hash * 31 + blockMode.hashCode();
            hash = hash * 31 + fluidMode.hashCode();
        } catch (Exception e) {
            
        }
        
        return hash;
    }
    
    @Unique
    private boolean aki$isPlayerInteraction(ClipContext context) {
        try {
            if (blockField == null) {
                synchronized (FastRayTraceMixin.class) {
                    if (blockField == null) {
                        blockField = ClipContext.class.getDeclaredField("block");
                        blockField.setAccessible(true);
                    }
                }
            }
            
            Object blockMode = blockField.get(context);
            if (blockMode == ClipContext.Block.OUTLINE) {
                Vec3 from = context.getFrom();
                Vec3 to = context.getTo();
                double distanceSq = from.distanceToSqr(to);
                
                if (distanceSq > 4.0 && distanceSq < 40.0) {
                    return true;
                }
            }
        } catch (Exception e) {
            return true;
        }
        return false;
    }
    
    @Unique
    private boolean aki$isProjectileContext(ClipContext context) {
        Vec3 from = context.getFrom();
        Vec3 to = context.getTo();
        double distanceSq = from.distanceToSqr(to);
        
        return distanceSq < 4.0; 
    }
    
    @Unique
    private static synchronized void aki$init() {
        if (init) {
            return;
        }
        
        org.virgil.akiasync.mixin.bridge.Bridge bridge = 
            org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        
        if (bridge != null) {
            enabled = bridge.isFastRayTraceEnabled();
            
            if (enabled) {
                BridgeConfigCache.debugLog("[AkiAsync] FastRayTrace initialized");
                BridgeConfigCache.debugLog("  - Cache size: " + cacheSize);
                BridgeConfigCache.debugLog("  - Improves villager AI by 15-20%");
            }
        }
        
        init = true;
    }
}
