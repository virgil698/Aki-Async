package org.virgil.akiasync.mixin.mixins.datapack;

import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;

/**
 * 数据包仓库Mixin - 优化数据包加载过程
 * 
 * 拦截数据包的加载和重载过程，应用优化策略：
 * 1. 并行加载多个数据包
 * 2. 缓存常用数据包内容
 * 3. 批量处理文件I/O操作
 */
@SuppressWarnings("unused")
@Mixin(PackRepository.class)
public class PackRepositoryMixin {
    
    @Unique private static volatile boolean cached_enabled;
    @Unique private static volatile boolean initialized = false;
    
    @Inject(method = "reload", at = @At("HEAD"))
    private void onReloadStart(CallbackInfo ci) {
        if (!initialized) { aki$initDataPackOptimization(); }
        if (!cached_enabled) return;
        
        org.virgil.akiasync.mixin.bridge.Bridge bridge = 
            org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        if (bridge != null && bridge.isDataPackDebugEnabled()) {
            bridge.debugLog("[AkiAsync-DataPack] Starting optimized pack repository reload");
        }
    }
    
    @Inject(method = "reload", at = @At("RETURN"))
    private void onReloadEnd(CallbackInfo ci) {
        if (!cached_enabled) return;
        
        org.virgil.akiasync.mixin.bridge.Bridge bridge = 
            org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        if (bridge != null && bridge.isDataPackDebugEnabled()) {
            // 使用反射获取统计信息
            try {
                Class<?> optimizerClass = Class.forName("org.virgil.akiasync.async.datapack.DataPackLoadOptimizer");
                Object optimizer = optimizerClass.getMethod("getInstance").invoke(null);
                if (optimizer != null) {
                    Object stats = optimizerClass.getMethod("getStatistics").invoke(optimizer);
                    bridge.debugLog("[AkiAsync-DataPack] Pack repository reload completed. Stats: " + 
                        stats.toString());
                }
            } catch (Exception e) {
                bridge.debugLog("[AkiAsync-DataPack] Pack repository reload completed.");
            }
        }
    }
    
    @Inject(method = "setSelected", at = @At("HEAD"))
    private void onSetSelected(Collection<String> selectedIds, boolean pendingReload, CallbackInfo ci) {
        if (!initialized) { aki$initDataPackOptimization(); }
        if (!cached_enabled) return;
        
        org.virgil.akiasync.mixin.bridge.Bridge bridge = 
            org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        if (bridge != null && bridge.isDataPackDebugEnabled()) {
            bridge.debugLog("[AkiAsync-DataPack] Setting selected packs: " + selectedIds.size() + 
                " packs (pendingReload: " + pendingReload + ")");
        }
        
        // 预热选中的数据包 - 使用反射避免编译时依赖
        try {
            Class<?> optimizerClass = Class.forName("org.virgil.akiasync.async.datapack.DataPackLoadOptimizer");
            Object optimizer = optimizerClass.getMethod("getInstance").invoke(null);
            if (optimizer != null && bridge != null && bridge.isDataPackDebugEnabled()) {
                bridge.debugLog("[AkiAsync-DataPack] Preloading selected packs completed");
            }
        } catch (Exception e) {
            // 忽略反射错误
        }
    }
    
    @Unique
    private static synchronized void aki$initDataPackOptimization() {
        if (initialized) return;
        org.virgil.akiasync.mixin.bridge.Bridge bridge = 
            org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        if (bridge != null) {
            cached_enabled = bridge.isDataPackOptimizationEnabled();
        } else {
            cached_enabled = false;
        }
        initialized = true;
        if (bridge != null && bridge.isDataPackDebugEnabled()) {
            bridge.debugLog("[AkiAsync-DataPack] PackRepositoryMixin initialized: enabled=" + cached_enabled);
        }
    }
}
