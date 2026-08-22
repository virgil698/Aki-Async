package com.akiasync.lag;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LagProfilerServiceTest {
    @Test
    void ranksTimedSourcesByWindowTotalAndWorstTick() {
        LagProfilerService service = new LagProfilerService();
        service.accept(tick(1, 100_000_000L, List.of(
                source(LagCauseCategory.MOB, "MOB", "minecraft:zombie", 2, 20_000_000L, 12_000_000L, 1),
                source(LagCauseCategory.MOB, "MOB", "minecraft:zombie", 3, 30_000_000L, 20_000_000L, 2),
                source(LagCauseCategory.PLUGIN, "PLUGIN_EVENT", "PlayerMoveEvent", 1, 40_000_000L, 40_000_000L, 0),
                source(LagCauseCategory.UNKNOWN, "WORLD", "world tick", 1, 100_000_000L, 100_000_000L, 0)
        ), List.of(stack("plugin.Main.onMove", 3, "plugin.Main.onMove(Main.java:10)"))));
        service.accept(tick(2, 200_000_000L, List.of(
                source(LagCauseCategory.MOB, "MOB", "minecraft:zombie", 1, 70_000_000L, 70_000_000L, 3),
                source(LagCauseCategory.PLUGIN, "PLUGIN_EVENT", "PlayerMoveEvent", 1, 10_000_000L, 10_000_000L, 0)
        ), List.of(
                stack("plugin.Main.onMove", 2, "plugin.Main.onMove(Main.java:10)"),
                stack("plugin.Main.onMove", 4, "plugin.Main.onMove(Main.java:20)")
        )));

        LagRankingReport report = service.rank(200, 10);

        assertEquals(2, report.inspectedTicks());
        assertEquals(2, report.sources().size());
        LagSourceRanking zombies = report.sources().getFirst();
        assertEquals("minecraft:zombie", zombies.detail());
        assertEquals(6, zombies.occurrences());
        assertEquals(2, zombies.affectedTicks());
        assertEquals(120_000_000L, zombies.totalNanos());
        assertEquals(70_000_000L, zombies.worstTickNanos());
        assertEquals(70_000_000L, zombies.maxOccurrenceNanos());
        assertTrue(zombies.hasWorstLocation());
        assertEquals(3, zombies.x());
        assertEquals(1, report.severityCounts().get(LagSeverity.MODERATE));
        assertEquals(1, report.severityCounts().get(LagSeverity.SEVERE));
    }

    @Test
    void keepsDifferentStackPathsSeparateWhileAggregatingExactMatches() {
        LagProfilerService service = new LagProfilerService();
        service.accept(tick(1, 80_000_000L, List.of(), List.of(
                stack("plugin.Main.onMove", 3, "plugin.Main.onMove(Main.java:10)")
        )));
        service.accept(tick(2, 90_000_000L, List.of(), List.of(
                stack("plugin.Main.onMove", 2, "plugin.Main.onMove(Main.java:10)"),
                stack("plugin.Main.onMove", 4, "plugin.Main.onMove(Main.java:20)")
        )));

        List<LagStackRanking> stacks = service.rank(200, 10).stackHotspots();

        assertEquals(2, stacks.size());
        assertEquals(5, stacks.getFirst().samples());
        assertEquals(2, stacks.getFirst().affectedTicks());
        assertEquals("plugin.Main.onMove(Main.java:10)", stacks.getFirst().frames().getFirst());
        assertEquals(4, stacks.get(1).samples());
        assertEquals("plugin.Main.onMove(Main.java:20)", stacks.get(1).frames().getFirst());
    }

    private static LagTickRecord tick(
            long tickId,
            long wallNanos,
            List<LagSourceRecord> sources,
            List<LagStackRecord> stacks
    ) {
        return new LagTickRecord(
                tickId, 0, wallNanos, wallNanos, wallNanos >= 50_000_000L, true,
                sources, stacks, new LagSystemRecord(0, 0, 0, 0, 0)
        );
    }

    private static LagSourceRecord source(
            LagCauseCategory category,
            String type,
            String detail,
            int count,
            long totalNanos,
            long maxNanos,
            int x
    ) {
        String owner = category == LagCauseCategory.PLUGIN ? "ExamplePlugin" : "";
        boolean hasLocation = category == LagCauseCategory.MOB;
        return new LagSourceRecord(
                category, type, owner, detail, "minecraft:overworld", hasLocation, x, 64, 0,
                count, totalNanos, maxNanos
        );
    }

    private static LagStackRecord stack(String source, int samples, String frame) {
        return new LagStackRecord(LagCauseCategory.PLUGIN, source, samples, List.of(frame, "server.tick(Server.java:1)"));
    }
}
