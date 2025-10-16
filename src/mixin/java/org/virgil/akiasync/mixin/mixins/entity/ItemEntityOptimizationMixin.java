package org.virgil.akiasync.mixin.mixins.entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;

/**
 * 零扫描、零破坏、生产级安全的 ItemEntity 优化
 * 只动自身状态，不扫世界、不扫邻居
 * 
 * @author Virgil
 */
@SuppressWarnings("unused")
@Mixin(value = ItemEntity.class, priority = 989)
public class ItemEntityOptimizationMixin {
    
    /**
     * 1. tick 节流: 每 10 tick 才执行一次逻辑帧
     * 完全跳过原版每 tick 的合并、拾取、年龄增长
     */
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void aki$throttleTick(CallbackInfo ci) {
        ItemEntity self = (ItemEntity) (Object) this;
        if (self.level() instanceof ServerLevel sl && sl.getGameTime() % 10 != 0) {
            ci.cancel(); // 跳过整个 tick，零扫描
        }
    }
    
    /**
     * 2. 跳过自身合并（不扫邻居）
     * 直接跳过 tryToMerge，让原版逻辑短路
     */
    @Inject(method = "tryToMerge", at = @At("HEAD"), cancellable = true)
    private void aki$skipSelfMerge(CallbackInfo ci) {
        ci.cancel(); // 不合并，不扫邻居
    }
}

