package org.virgil.akiasync.mixin.pathfinding;

import net.minecraft.server.MinecraftServer;
import org.virgil.akiasync.mixin.bridge.Bridge;
import org.virgil.akiasync.mixin.bridge.BridgeManager;
import org.virgil.akiasync.mixin.util.BridgeConfigCache;

/**
 * 增强寻路系统初始化器
 * 
 * 负责在服务器启动时初始化增强寻路系统
 * 
 * @author AkiAsync
 */
public class EnhancedPathfindingInitializer {
    
    private static volatile boolean initialized = false;
    private static volatile boolean enabled = false;
    
    /**
     * 初始化增强寻路系统
     */
    public static synchronized void initialize() {
        if (initialized) return;
        
        Bridge bridge = BridgeManager.getBridge();
        if (bridge == null) {
            BridgeConfigCache.debugLog("[EnhancedPathfinding] Bridge not available, skipping initialization");
            return;
        }
        
        enabled = bridge.isAsyncPathfindingEnabled() && bridge.isEnhancedPathfindingEnabled();
        
        if (!enabled) {
            BridgeConfigCache.debugLog("[EnhancedPathfinding] Enhanced pathfinding is disabled");
            initialized = true;
            return;
        }
        
        int maxConcurrent = bridge.getEnhancedPathfindingMaxConcurrentRequests();
        int maxPerTick = bridge.getEnhancedPathfindingMaxRequestsPerTick();
        int highPriorityDist = bridge.getEnhancedPathfindingHighPriorityDistance();
        int mediumPriorityDist = bridge.getEnhancedPathfindingMediumPriorityDistance();
        
        EnhancedPathfindingSystem.updateConfig(
            enabled,
            maxConcurrent,
            maxPerTick,
            highPriorityDist,
            mediumPriorityDist
        );
        
        BridgeConfigCache.debugLog(String.format(
            "[EnhancedPathfinding] Initialized: maxConcurrent=%d, maxPerTick=%d, highPriority=%d, mediumPriority=%d",
            maxConcurrent, maxPerTick, highPriorityDist, mediumPriorityDist
        ));
        
        initialized = true;
    }
    
    /**
     * 每 tick 调用
     */
    public static void tick() {
        if (!initialized) {
            initialize();
        }
        
        if (enabled) {
            EnhancedPathfindingSystem.processTick();
        }
    }
    
    /**
     * 服务器关闭时清理
     */
    public static void shutdown() {
        if (enabled) {
            EnhancedPathfindingSystem.clear();
            BridgeConfigCache.debugLog("[EnhancedPathfinding] Shutdown complete");
        }
        initialized = false;
        enabled = false;
    }
    
    /**
     * 获取统计信息
     */
    public static String getStatistics() {
        if (!enabled) {
            return "EnhancedPathfinding: Disabled";
        }
        return EnhancedPathfindingSystem.getStatistics();
    }
    
    /**
     * 检查是否已启用
     */
    public static boolean isEnabled() {
        return enabled;
    }
}
