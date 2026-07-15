package com.akiasync.lag;

public enum LagSeverity {
    NORMAL("正常", 0, 50),
    MINOR("轻微", 50, 75),
    MODERATE("中度", 75, 150),
    SEVERE("严重", 150, 500),
    CRITICAL("致命", 500, Long.MAX_VALUE);

    private final String displayName;
    private final long minimumMillis;
    private final long maximumMillis;

    LagSeverity(String displayName, long minimumMillis, long maximumMillis) {
        this.displayName = displayName;
        this.minimumMillis = minimumMillis;
        this.maximumMillis = maximumMillis;
    }

    public String displayName() {
        return displayName;
    }

    public long minimumMillis() {
        return minimumMillis;
    }

    public long maximumMillis() {
        return maximumMillis;
    }

    public static LagSeverity fromNanos(long nanos) {
        double millis = nanos / 1_000_000.0;
        if (millis >= 500) {
            return CRITICAL;
        }
        if (millis >= 150) {
            return SEVERE;
        }
        if (millis >= 75) {
            return MODERATE;
        }
        if (millis >= 50) {
            return MINOR;
        }
        return NORMAL;
    }
}
