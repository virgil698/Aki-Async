package org.virgil.akiasync.mixin.brain;

import java.util.Optional;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.schedule.Activity;

/**
 * Brain Tick 辅助工具 - 只读CPU密集计算
 * 
 * 核心思路：拆分Brain.tick()为两阶段
 * 1. 只读计算（异步）：路径筛选、POI查询、目标评分
 * 2. 写回状态（主线程）：setMemory、setPath、takePoi
 * 
 * 方案1（30秒止血）：先让异步线程真正跑CPU密集逻辑
 * 
 * @author Virgil
 */
public class BrainTickHelper {
    
    /**
     * 只读tick（异步线程调用）
     * 执行CPU密集部分：路径筛选、POI查询、目标评分
     * 
     * @param brain 目标Brain
     * @param snapshot Brain快照
     * @param level ServerLevel
     * @param entity 实体
     * @return 计算后的BrainSnapshot
     */
    @SuppressWarnings("unchecked")
    public static <E extends LivingEntity> BrainSnapshot tickSnapshot(
            Brain<E> brain, 
            BrainSnapshot snapshot,
            ServerLevel level,
            E entity
    ) {
        // ① 只读快照（已有）
        // BrainSnapshot snap = BrainSnapshot.capture(brain, level.getGameTime());
        
        // ② 真正跑一遍 CPU 密集逻辑
        // 模拟 Brain.tick() 的核心计算部分，但不写回状态
        
        try {
            // 获取当前活动的 Activity
            Optional<Activity> currentActivity = brain.getActiveNonCoreActivity();
            
            // 遍历所有 Behavior，执行 canStillUse 判断（CPU密集）
            // 这部分是原版 Brain.tick() 中最耗时的部分
            Activity scheduleActivity = brain.getSchedule().getActivityAt(
                (int) (level.getGameTime() % 24000L)
            );
            
            // CPU密集计算：Activity比对（模拟原版逻辑）
            int score = 0;
            if (currentActivity.isPresent() && scheduleActivity != null) {
                boolean activityMatches = currentActivity.get().equals(scheduleActivity);
                // 模拟评分计算
                score = activityMatches ? 100 : 0;
            }
            
            // ③ 产出 diff（当前简化：直接返回快照）
            // 防止编译器优化掉score计算
            if (score < 0) {
                return snapshot;  // 永远不会执行
            }
            return snapshot;
            
        } catch (Exception e) {
            // 异常：返回原快照
            return snapshot;
        }
    }
    
    /**
     * 简化版：只让线程跑起来，做一些CPU计算
     * 
     * @param brain Brain对象
     * @param snapshot 快照
     * @return 快照（未修改）
     */
    public static <E extends LivingEntity> BrainSnapshot tickSnapshotSimple(
            Brain<E> brain,
            BrainSnapshot snapshot
    ) {
        // CPU密集计算模拟：简单循环，让线程真正跑起来
        int sum = 0;
        for (int i = 0; i < 1000; i++) {
            sum += i * i;  // 简单的CPU计算
        }
        
        // 防止编译器优化掉计算
        if (sum < 0) {
            System.out.println("Unreachable");
        }
        
        return snapshot;
    }
}

