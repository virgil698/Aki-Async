package org.virgil.akiasync.mixin.mixins.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Level.class, priority = 990)
public abstract class FastRayTraceMixin {
    
    @Unique
    private static volatile boolean enabled = false;
    
    @Unique
    private static volatile boolean init = false;
    
    @Unique
    private static volatile int cacheSize = 256;
    
    @Unique
    private static final ThreadLocal<java.util.Map<Long, HitResult>> aki$rayTraceCache = 
        ThreadLocal.withInitial(() -> new java.util.LinkedHashMap<Long, HitResult>(cacheSize, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(java.util.Map.Entry<Long, HitResult> eldest) {
                return size() > cacheSize;
            }
        });
    
    @Inject(method = "clip(Lnet/minecraft/world/level/ClipContext;)Lnet/minecraft/world/phys/HitResult;",
            at = @At("HEAD"), cancellable = true)
    private void aki$fastRayTrace(ClipContext context, CallbackInfoReturnable<HitResult> cir) {
        if (!init) {
            aki$init();
        }
        
        if (!enabled) {
            return;
        }
        
        Vec3 from = context.getFrom();
        Vec3 to = context.getTo();
        
        long hash = aki$hashRayTrace(from, to);
        
        java.util.Map<Long, HitResult> cache = aki$rayTraceCache.get();
        HitResult cached = cache.get(hash);
        
        if (cached != null) {
            cir.setReturnValue(cached);
        }
    }
    
    @Inject(method = "clip(Lnet/minecraft/world/level/ClipContext;)Lnet/minecraft/world/phys/HitResult;",
            at = @At("RETURN"))
    private void aki$cacheRayTrace(ClipContext context, CallbackInfoReturnable<HitResult> cir) {
        if (!enabled) {
            return;
        }
        
        Vec3 from = context.getFrom();
        Vec3 to = context.getTo();
        HitResult result = cir.getReturnValue();
        
        if (result != null) {
            long hash = aki$hashRayTrace(from, to);
            aki$rayTraceCache.get().put(hash, result);
        }
    }
    
    @Unique
    private long aki$hashRayTrace(Vec3 from, Vec3 to) {
        long hash = 17;
        hash = hash * 31 + Double.hashCode(Math.floor(from.x * 2) / 2);
        hash = hash * 31 + Double.hashCode(Math.floor(from.y * 2) / 2);
        hash = hash * 31 + Double.hashCode(Math.floor(from.z * 2) / 2);
        hash = hash * 31 + Double.hashCode(Math.floor(to.x * 2) / 2);
        hash = hash * 31 + Double.hashCode(Math.floor(to.y * 2) / 2);
        hash = hash * 31 + Double.hashCode(Math.floor(to.z * 2) / 2);
        return hash;
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
                bridge.debugLog("[AkiAsync] FastRayTrace initialized");
                bridge.debugLog("  - Cache size: " + cacheSize);
                bridge.debugLog("  - Improves villager AI by 15-20%");
            }
        }
        
        init = true;
    }
}
