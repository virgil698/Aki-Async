package org.virgil.akiasync.mixin.util;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class TPSTracker {
    private Long lastTick;
    private Long currentTick;
    private double allMissedTicks = 0;
    private final List<Double> tpsHistory = new CopyOnWriteArrayList<>();

    private static final int HISTORY_LIMIT = 40;
    public static final int MAX_TPS = 20;
    public static final int FULL_TICK_MS = 50;

    private static TPSTracker instance;

    private TPSTracker() {
        this.lastTick = null;
        this.currentTick = null;
        this.allMissedTicks = 0;
    }

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

    public void onTick() {
        if (currentTick != null) {
            lastTick = currentTick;
        }

        currentTick = System.currentTimeMillis();
        addToHistory(getTPS());
        clearMissedTicks();
        missedTick();
    }

    private void missedTick() {
        if (lastTick == null) return;

        long mspt = getMSPT();
        if (mspt <= FULL_TICK_MS) return;

        double missedTicks = (mspt / (double) FULL_TICK_MS) - 1;
        allMissedTicks += missedTicks;
    }

    public int getApplicableMissedTicks() {
        return (int) Math.floor(allMissedTicks);
    }

    private void clearMissedTicks() {
        allMissedTicks -= getApplicableMissedTicks();
    }

    public void resetMissedTicks() {
        allMissedTicks = 0;
    }

    public double getAllMissedTicks() {
        return allMissedTicks;
    }

    public long getMSPT() {
        if (lastTick == null) return FULL_TICK_MS;
        long mspt = currentTick - lastTick;
        return mspt > 0 ? mspt : FULL_TICK_MS;
    }

    public double getTPS() {
        if (lastTick == null) return MAX_TPS;

        long mspt = getMSPT();
        if (mspt <= 0) return MAX_TPS;

        double tps = 1000.0 / mspt;
        return Math.min(tps, MAX_TPS);
    }

    public double getAverageTPS() {
        if (tpsHistory.isEmpty()) return MAX_TPS;

        return tpsHistory.stream()
            .mapToDouble(Double::doubleValue)
            .average()
            .orElse(MAX_TPS);
    }

    public double getMostAccurateTPS() {
        double current = getTPS();
        double average = getAverageTPS();
        return current > average ? average : current;
    }

    private void addToHistory(double tps) {
        if (tpsHistory.size() >= HISTORY_LIMIT) {
            tpsHistory.remove(0);
        }
        tpsHistory.add(tps);
    }

    public int tt20(int ticks, boolean limitZero) {
        int newTicks = (int) Math.ceil(rawTT20(ticks));

        if (limitZero) {
            return newTicks > 0 ? newTicks : 1;
        }
        return newTicks;
    }

    public float tt20(float ticks, boolean limitZero) {
        float newTicks = (float) rawTT20(ticks);

        if (limitZero) {
            return newTicks > 0 ? newTicks : 1;
        }
        return newTicks;
    }

    public double tt20(double ticks, boolean limitZero) {
        double newTicks = rawTT20(ticks);

        if (limitZero) {
            return newTicks > 0 ? newTicks : 1;
        }
        return newTicks;
    }

    public double rawTT20(double ticks) {
        if (ticks == 0) return 0;
        return ticks * getMostAccurateTPS() / MAX_TPS;
    }

    public String getDebugInfo() {
        return String.format(
            "TPS: %.2f (avg: %.2f), MSPT: %dms, Missed: %.2f ticks",
            getTPS(), getAverageTPS(), getMSPT(), allMissedTicks
        );
    }
}
