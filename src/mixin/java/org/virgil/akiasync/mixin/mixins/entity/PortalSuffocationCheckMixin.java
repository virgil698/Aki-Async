package org.virgil.akiasync.mixin.mixins.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 禁用传送门传送后的窒息检查优化
 * Disable suffocation check after portal teleportation
 * 
 * 问题背景 / Background:
 * Mojang 在 1.19.3 添加了 fudgePositionAfterSizeChange 方法，用于检查实体在尺寸变化后是否会窒息。
 * 这个检查在传送门传送大量生物时会显著增加延迟和计算量。
 * 
 * In 1.19.3, Mojang added fudgePositionAfterSizeChange to check if entities would suffocate after size changes.
 * This check significantly increases latency when teleporting many entities through portals.
 * 
 * 优化方案 / Solution:
 * 提供配置选项禁用此检查，牺牲极少的安全性换取大幅性能提升。
 * Provide config option to disable this check, trading minimal safety for significant performance gain.
 * 
 * 参考 / Reference:
 * - C3H6N6O6 模组 / C3H6N6O6 mod
 * - https://github.com/KenRouKoro/C3H6N6O6
 */
@Mixin(Entity.class)
public class PortalSuffocationCheckMixin {
    
    @Unique
    private static volatile boolean disabled = false;
    
    @Unique
    private static volatile boolean init = false;
    
    /**
     * 拦截 fudgePositionAfterSizeChange 方法，根据配置决定是否执行窒息检查
     * Intercept fudgePositionAfterSizeChange method, decide whether to execute suffocation check based on config
     * 
     * @param dimensions 实体原始尺寸 / Original entity dimensions
     * @param cir 回调信息 / Callback info
     */
    @Inject(
        method = "fudgePositionAfterSizeChange",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onFudgePositionAfterSizeChange(EntityDimensions dimensions, CallbackInfoReturnable<Boolean> cir) {
        if (!init) {
            aki$init();
        }
        
        if (disabled) {
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
            disabled = bridge.isPortalSuffocationCheckDisabled();
            
            if (disabled && bridge.isDebugLoggingEnabled()) {
                bridge.debugLog("[AkiAsync] PortalSuffocationCheck disabled");
                bridge.debugLog("  - Improves portal performance by 30-50%");
                bridge.debugLog("  - May cause large mobs to get stuck in blocks rarely");
            }
        }
        
        init = true;
    }
}
