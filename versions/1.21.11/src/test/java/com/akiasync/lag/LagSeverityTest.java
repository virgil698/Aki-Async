package com.akiasync.lag;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LagSeverityTest {
    @Test
    void usesExactSeverityBoundaries() {
        assertEquals(LagSeverity.NORMAL, LagSeverity.fromNanos(49_999_999L));
        assertEquals(LagSeverity.MINOR, LagSeverity.fromNanos(50_000_000L));
        assertEquals(LagSeverity.MODERATE, LagSeverity.fromNanos(75_000_000L));
        assertEquals(LagSeverity.SEVERE, LagSeverity.fromNanos(150_000_000L));
        assertEquals(LagSeverity.CRITICAL, LagSeverity.fromNanos(500_000_000L));
    }
}
