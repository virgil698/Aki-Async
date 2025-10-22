package org.virgil.akiasync.mixin.mixins.entity;
import java.util.List;
import java.util.function.Predicate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
@SuppressWarnings("unused")
@Mixin(Level.class)
public abstract class EntityLookupCacheMixin {
    private static volatile boolean enabled;
    private static volatile int cacheDuration;
    private static volatile boolean initialized = false;
    private List<Entity> cachedEntities;
    private AABB cachedBox;
    private long cacheTime;
    @Inject(method = "getEntities(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;", at = @At("HEAD"), cancellable = true)
    private void cacheGetEntities(Entity except, AABB box, Predicate<? super Entity> predicate, CallbackInfoReturnable<List<Entity>> cir) {
        if (!initialized) { akiasync$initLookupCache(); }
        if (!enabled) return;
        long now = System.currentTimeMillis();
        if (cachedEntities != null && 
            cachedBox != null && 
            cachedBox.equals(box) && 
            now - cacheTime < cacheDuration) {
            List<Entity> filtered = new java.util.ArrayList<>();
            for (Entity entity : cachedEntities) {
                if (entity != except && (predicate == null || predicate.test(entity))) {
                    filtered.add(entity);
                }
            }
            cir.setReturnValue(filtered);
        }
    }
    @Inject(method = "getEntities(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;", at = @At("RETURN"))
    private void updateCache(Entity except, AABB box, Predicate<? super Entity> predicate, CallbackInfoReturnable<List<Entity>> cir) {
        if (!enabled) return;
        cachedEntities = cir.getReturnValue();
        cachedBox = box;
        cacheTime = System.currentTimeMillis();
    }
    private static synchronized void akiasync$initLookupCache() {
        if (initialized) return;
        org.virgil.akiasync.mixin.bridge.Bridge bridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        if (bridge != null) {
            enabled = bridge.isEntityLookupCacheEnabled();
            cacheDuration = bridge.getEntityLookupCacheDurationMs();
        } else {
            enabled = true;
            cacheDuration = 50;
        }
        initialized = true;
        System.out.println("[AkiAsync] EntityLookupCacheMixin initialized: enabled=" + enabled + ", duration=" + cacheDuration + "ms");
    }
}