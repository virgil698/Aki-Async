package com.akiasync.mixin;

import java.util.List;

public interface Bridge {
    void publish(LagTickSnapshot snapshot);

    enum LagCauseCategory {
        MOB,
        ENTITY,
        BLOCK,
        CHUNK,
        PLUGIN,
        WORLD_SAVE,
        NETWORK,
        GC,
        CPU,
        MEMORY,
        DISK_IO,
        WORLD_GENERATION,
        PLAYER_ACTION,
        COMMAND,
        DATA_PACK,
        CONFIGURATION,
        UNKNOWN
    }

    enum LagSourceType {
        WORLD, CHUNK, MOB, ENTITY, BLOCK_ENTITY, BLOCK_ENTITIES, PLUGIN_TASK, PLUGIN_EVENT, OTHER
    }

    record LagSourceSnapshot(
            LagCauseCategory category, LagSourceType type, String owner, String detail, String world,
            boolean hasLocation, int x, int y, int z,
            int count, long totalNanos, long maxNanos
    ) {
    }

    record LagStackSnapshot(
            LagCauseCategory category, String source, int samples, List<String> frames
    ) {
        public LagStackSnapshot {
            frames = List.copyOf(frames);
        }
    }

    record LagSystemSnapshot(
            long heapUsedBytes,
            long heapMaxBytes,
            long heapDeltaBytes,
            long gcCollections,
            long gcPauseNanos
    ) {
    }

    record LagTickSnapshot(
            long tickId, long startedAtMillis, long wallNanos, long cpuNanos,
            boolean slow, boolean detailed, List<LagSourceSnapshot> sources,
            List<LagStackSnapshot> stackSamples, LagSystemSnapshot system
    ) {
        public LagTickSnapshot {
            sources = List.copyOf(sources);
            stackSamples = List.copyOf(stackSamples);
        }
    }
}
