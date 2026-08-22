package com.akiasync.lag;

import com.akiasync.lag.*;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class LagProfilerService {
    private static final int HISTORY_LIMIT = 600;
    private final Deque<LagTickRecord> history = new ArrayDeque<>(HISTORY_LIMIT);

    public synchronized void accept(LagTickRecord snapshot) {
        history.addLast(snapshot);
        while (history.size() > HISTORY_LIMIT) {
            history.removeFirst();
        }
    }

    public synchronized Optional<LagTickRecord> latest() {
        return Optional.ofNullable(history.peekLast());
    }

    public synchronized Optional<LagTickRecord> latestSlow() {
        return history.reversed().stream().filter(LagTickRecord::slow).findFirst();
    }

    public synchronized Optional<LagTickRecord> find(long tickId) {
        return history.stream().filter(snapshot -> snapshot.tickId() == tickId).findFirst();
    }

    public synchronized int size() {
        return history.size();
    }

    public synchronized List<LagTickRecord> recentSlow(int limit) {
        return history.reversed().stream().filter(LagTickRecord::slow).limit(limit).toList();
    }

    public synchronized List<LagTickRecord> topSlow(int limit) {
        return history.stream()
                .filter(LagTickRecord::slow)
                .sorted(Comparator.comparingLong(LagTickRecord::wallNanos).reversed())
                .limit(limit)
                .toList();
    }

    public synchronized LagRankingReport rank(int windowTicks, int limit) {
        int boundedWindow = Math.max(1, Math.min(HISTORY_LIMIT, windowTicks));
        int boundedLimit = Math.max(1, Math.min(20, limit));
        List<LagTickRecord> ticks = history.reversed().stream().limit(boundedWindow).toList();
        Map<SourceIdentity, SourceAggregate> sources = new HashMap<>();
        Map<StackIdentity, StackAggregate> stacks = new HashMap<>();
        EnumMap<LagSeverity, Integer> severityCounts = new EnumMap<>(LagSeverity.class);

        for (LagTickRecord tick : ticks) {
            severityCounts.merge(LagSeverity.fromNanos(tick.wallNanos()), 1, Integer::sum);

            Map<SourceIdentity, TickSourceAggregate> tickSources = new HashMap<>();
            for (LagSourceRecord source : tick.sources()) {
                if (source.type().equals("WORLD") || source.type().equals("OTHER")) {
                    continue;
                }
                SourceIdentity identity = new SourceIdentity(
                        source.category(), source.type(), source.owner(), source.detail(), source.world()
                );
                tickSources.computeIfAbsent(identity, ignored -> new TickSourceAggregate()).add(source);
            }
            tickSources.forEach((identity, aggregate) ->
                    sources.computeIfAbsent(identity, ignored -> new SourceAggregate()).add(aggregate)
            );

            Set<StackIdentity> seenStacks = new HashSet<>();
            for (LagStackRecord stack : tick.stackSamples()) {
                StackIdentity identity = new StackIdentity(stack.category(), stack.source(), stack.frames());
                stacks.computeIfAbsent(identity, ignored -> new StackAggregate()).samples += stack.samples();
                if (seenStacks.add(identity)) {
                    stacks.get(identity).affectedTicks++;
                }
            }
        }

        List<LagSourceRanking> sourceRankings = sources.entrySet().stream()
                .map(entry -> entry.getValue().snapshot(entry.getKey()))
                .sorted(Comparator.comparingLong(LagSourceRanking::totalNanos).reversed()
                        .thenComparing(Comparator.comparingLong(LagSourceRanking::worstTickNanos).reversed()))
                .limit(boundedLimit)
                .toList();
        List<LagStackRanking> stackRankings = stacks.entrySet().stream()
                .map(entry -> new LagStackRanking(
                        entry.getKey().category,
                        entry.getKey().source,
                        entry.getValue().samples,
                        entry.getValue().affectedTicks,
                        entry.getKey().frames
                ))
                .sorted(Comparator.comparingInt(LagStackRanking::samples).reversed()
                        .thenComparing(Comparator.comparingInt(LagStackRanking::affectedTicks).reversed()))
                .limit(Math.min(5, boundedLimit))
                .toList();
        return new LagRankingReport(ticks.size(), sourceRankings, stackRankings, severityCounts);
    }

    public synchronized void clear() {
        history.clear();
    }

    private record SourceIdentity(
            LagCauseCategory category,
            String type,
            String owner,
            String detail,
            String world
    ) {
    }

    private record StackIdentity(LagCauseCategory category, String source, List<String> frames) {
        private StackIdentity {
            frames = List.copyOf(frames);
        }
    }

    private static final class TickSourceAggregate {
        private int occurrences;
        private long totalNanos;
        private long maxOccurrenceNanos;
        private boolean hasWorstLocation;
        private int x;
        private int y;
        private int z;

        private void add(LagSourceRecord source) {
            occurrences += source.count();
            totalNanos += source.totalNanos();
            if (source.maxNanos() > maxOccurrenceNanos) {
                maxOccurrenceNanos = source.maxNanos();
                hasWorstLocation = source.hasLocation();
                x = source.x();
                y = source.y();
                z = source.z();
            }
        }
    }

    private static final class SourceAggregate {
        private int occurrences;
        private int affectedTicks;
        private long totalNanos;
        private long worstTickNanos;
        private long maxOccurrenceNanos;
        private boolean hasWorstLocation;
        private int x;
        private int y;
        private int z;

        private void add(TickSourceAggregate tick) {
            occurrences += tick.occurrences;
            affectedTicks++;
            totalNanos += tick.totalNanos;
            worstTickNanos = Math.max(worstTickNanos, tick.totalNanos);
            if (tick.maxOccurrenceNanos > maxOccurrenceNanos) {
                maxOccurrenceNanos = tick.maxOccurrenceNanos;
                hasWorstLocation = tick.hasWorstLocation;
                x = tick.x;
                y = tick.y;
                z = tick.z;
            }
        }

        private LagSourceRanking snapshot(SourceIdentity identity) {
            return new LagSourceRanking(
                    identity.category, identity.type, identity.owner, identity.detail, identity.world,
                    occurrences, affectedTicks, totalNanos, worstTickNanos, maxOccurrenceNanos,
                    hasWorstLocation, x, y, z
            );
        }
    }

    private static final class StackAggregate {
        private int samples;
        private int affectedTicks;
    }
}
