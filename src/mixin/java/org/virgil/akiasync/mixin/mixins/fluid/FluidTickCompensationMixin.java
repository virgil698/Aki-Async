package org.virgil.akiasync.mixin.mixins.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 流体Tick补偿优化 - 基于TT20算法
 * 
 * 参考 Leaf 的实现思路：
 * 1. 根据服务器TPS动态调整流体流速
 * 2. TPS低时启用滞后补偿，保持流体行为一致
 * 3. TPS正常时使用标准流速
 * 
 * 核心算法：
 * - 计算补偿系数 = rawTT20(ticks) / MAX_TPS
 * - 当TPS < 阈值时，启用滞后补偿
 * - 调整流体的 spreadDelay 来补偿性能损失
 */
@Mixin(FlowingFluid.class)
public abstract class FluidTickCompensationMixin {

    @Unique
    private static volatile boolean enabled;
    @Unique
    private static volatile boolean enableForWater;
    @Unique
    private static volatile boolean enableForLava;
    @Unique
    private static volatile double tpsThreshold;
    @Unique
    private static volatile boolean initialized = false;

    /**
     * 修改流体扩散延迟以实现TT20补偿
     * 
     * 重定向 scheduleTick 调用，修改 delay 参数
     * 对应源码第504行: level.scheduleTick(pos, newLiquid.getType(), spreadDelay);
     */
    @org.spongepowered.asm.mixin.injection.Redirect(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;scheduleTick(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/material/Fluid;I)V"
        )
    )
    private void compensateFluidSpread(ServerLevel level, BlockPos pos, net.minecraft.world.level.material.Fluid fluid, int originalDelay) {
        if (!initialized) {
            akiasync$initCompensation();
        }

        int finalDelay = originalDelay;
        
        if (enabled) {
            try {
                
                FluidState fluidState = level.getFluidState(pos);
                
                if (akiasync$shouldCompensate(fluidState)) {
                    
                    double currentTPS = akiasync$getCurrentTPS(level);
                    
                    if (currentTPS < tpsThreshold) {
                        
                        finalDelay = akiasync$tt20(originalDelay, true, currentTPS);
                    }
                }
            } catch (Throwable t) {
                
            }
        }
        
        level.scheduleTick(pos, fluid, finalDelay);
    }

    /**
     * 检查是否应该对这种流体启用补偿
     */
    @Unique
    private boolean akiasync$shouldCompensate(FluidState state) {
        try {
            String fluidType = state.getType().toString();
            
            if (fluidType.contains("water") || fluidType.contains("flowing_water")) {
                return enableForWater;
            }
            
            if (fluidType.contains("lava") || fluidType.contains("flowing_lava")) {
                return enableForLava;
            }
            
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取当前服务器TPS
     */
    @Unique
    @SuppressWarnings("removal") 
    private double akiasync$getCurrentTPS(ServerLevel level) {
        try {
            
            net.minecraft.server.MinecraftServer server = level.getServer();
            if (server != null) {
                
                double[] recentTps = server.recentTps;
                if (recentTps != null && recentTps.length > 0) {
                    return Math.min(recentTps[0], 20.0);
                }
            }
        } catch (Exception e) {
            
        }
        return 20.0; 
    }

    /**
     * 计算TT20补偿后的tick数
     * 
     * @param ticks 原始tick数
     * @param limitZero 是否限制为非零
     * @return 补偿后的tick数
     */
    @Unique
    private static int akiasync$tt20(int ticks, boolean limitZero, double currentTPS) {
        int newTicks = (int) Math.ceil(akiasync$rawTT20(ticks, currentTPS));
        return limitZero ? (newTicks > 0 ? newTicks : 1) : newTicks;
    }

    /**
     * 原始TT20计算
     */
    @Unique
    private static double akiasync$rawTT20(double ticks, double currentTPS) {
        if (ticks == 0) {
            return 0;
        }
        
        return ticks * currentTPS / 20.0;
    }

    /**
     * 初始化配置
     */
    @Unique
    private static synchronized void akiasync$initCompensation() {
        if (initialized) return;

        org.virgil.akiasync.mixin.bridge.Bridge bridge =
            org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();

        if (bridge != null) {
            enabled = bridge.isFluidTickCompensationEnabled();
            enableForWater = bridge.isFluidCompensationEnabledForWater();
            enableForLava = bridge.isFluidCompensationEnabledForLava();
            tpsThreshold = bridge.getFluidCompensationTPSThreshold();
        } else {
            
            enabled = false; 
            enableForWater = false;
            enableForLava = false;
            tpsThreshold = 18.0; 
        }

        initialized = true;

        if (bridge != null) {
            bridge.debugLog("[AkiAsync] FluidTickCompensationMixin initialized: enabled=" + enabled +
                ", enableForWater=" + enableForWater +
                ", enableForLava=" + enableForLava +
                ", tpsThreshold=" + tpsThreshold);
        }
    }
}
