package org.virgil.akiasync.mixin.util;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * TPS追踪器 - 基于TT20的实现
 * 
 * 功能：
 * 1. 精确追踪服务器TPS
 * 2. 计算丢失的tick数
 * 3. 提供平均TPS
 * 4. 不依赖Paper API
 * 
 * 参考：TT20 TPSCalculator
 */
public class TPSTracker {
    private Long lastTick;
    private Long currentTick;
    private double allMissedTicks = 0;
    private final List<Double> tpsHistory = new CopyOnWriteArrayList<>();
    
    private static final int HISTORY_LIMIT = 40;
    public static final int MAX_TPS = 20;
    public static final int FULL_TICK_MS = 50;
    
    private static TPSTracker instance;
    
    private TPSTracker() {}
    
    public static TPSTracker getInstance() {
        if (instance == null) {
            synchronized (TPSTracker.class) {
                if (instance == null) {
                    instance = new TPSTracker();
                }
            }
        }
        return instance;
    }
    
    /**
     * 每tick调用一次
     */
    public void onTick() {
        if (currentTick != null) {
            lastTick = currentTick;
        }
        
        currentTick = System.currentTimeMillis();
        addToHistory(getTPS());
        clearMissedTicks();
        missedTick();
    }
    
    /**
     * 计算丢失的tick
     */
    private void missedTick() {
        if (lastTick == null) return;
        
        long mspt = getMSPT();
        if (mspt <= FULL_TICK_MS) return;
        
        double missedTicks = (mspt / (double) FULL_TICK_MS) - 1;
        allMissedTicks += missedTicks;
    }
    
    /**
     * 获取可应用的丢失tick数（整数）
     */
    public int getApplicableMissedTicks() {
        return (int) Math.floor(allMissedTicks);
    }
    
    /**
     * 清理已应用的丢失tick
     */
    private void clearMissedTicks() {
        allMissedTicks -= getApplicableMissedTicks();
    }
    
    /**
     * 重置丢失的tick计数
     */
    public void resetMissedTicks() {
        allMissedTicks = 0;
    }
    
    /**
     * 获取所有丢失的tick（包含小数）
     */
    public double getAllMissedTicks() {
        return allMissedTicks;
    }
    
    /**
     * 获取MSPT（毫秒每tick）
     */
    public long getMSPT() {
        if (lastTick == null) return FULL_TICK_MS;
        long mspt = currentTick - lastTick;
        return mspt > 0 ? mspt : FULL_TICK_MS;
    }
    
    /**
     * 获取当前TPS
     */
    public double getTPS() {
        if (lastTick == null) return MAX_TPS;
        
        long mspt = getMSPT();
        if (mspt <= 0) return MAX_TPS;
        
        double tps = 1000.0 / mspt;
        return Math.min(tps, MAX_TPS);
    }
    
    /**
     * 获取平均TPS
     */
    public double getAverageTPS() {
        if (tpsHistory.isEmpty()) return MAX_TPS;
        
        return tpsHistory.stream()
            .mapToDouble(Double::doubleValue)
            .average()
            .orElse(MAX_TPS);
    }
    
    /**
     * 获取最准确的TPS（当前TPS和平均TPS中较小的）
     */
    public double getMostAccurateTPS() {
        double current = getTPS();
        double average = getAverageTPS();
        return current > average ? average : current;
    }
    
    /**
     * 添加TPS到历史记录
     */
    private void addToHistory(double tps) {
        if (tpsHistory.size() >= HISTORY_LIMIT) {
            tpsHistory.remove(0);
        }
        tpsHistory.add(tps);
    }
    
    /**
     * TT20计算 - 整数版本
     * 
     * @param ticks 原始tick数
     * @param limitZero 是否限制为非零
     * @return 补偿后的tick数
     */
    public int tt20(int ticks, boolean limitZero) {
        int newTicks = (int) Math.ceil(rawTT20(ticks));
        
        if (limitZero) {
            return newTicks > 0 ? newTicks : 1;
        }
        return newTicks;
    }
    
    /**
     * TT20计算 - 浮点版本
     */
    public float tt20(float ticks, boolean limitZero) {
        float newTicks = (float) rawTT20(ticks);
        
        if (limitZero) {
            return newTicks > 0 ? newTicks : 1;
        }
        return newTicks;
    }
    
    /**
     * TT20计算 - 双精度版本
     */
    public double tt20(double ticks, boolean limitZero) {
        double newTicks = rawTT20(ticks);
        
        if (limitZero) {
            return newTicks > 0 ? newTicks : 1;
        }
        return newTicks;
    }
    
    /**
     * 原始TT20计算
     * 公式：ticks * (currentTPS / 20.0)
     */
    public double rawTT20(double ticks) {
        if (ticks == 0) return 0;
        return ticks * getMostAccurateTPS() / MAX_TPS;
    }
    
    /**
     * 获取调试信息
     */
    public String getDebugInfo() {
        return String.format(
            "TPS: %.2f (avg: %.2f), MSPT: %dms, Missed: %.2f ticks",
            getTPS(), getAverageTPS(), getMSPT(), allMissedTicks
        );
    }
}
