package org.virgil.akiasync.mixin.mixins.entity.tracker;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.ref.WeakReference;

@Mixin(Projectile.class)
public abstract class ProjectileOwnerCacheMixin {

    @Shadow
    public abstract Entity getOwner();

    @Unique
    private WeakReference<Entity> aki$cachedOwner = null;

    @Unique
    private long aki$ownerCacheTime = 0;

    @Unique
    private static final long CACHE_DURATION = 100;

    @Inject(method = "getOwner", at = @At("HEAD"), cancellable = true)
    private void aki$getOwnerFromCache(CallbackInfoReturnable<Entity> cir) {

        if (!aki$isMainThread()) {

            if (aki$cachedOwner != null) {
                Entity cached = aki$cachedOwner.get();
                if (cached != null && !cached.isRemoved()) {
                    cir.setReturnValue(cached);
                    return;
                }
            }

            cir.setReturnValue(null);
        }
    }

    @Inject(method = "getOwner", at = @At("RETURN"))
    private void aki$cacheOwner(CallbackInfoReturnable<Entity> cir) {
        if (aki$isMainThread()) {
            Entity owner = cir.getReturnValue();
            if (owner != null) {
                aki$cachedOwner = new WeakReference<>(owner);
                aki$ownerCacheTime = System.currentTimeMillis();
            }
        }
    }

    @Unique
    private boolean aki$isMainThread() {

        String threadName = Thread.currentThread().getName();
        return threadName.equals("Server thread") ||
               threadName.startsWith("Server-Worker-") ||
               !threadName.startsWith("AkiAsync-");
    }
}
