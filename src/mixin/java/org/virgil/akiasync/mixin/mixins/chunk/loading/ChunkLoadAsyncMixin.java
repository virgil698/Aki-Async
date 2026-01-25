package org.virgil.akiasync.mixin.mixins.chunk.loading;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.virgil.akiasync.mixin.util.BridgeConfigCache;
import org.virgil.akiasync.mixin.bridge.Bridge;
import org.virgil.akiasync.mixin.bridge.BridgeManager;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;

@Pseudo
@Mixin(targets = "net.minecraft.world.level.chunk.storage.ChunkStorage", remap = false)
public class ChunkLoadAsyncMixin {

    @Unique
    private static volatile Executor CHUNK_IO_EXECUTOR = null;

    @Unique
    private static final ConcurrentHashMap<Long, CompletableFuture<Optional<CompoundTag>>> READ_CACHE =
        new ConcurrentHashMap<>();

    @Unique
    private static final int MAX_CACHE_SIZE = 256;

    @Unique
    private static volatile boolean initialized = false;

    @Unique
    private static final AtomicLong asyncReads = new AtomicLong(0);

    @Unique
    private static final AtomicLong cacheHits = new AtomicLong(0);

    @Unique
    private static volatile long lastLogTime = 0L;

    @Unique
    private static Executor akiasync$getExecutor() {
        if (CHUNK_IO_EXECUTOR == null) {
            Bridge bridge = BridgeManager.getBridge();
            if (bridge != null) {
                CHUNK_IO_EXECUTOR = bridge.getGeneralExecutor();
            }
        }
        return CHUNK_IO_EXECUTOR;
    }

    @Unique
    private static CompletableFuture<Optional<CompoundTag>> akiasync$readChunkAsync(
            ChunkPos pos,
            java.util.function.Supplier<Optional<CompoundTag>> reader) {

        if (!BridgeConfigCache.isChunkOptimizationEnabled()) {
            return CompletableFuture.completedFuture(reader.get());
        }

        Executor executor = akiasync$getExecutor();
        if (executor == null) {
            return CompletableFuture.completedFuture(reader.get());
        }

        long key = pos.toLong();

        CompletableFuture<Optional<CompoundTag>> cached = READ_CACHE.get(key);
        if (cached != null && !cached.isCompletedExceptionally()) {
            cacheHits.incrementAndGet();
            akiasync$logStatistics();
            return cached;
        }

        if (READ_CACHE.size() > MAX_CACHE_SIZE) {
            akiasync$cleanupCache();
        }

        CompletableFuture<Optional<CompoundTag>> future = CompletableFuture.supplyAsync(() -> {
            try {
                return reader.get();
            } catch (Exception e) {
                BridgeConfigCache.debugLog("[AkiAsync-ChunkLoad] Error reading chunk " + pos + ": " + e.getMessage());
                return Optional.empty();
            }
        }, executor);

        READ_CACHE.put(key, future);

        future.whenComplete((result, error) -> {
            READ_CACHE.remove(key, future);
        });

        asyncReads.incrementAndGet();
        akiasync$logStatistics();

        return future;
    }

    @Unique
    private static void akiasync$cleanupCache() {

        READ_CACHE.entrySet().removeIf(entry -> {
            CompletableFuture<?> future = entry.getValue();
            return future.isDone() || future.isCompletedExceptionally() || future.isCancelled();
        });

        if (READ_CACHE.size() > MAX_CACHE_SIZE) {
            int toRemove = READ_CACHE.size() / 2;
            var iterator = READ_CACHE.entrySet().iterator();
            while (iterator.hasNext() && toRemove > 0) {
                iterator.next();
                iterator.remove();
                toRemove--;
            }
        }
    }

    @Unique
    private static void akiasync$invalidateCache(ChunkPos pos) {
        READ_CACHE.remove(pos.toLong());
    }

    @Unique
    private static void akiasync$clearCache() {
        READ_CACHE.clear();
    }

    @Unique
    private static void akiasync$logStatistics() {
        if (!initialized) {
            initialized = true;
            BridgeConfigCache.debugLog("[AkiAsync-ChunkLoad] Async chunk load optimization enabled");
            BridgeConfigCache.debugLog("[AkiAsync-ChunkLoad] Using unified thread pool");
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastLogTime > 60000) {
            lastLogTime = currentTime;
            long reads = asyncReads.get();
            long hits = cacheHits.get();
            double hitRate = reads > 0 ? (hits * 100.0 / (reads + hits)) : 0;

            BridgeConfigCache.debugLog(String.format(
                "[AkiAsync-ChunkLoad] Stats - Async reads: %d, Cache hits: %d (%.1f%%), Cache size: %d",
                reads, hits, hitRate, READ_CACHE.size()
            ));
        }
    }
}
