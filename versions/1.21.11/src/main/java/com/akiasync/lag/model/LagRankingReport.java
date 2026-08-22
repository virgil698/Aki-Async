package com.akiasync.lag;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public record LagRankingReport(
        int inspectedTicks,
        List<LagSourceRanking> sources,
        List<LagStackRanking> stackHotspots,
        Map<LagSeverity, Integer> severityCounts
) {
    public LagRankingReport {
        sources = List.copyOf(sources);
        stackHotspots = List.copyOf(stackHotspots);
        EnumMap<LagSeverity, Integer> counts = new EnumMap<>(LagSeverity.class);
        counts.putAll(severityCounts);
        severityCounts = Collections.unmodifiableMap(counts);
    }
}
