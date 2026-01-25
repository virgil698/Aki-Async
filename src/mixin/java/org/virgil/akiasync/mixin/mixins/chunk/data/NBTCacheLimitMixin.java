package org.virgil.akiasync.mixin.mixins.chunk.data;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.IOWorker;
import net.minecraft.world.level.chunk.storage.RegionFileStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.virgil.akiasync.mixin.util.BridgeConfigCache;

import java.util.Iterator;
import java.util.Map;
import java.util.SequencedMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

@Mixin(value = IOWorker.class, priority = 990)
public abstract class NBTCacheLimitMixin {

    @Shadow
    @Final
    private SequencedMap<ChunkPos, ?> pendingWrites;

    @Shadow
    @Final
    public RegionFileStorage storage;

    @Unique
    private static final int CACHE_HARD_LIMIT = 1024;

    @Unique
    private static final int CACHE_SOFT_LIMIT = 256;

    @Unique
    private static volatile boolean akiasync$initialized = false;

    @Unique
    private static final AtomicLong akiasync$hardLimitHits = new AtomicLong(0);

    @Unique
    private static final AtomicLong akiasync$softLimitWrites = new AtomicLong(0);

    @Unique
    private static volatile long akiasync$lastLogTime = 0L;

    @Inject(
        method = "store(Lnet/minecraft/world/level/ChunkPos;Ljava/util/function/Supplier;)Ljava/util/concurrent/CompletableFuture;",
        at = @At("HEAD"),
        require = 0
    )
    private void akiasync$checkCacheLimitOnStore(ChunkPos chunkPos, Supplier<?> dataSupplier, CallbackInfoReturnable<CompletableFuture<Void>> cir) {
        if (!BridgeConfigCache.isChunkOptimizationEnabled()) {
            return;
        }

        akiasync$initIfNeeded();
        akiasync$checkAndEnforceLimits();
    }

    @Inject(method = "tellStorePending", at = @At("HEAD"), require = 0)
    private void akiasync$checkCacheLimitOnPending(CallbackInfo ci) {
        if (!BridgeConfigCache.isChunkOptimizationEnabled()) {
            return;
        }

        akiasync$checkAndEnforceLimits();
    }

    @Unique
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void akiasync$checkAndEnforceLimits() {
        int size = this.pendingWrites.size();

        if (size >= CACHE_HARD_LIMIT) {
            akiasync$hardLimitHits.incrementAndGet();
            BridgeConfigCache.debugLog(String.format(
                "[AkiAsync-NBTCache] Cache exceeded hard limit (%d >= %d), forcing writes",
                size, CACHE_HARD_LIMIT
            ));

            int targetSize = (int)(CACHE_SOFT_LIMIT * 0.75);
            int writeCount = 0;

            while (this.pendingWrites.size() > targetSize && writeCount < 100) {
                akiasync$writeFirstPending();
                writeCount++;
            }

            akiasync$logStatistics();
        }

        else if (size >= CACHE_SOFT_LIMIT) {
            int writeFrequency = Math.max(1, (size - CACHE_SOFT_LIMIT) / 16);
            for (int i = 0; i < writeFrequency && !this.pendingWrites.isEmpty(); i++) {
                akiasync$writeFirstPending();
                akiasync$softLimitWrites.incrementAndGet();
            }
        }
    }

    @Unique
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void akiasync$writeFirstPending() {
        try {
            Iterator<Map.Entry<ChunkPos, ?>> iterator = ((SequencedMap) this.pendingWrites).entrySet().iterator();
            if (iterator.hasNext()) {
                Map.Entry<ChunkPos, ?> entry = iterator.next();
                ChunkPos pos = entry.getKey();
                Object pendingStore = entry.getValue();

                try {
                    java.lang.reflect.Field dataField = pendingStore.getClass().getDeclaredField("data");
                    dataField.setAccessible(true);
                    Object data = dataField.get(pendingStore);

                    java.lang.reflect.Field resultField = pendingStore.getClass().getDeclaredField("result");
                    resultField.setAccessible(true);
                    CompletableFuture<Void> result = (CompletableFuture<Void>) resultField.get(pendingStore);

                    this.storage.write(pos, (net.minecraft.nbt.CompoundTag) data);
                    result.complete(null);

                    iterator.remove();
                } catch (Exception e) {
                    BridgeConfigCache.debugLog("[AkiAsync-NBTCache] Reflection error: " + e.getMessage());

                }
            }
        } catch (Exception e) {
            BridgeConfigCache.debugLog("[AkiAsync-NBTCache] Error writing pending chunk: " + e.getMessage());
        }
    }

    @Unique
    private static void akiasync$initIfNeeded() {
        if (!akiasync$initialized) {
            akiasync$initialized = true;
            BridgeConfigCache.debugLog("[AkiAsync-NBTCache] C2ME-style NBT cache limit optimization enabled");
            BridgeConfigCache.debugLog("[AkiAsync-NBTCache] Hard limit: " + CACHE_HARD_LIMIT + ", Soft limit: " + CACHE_SOFT_LIMIT);
        }
    }

    @Unique
    private static void akiasync$logStatistics() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - akiasync$lastLogTime > 60000) {
            akiasync$lastLogTime = currentTime;
            BridgeConfigCache.debugLog(String.format(
                "[AkiAsync-NBTCache] Stats - Hard limit hits: %d, Soft limit writes: %d",
                akiasync$hardLimitHits.get(), akiasync$softLimitWrites.get()
            ));
        }
    }
}
