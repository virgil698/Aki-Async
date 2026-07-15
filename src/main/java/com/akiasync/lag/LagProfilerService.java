package com.akiasync.lag;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

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

    public synchronized void clear() {
        history.clear();
    }
}
