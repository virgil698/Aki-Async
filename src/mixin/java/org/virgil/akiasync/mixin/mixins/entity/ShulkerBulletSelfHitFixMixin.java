package org.virgil.akiasync.mixin.mixins.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 防御性修复：防止潜影贝子弹击中自己
 * Defensive Fix: Prevent Shulker bullets from hitting themselves
 * 
 * 问题背景 / Background:
 * 用户反馈：雪傀儡攻击末影螨时，潜影贝反击的子弹会击中自己。
 * 删除插件后问题消失，说明是插件导致的。
 * 
 * User reported: When snow golems attack endermites, shulker bullets hit the shulker itself.
 * Problem disappears after removing plugin, indicating it's caused by the plugin.
 * 
 * 根本原因 / Root Cause:
 * FastRayTraceMixin 的射线追踪缓存导致碰撞检测异常。
 * 已通过修改 FastRayTraceMixin 排除抛射物缓存来解决。
 * 
 * FastRayTraceMixin's raytrace cache causes collision detection issues.
 * Fixed by excluding projectile raytrace from cache in FastRayTraceMixin.
 * 
 * 本修复的作用 / Purpose of This Fix:
 * 作为双重保险，即使缓存修复失效，也能防止潜影贝自伤。
 * 性能开销极小（仅一个 if 判断），但提供了最后一道防线。
 * 
 * Acts as a safety net - even if cache fix fails, prevents shulker self-damage.
 * Minimal performance cost (just one if check), but provides last line of defense.
 * 
 * 用户反馈 / User Report:
 * "雪傀儡攻击末影螨，然后会有概率攻击到潜影贝，然后潜影贝攻击雪傀儡，
 *  由于方向的问题，潜影贝只能攻击到自己"
 * "删掉插件就没这个问题了"
 */
@Mixin(ShulkerBullet.class)
public class ShulkerBulletSelfHitFixMixin {
    
    @Unique
    private static volatile boolean enabled = true;
    
    @Unique
    private static volatile boolean init = false;
    
    /**
     * 防止潜影贝子弹击中发射它的潜影贝
     * Prevent shulker bullets from hitting their shooter
     * 
     * @param target 目标实体 / Target entity
     * @param cir 回调信息 / Callback info
     */
    @Inject(
        method = "canHitEntity",
        at = @At("HEAD"),
        cancellable = true
    )
    private void preventSelfHit(Entity target, CallbackInfoReturnable<Boolean> cir) {
        if (!init) {
            aki$init();
        }
        
        if (!enabled) {
            return;
        }
        
        ShulkerBullet bullet = (ShulkerBullet) (Object) this;
        Entity owner = bullet.getOwner();
        
        if (owner != null && target == owner) {
            cir.setReturnValue(false);
        }
    }
    
    @Unique
    private static synchronized void aki$init() {
        if (init) {
            return;
        }
        
        org.virgil.akiasync.mixin.bridge.Bridge bridge = 
            org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        
        if (bridge != null) {
            enabled = bridge.isShulkerBulletSelfHitFixEnabled();
            
            if (enabled && bridge.isDebugLoggingEnabled()) {
                bridge.debugLog("[AkiAsync] ShulkerBulletSelfHitFix enabled");
                bridge.debugLog("  - Fixes vanilla bug where shulker bullets can hit their shooter");
            }
        }
        
        init = true;
    }
}
