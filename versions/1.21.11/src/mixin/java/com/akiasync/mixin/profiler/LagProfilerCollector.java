package com.akiasync.mixin.profiler;

import com.akiasync.mixin.Bridge.LagCauseCategory;
import com.akiasync.mixin.Bridge.LagSourceSnapshot;
import com.akiasync.mixin.Bridge.LagSourceType;
import com.akiasync.mixin.Bridge.LagStackSnapshot;
import com.akiasync.mixin.Bridge.LagSystemSnapshot;
import com.akiasync.mixin.Bridge.LagTickSnapshot;
import com.akiasync.mixin.BridgeManager;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

public final class LagProfilerCollector {
    public static final long SLOW_TICK_NANOS = 50_000_000L;
    private static final long STACK_SAMPLING_START_NANOS = 25_000_000L;
    private static final long STACK_SAMPLE_INTERVAL_NANOS = 2_000_000L;
    private static final long IDLE_POLL_NANOS = 5_000_000L;
    private static final long MIN_RECORDED_NANOS = 25_000L;
    private static final int AUTOMATIC_DETAIL_TICKS = 200;
    private static final int MAX_SOURCE_KEYS = 1_024;
    private static final int MAX_PUBLISHED_SOURCES = 32;
    private static final int MAX_PUBLISHED_STACKS = 8;
    private static final int MAX_STACK_DEPTH = 24;
    private static final ThreadLocal<TickContext> CURRENT = new ThreadLocal<>();
    private static final AtomicReference<TickContext> ACTIVE = new AtomicReference<>();
    private static final AtomicLong SAMPLER_GENERATION = new AtomicLong();
    private static final ThreadMXBean THREAD_MX_BEAN = ManagementFactory.getThreadMXBean();
    private static final MemoryMXBean MEMORY_MX_BEAN = ManagementFactory.getMemoryMXBean();
    private static final List<GarbageCollectorMXBean> GC_BEANS = ManagementFactory.getGarbageCollectorMXBeans();
    private static volatile Thread samplerThread;

    private LagProfilerCollector() {
    }

    public static void startSampler() {
        long generation = SAMPLER_GENERATION.incrementAndGet();
        Thread thread = new Thread(() -> sampleLoop(generation), "Aki-Async-LagSampler");
        thread.setDaemon(true);
        thread.setPriority(Math.max(Thread.MIN_PRIORITY, Thread.NORM_PRIORITY - 1));
        samplerThread = thread;
        thread.start();
    }

    public static void stopSampler() {
        SAMPLER_GENERATION.incrementAndGet();
        ACTIVE.set(null);
        Thread thread = samplerThread;
        samplerThread = null;
        if (thread != null) {
            LockSupport.unpark(thread);
        }
    }

    public static void beginTick(long tickId) {
        if (!BridgeManager.INSTANCE.isInstalled()) {
            CURRENT.remove();
            return;
        }
        boolean detailed = BridgeManager.INSTANCE.consumeDetailedTick();
        TickContext context = new TickContext(
                tickId,
                System.currentTimeMillis(),
                System.nanoTime(),
                currentCpuTime(),
                currentHeapUsed(),
                currentGcCollections(),
                currentGcTimeMillis(),
                detailed,
                Thread.currentThread()
        );
        CURRENT.set(context);
        ACTIVE.set(context);
    }

    public static void endTick() {
        TickContext context = CURRENT.get();
        CURRENT.remove();
        if (context == null) {
            return;
        }
        context.closed = true;
        ACTIVE.compareAndSet(context, null);

        long wallNanos = System.nanoTime() - context.startedNanos;
        long endingCpu = currentCpuTime();
        long cpuNanos = context.startedCpuNanos < 0 || endingCpu < 0 ? -1 : endingCpu - context.startedCpuNanos;
        long heapUsed = currentHeapUsed();
        long heapMax = MEMORY_MX_BEAN.getHeapMemoryUsage().getMax();
        long gcCollections = nonNegativeDelta(context.startedGcCollections, currentGcCollections());
        long gcPauseNanos = nonNegativeDelta(context.startedGcTimeMillis, currentGcTimeMillis()) * 1_000_000L;
        boolean slow = wallNanos >= SLOW_TICK_NANOS;
        if (slow) {
            BridgeManager.INSTANCE.requestDetailedTicks(AUTOMATIC_DETAIL_TICKS);
        }

        List<LagSourceSnapshot> sources = context.sources.entrySet().stream()
                .map(entry -> entry.getValue().snapshot(entry.getKey()))
                .sorted(Comparator.comparingLong(LagSourceSnapshot::totalNanos).reversed())
                .limit(MAX_PUBLISHED_SOURCES)
                .toList();
        List<LagStackSnapshot> stackSamples = context.stackSamples.entrySet().stream()
                .map(entry -> new LagStackSnapshot(
                        entry.getKey().category,
                        entry.getKey().source,
                        entry.getValue().samples.get(),
                        entry.getValue().frames
                ))
                .sorted(Comparator.comparingInt(LagStackSnapshot::samples).reversed())
                .limit(MAX_PUBLISHED_STACKS)
                .toList();
        BridgeManager.INSTANCE.publish(new LagTickSnapshot(
                context.tickId,
                context.startedAtMillis,
                wallNanos,
                cpuNanos,
                slow,
                context.detailed,
                sources,
                stackSamples,
                new LagSystemSnapshot(
                        heapUsed,
                        heapMax,
                        heapUsed - context.startedHeapUsed,
                        gcCollections,
                        gcPauseNanos
                )
        ));
    }

    public static long start(boolean detailedOnly) {
        TickContext context = CURRENT.get();
        if (context == null || detailedOnly && !context.detailed) {
            return -1L;
        }
        return System.nanoTime();
    }

    public static void record(
            long startedNanos,
            LagSourceType type,
            String owner,
            String detail,
            String world,
            boolean hasLocation,
            int x,
            int y,
            int z
    ) {
        TickContext context = CURRENT.get();
        if (startedNanos < 0 || context == null) {
            return;
        }
        long elapsed = System.nanoTime() - startedNanos;
        if (elapsed < MIN_RECORDED_NANOS && type != LagSourceType.PLUGIN_TASK && type != LagSourceType.PLUGIN_EVENT) {
            return;
        }
        SourceKey key = new SourceKey(type, safe(owner), safe(detail), safe(world), hasLocation, x, y, z);
        Aggregate aggregate = context.sources.get(key);
        if (aggregate == null) {
            if (context.sources.size() >= MAX_SOURCE_KEYS) {
                key = SourceKey.OTHER;
            }
            aggregate = context.sources.computeIfAbsent(key, ignored -> new Aggregate());
        }
        aggregate.add(elapsed);
    }

    private static void sampleLoop(long generation) {
        while (SAMPLER_GENERATION.get() == generation) {
            TickContext context = ACTIVE.get();
            if (context == null || context.closed) {
                LockSupport.parkNanos(IDLE_POLL_NANOS);
                continue;
            }

            long elapsed = System.nanoTime() - context.startedNanos;
            if (elapsed < STACK_SAMPLING_START_NANOS) {
                LockSupport.parkNanos(Math.min(IDLE_POLL_NANOS, STACK_SAMPLING_START_NANOS - elapsed));
                continue;
            }

            StackSample sample = stackSample(context.tickThread.getStackTrace());
            if (sample != null && !context.closed) {
                StackKey key = new StackKey(sample.category, sample.source);
                context.stackSamples.computeIfAbsent(
                        key, ignored -> new StackAggregate(sample.frames)
                ).samples.incrementAndGet();
            }
            LockSupport.parkNanos(STACK_SAMPLE_INTERVAL_NANOS);
        }
    }

    private static StackSample stackSample(StackTraceElement[] stackTrace) {
        List<String> frames = new ArrayList<>(Math.min(MAX_STACK_DEPTH, stackTrace.length));
        List<String> classNames = new ArrayList<>(Math.min(MAX_STACK_DEPTH, stackTrace.length));
        for (StackTraceElement frame : stackTrace) {
            String className = frame.getClassName();
            if (className.equals(Thread.class.getName()) || className.startsWith(LagProfilerCollector.class.getName())) {
                continue;
            }
            classNames.add(className);
            frames.add(formatFrame(frame));
            if (frames.size() >= MAX_STACK_DEPTH) {
                break;
            }
        }
        if (frames.isEmpty()) {
            return null;
        }

        Classification classification = classify(classNames, frames);
        return new StackSample(classification.category, classification.source, List.copyOf(frames));
    }

    private static Classification classify(List<String> classNames, List<String> frames) {
        String external = firstExternal(classNames);
        if (contains(classNames, "org.bukkit.plugin.RegisteredListener")) {
            return new Classification(LagCauseCategory.PLUGIN, external.isEmpty() ? "Bukkit event listener" : external);
        }
        if (contains(classNames, "org.bukkit.craftbukkit.scheduler.CraftTask")) {
            return new Classification(LagCauseCategory.PLUGIN, external.isEmpty() ? "Bukkit scheduled task" : external);
        }
        if (containsAny(frames, ".saveAllChunks(", ".saveEverything(", ".saveLevelData(", ".saveIncrementally(", "ChunkMap.save(", "PlayerDataStorage.save(")) {
            return new Classification(LagCauseCategory.WORLD_SAVE, firstMatchingText(
                    frames, ".saveAllChunks(", ".saveEverything(", ".saveLevelData(", ".saveIncrementally(", "ChunkMap.save(", "PlayerDataStorage.save("
            ));
        }
        if (containsAny(classNames, ".server.packs.", "ReloadableServerResources", "WorldLoader", "TagLoader", "ServerFunctionLibrary")
                || containsAny(frames, ".reloadResources(", ".loadResources(")) {
            return new Classification(LagCauseCategory.DATA_PACK, firstMatchingText(
                    frames, ".server.packs.", "ReloadableServerResources", "WorldLoader", "TagLoader",
                    "ServerFunctionLibrary", ".reloadResources(", ".loadResources("
            ));
        }
        if (containsAny(classNames, ".configuration.", ".config.", "YamlConfiguration", "GlobalConfiguration", "WorldConfiguration")
                || containsAny(frames, ".reloadConfig(", ".loadConfig(")) {
            return new Classification(LagCauseCategory.CONFIGURATION, firstMatchingText(
                    frames, ".configuration.", ".config.", "YamlConfiguration", "GlobalConfiguration", "WorldConfiguration",
                    ".reloadConfig(", ".loadConfig("
            ));
        }
        if (containsAny(classNames, ".commands.", "CommandDispatcher")) {
            return new Classification(LagCauseCategory.COMMAND, firstMatchingFrame(classNames, frames, ".commands.", "CommandDispatcher"));
        }
        if (containsAny(frames, ".handleMovePlayer(", ".handlePlayerAction(", ".handleUseItem(", ".handleUseItemOn(", ".handleContainer", ".handleInteract(", "ServerPlayerGameMode")) {
            return new Classification(LagCauseCategory.PLAYER_ACTION, firstMatchingText(
                    frames, ".handleMovePlayer(", ".handlePlayerAction(", ".handleUseItem(", ".handleUseItemOn(",
                    ".handleContainer", ".handleInteract(", "ServerPlayerGameMode"
            ));
        }
        if (containsAny(classNames, ".network.protocol.", "PacketUtils", "FriendlyByteBuf", "PacketDecoder", "PacketEncoder")) {
            return new Classification(LagCauseCategory.NETWORK, firstMatchingFrame(
                    classNames, frames, ".network.protocol.", "PacketUtils", "FriendlyByteBuf", "PacketDecoder", "PacketEncoder"
            ));
        }
        if (containsAny(classNames, "io.netty.", ".network.Connection", ".server.network.ServerConnectionListener", "java.net.")) {
            return new Classification(LagCauseCategory.NETWORK, firstMatchingFrame(
                    classNames, frames, "io.netty.", ".network.Connection", ".server.network.ServerConnectionListener", "java.net."
            ));
        }
        if (containsAny(classNames, "java.io.", "java.nio.file.", "RegionFile", "FileChannel", "IOWorker")) {
            return new Classification(LagCauseCategory.DISK_IO, firstMatchingFrame(
                    classNames, frames, "java.io.", "java.nio.file.", "RegionFile", "FileChannel", "IOWorker"
            ));
        }
        if (containsAny(classNames, ".world.level.levelgen.", ".world.level.biome.", "ChunkGenerator")) {
            return new Classification(LagCauseCategory.WORLD_GENERATION, firstMatchingFrame(
                    classNames, frames, ".world.level.levelgen.", ".world.level.biome.", "ChunkGenerator"
            ));
        }
        if (containsAny(classNames, ".world.level.block.entity.", ".world.level.block.", "TickingBlockEntity", "NeighborUpdater", "LevelTicks", ".redstone.")) {
            return new Classification(LagCauseCategory.BLOCK, firstMatchingFrame(
                    classNames, frames, ".world.level.block.entity.", ".world.level.block.", "TickingBlockEntity", "NeighborUpdater", "LevelTicks", ".redstone."
            ));
        }
        if (containsAny(classNames, ".world.entity.ai.", ".world.entity.monster.", ".world.entity.animal.", ".world.entity.npc.")) {
            return new Classification(LagCauseCategory.MOB, firstMatchingFrame(
                    classNames, frames, ".world.entity.ai.", ".world.entity.monster.", ".world.entity.animal.", ".world.entity.npc."
            ));
        }
        if (containsAny(classNames, ".world.entity.", ".server.level.ServerEntity")) {
            return new Classification(LagCauseCategory.ENTITY, firstMatchingFrame(classNames, frames, ".world.entity.", ".server.level.ServerEntity"));
        }
        if (containsAny(classNames, ".world.level.chunk.", ".server.level.ChunkMap", "moonrise")) {
            return new Classification(LagCauseCategory.CHUNK, firstMatchingFrame(classNames, frames, ".world.level.chunk.", ".server.level.ChunkMap", "moonrise"));
        }
        if (!external.isEmpty()) {
            return new Classification(LagCauseCategory.PLUGIN, external);
        }
        return new Classification(LagCauseCategory.UNKNOWN, frames.getFirst());
    }

    private static String firstExternal(List<String> classNames) {
        for (String className : classNames) {
            if (!isServerOrJdkClass(className)) {
                return className;
            }
        }
        return "";
    }

    private static boolean isServerOrJdkClass(String className) {
        return className.startsWith("net.minecraft.")
                || className.startsWith("org.bukkit.")
                || className.startsWith("io.papermc.")
                || className.startsWith("org.leavesmc.")
                || className.startsWith("ca.spottedleaf.")
                || className.startsWith("com.akiasync.")
                || className.startsWith("com.mojang.")
                || className.startsWith("com.llamalad7.")
                || className.startsWith("org.spongepowered.")
                || className.startsWith("java.")
                || className.startsWith("jdk.")
                || className.startsWith("sun.");
    }

    private static boolean contains(List<String> values, String needle) {
        return values.stream().anyMatch(value -> value.contains(needle));
    }

    private static boolean containsAny(List<String> values, String... needles) {
        for (String value : values) {
            for (String needle : needles) {
                if (value.contains(needle)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String firstMatchingFrame(List<String> classNames, List<String> frames, String... needles) {
        for (int index = 0; index < classNames.size(); index++) {
            for (String needle : needles) {
                if (classNames.get(index).contains(needle)) {
                    return frames.get(index);
                }
            }
        }
        return frames.getFirst();
    }

    private static String firstMatchingText(List<String> values, String... needles) {
        for (String value : values) {
            for (String needle : needles) {
                if (value.contains(needle)) {
                    return value;
                }
            }
        }
        return values.getFirst();
    }

    private static String formatFrame(StackTraceElement frame) {
        return frame.getClassName() + "." + frame.getMethodName()
                + "(" + (frame.getFileName() == null ? "Unknown Source" : frame.getFileName())
                + (frame.getLineNumber() >= 0 ? ":" + frame.getLineNumber() : "") + ")";
    }

    private static long currentCpuTime() {
        return THREAD_MX_BEAN.isCurrentThreadCpuTimeSupported() ? THREAD_MX_BEAN.getCurrentThreadCpuTime() : -1L;
    }

    private static long currentHeapUsed() {
        return MEMORY_MX_BEAN.getHeapMemoryUsage().getUsed();
    }

    private static long currentGcCollections() {
        return GC_BEANS.stream().mapToLong(bean -> Math.max(0, bean.getCollectionCount())).sum();
    }

    private static long currentGcTimeMillis() {
        return GC_BEANS.stream().mapToLong(bean -> Math.max(0, bean.getCollectionTime())).sum();
    }

    private static long nonNegativeDelta(long before, long after) {
        return Math.max(0, after - before);
    }

    private static LagCauseCategory categoryFor(LagSourceType type) {
        return switch (type) {
            case MOB -> LagCauseCategory.MOB;
            case ENTITY -> LagCauseCategory.ENTITY;
            case BLOCK_ENTITY, BLOCK_ENTITIES -> LagCauseCategory.BLOCK;
            case CHUNK -> LagCauseCategory.CHUNK;
            case PLUGIN_TASK, PLUGIN_EVENT -> LagCauseCategory.PLUGIN;
            case WORLD, OTHER -> LagCauseCategory.UNKNOWN;
        };
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static final class TickContext {
        private final long tickId;
        private final long startedAtMillis;
        private final long startedNanos;
        private final long startedCpuNanos;
        private final long startedHeapUsed;
        private final long startedGcCollections;
        private final long startedGcTimeMillis;
        private final boolean detailed;
        private final Thread tickThread;
        private final Map<SourceKey, Aggregate> sources = new HashMap<>();
        private final Map<StackKey, StackAggregate> stackSamples = new ConcurrentHashMap<>();
        private volatile boolean closed;

        private TickContext(
                long tickId,
                long startedAtMillis,
                long startedNanos,
                long startedCpuNanos,
                long startedHeapUsed,
                long startedGcCollections,
                long startedGcTimeMillis,
                boolean detailed,
                Thread tickThread
        ) {
            this.tickId = tickId;
            this.startedAtMillis = startedAtMillis;
            this.startedNanos = startedNanos;
            this.startedCpuNanos = startedCpuNanos;
            this.startedHeapUsed = startedHeapUsed;
            this.startedGcCollections = startedGcCollections;
            this.startedGcTimeMillis = startedGcTimeMillis;
            this.detailed = detailed;
            this.tickThread = tickThread;
        }
    }

    private record SourceKey(
            LagSourceType type,
            String owner,
            String detail,
            String world,
            boolean hasLocation,
            int x,
            int y,
            int z
    ) {
        private static final SourceKey OTHER = new SourceKey(
                LagSourceType.OTHER, "", "overflow", "", false, 0, 0, 0
        );
    }

    private record StackKey(LagCauseCategory category, String source) {
    }

    private record StackSample(LagCauseCategory category, String source, List<String> frames) {
    }

    private record Classification(LagCauseCategory category, String source) {
    }

    private static final class StackAggregate {
        private final List<String> frames;
        private final AtomicInteger samples = new AtomicInteger();

        private StackAggregate(List<String> frames) {
            this.frames = frames;
        }
    }

    private static final class Aggregate {
        private int count;
        private long totalNanos;
        private long maxNanos;

        private void add(long nanos) {
            count++;
            totalNanos += nanos;
            maxNanos = Math.max(maxNanos, nanos);
        }

        private LagSourceSnapshot snapshot(SourceKey key) {
            return new LagSourceSnapshot(
                    categoryFor(key.type), key.type, key.owner, key.detail, key.world, key.hasLocation,
                    key.x, key.y, key.z, count, totalNanos, maxNanos
            );
        }
    }
}
