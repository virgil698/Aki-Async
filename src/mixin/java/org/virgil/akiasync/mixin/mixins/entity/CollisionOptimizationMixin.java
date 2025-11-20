package org.virgil.akiasync.mixin.mixins.entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.world.entity.Entity;
@SuppressWarnings("unused")
@Mixin(Entity.class)
public abstract class CollisionOptimizationMixin {
    private static volatile boolean enabled;
    private static volatile double minMovement = 0.001D;
    private static volatile boolean initialized = false;
    @Inject(method = "checkInsideBlocks", at = @At("HEAD"), cancellable = true)
    private void optimizeBlockCollision(CallbackInfo ci) {
        if (!initialized) { akiasync$initCollisionOptimization(); }
        if (!enabled) return;
        Entity self = (Entity) (Object) this;
        
        // 岩浆或火焰中的实体必须每tick检测，确保持续伤害
        if (self.isInLava() || self.isOnFire() || self.getRemainingFireTicks() > 0) {
            return; // 不跳过检测
        }
        
        if (self.getDeltaMovement().lengthSqr() < minMovement) {
            ci.cancel();
        }
    }
    @Inject(method = "push(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
    private void optimizeEntityPush(Entity other, CallbackInfo ci) {
        if (!enabled) return;
        Entity self = (Entity) (Object) this;
        if (self.getDeltaMovement().lengthSqr() < minMovement && 
            other.getDeltaMovement().lengthSqr() < minMovement) {
            ci.cancel();
        }
    }
    private static synchronized void akiasync$initCollisionOptimization() {
        if (initialized) return;
        org.virgil.akiasync.mixin.bridge.Bridge bridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        if (bridge != null) {
            enabled = bridge.isCollisionOptimizationEnabled();
        } else {
            enabled = true;
        }
        initialized = true;
        if (bridge != null) {
            bridge.debugLog("[AkiAsync] CollisionOptimizationMixin initialized: enabled=" + enabled);
        }
    }
}