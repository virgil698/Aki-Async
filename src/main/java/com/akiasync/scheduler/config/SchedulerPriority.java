package com.akiasync.scheduler.config;

import ca.spottedleaf.concurrentutil.util.Priority;

public enum SchedulerPriority {
    CRITICAL(Priority.HIGHEST),
    HIGH(Priority.HIGH),
    NORMAL(Priority.NORMAL),
    LOW(Priority.LOW),
    BACKGROUND(Priority.LOWEST);

    private final Priority delegate;

    SchedulerPriority(Priority delegate) {
        this.delegate = delegate;
    }

    public Priority delegate() {
        return delegate;
    }
}
