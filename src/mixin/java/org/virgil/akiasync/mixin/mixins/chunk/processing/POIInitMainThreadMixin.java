package org.virgil.akiasync.mixin.mixins.chunk.processing;

import net.minecraft.core.SectionPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.virgil.akiasync.mixin.util.BridgeConfigCache;

import java.util.concurrent.atomic.AtomicLong;

@Mixin(value = net.minecraft.world.level.chunk.storage.SerializableChunkData.class, priority = 1100)
public class POIInitMainThreadMixin {

    @Unique
    private static final AtomicLong akiasync$mainThreadCalls = new AtomicLong(0);

    @Unique
    private static final AtomicLong akiasync$asyncCalls = new AtomicLong(0);

    @Unique
    private static volatile long akiasync$lastLogTime = 0L;

    @Unique
    private static volatile boolean akiasync$initialized = false;

    @Redirect(
        method = "parse",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/ai/village/poi/PoiManager;checkConsistencyWithBlocks(Lnet/minecraft/core/SectionPos;Lnet/minecraft/world/level/chunk/LevelChunkSection;)V"
        ),
        require = 0
    )
    private static void akiasync$redirectPoiInit(PoiManager poiManager, SectionPos sectionPos, LevelChunkSection section) {
        if (!BridgeConfigCache.isChunkOptimizationEnabled()) {
            poiManager.checkConsistencyWithBlocks(sectionPos, section);
            return;
        }

        akiasync$initIfNeeded();

        MinecraftServer server = akiasync$getServer();
        boolean isMainThread = server != null && server.isSameThread();

        if (isMainThread) {
            akiasync$mainThreadCalls.incrementAndGet();
        } else {
            akiasync$asyncCalls.incrementAndGet();
        }

        try {
            poiManager.checkConsistencyWithBlocks(sectionPos, section);
        } catch (Exception e) {
            BridgeConfigCache.debugLog("[AkiAsync-POIInit] Error in POI init: " + e.getMessage());
        }

        akiasync$logStatistics();
    }

    @Unique
    private static MinecraftServer akiasync$getServer() {
        try {
            return net.minecraft.server.MinecraftServer.getServer();
        } catch (Exception e) {
            return null;
        }
    }

    @Unique
    private static void akiasync$initIfNeeded() {
        if (!akiasync$initialized) {
            akiasync$initialized = true;
            BridgeConfigCache.debugLog("[AkiAsync-POIInit] POI init tracking enabled (Paper/Leaves handles thread safety)");
        }
    }

    @Unique
    private static void akiasync$logStatistics() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - akiasync$lastLogTime > 60000) {
            akiasync$lastLogTime = currentTime;
            long mainThread = akiasync$mainThreadCalls.get();
            long async = akiasync$asyncCalls.get();
            if (mainThread > 0 || async > 0) {
                BridgeConfigCache.debugLog(String.format(
                    "[AkiAsync-POIInit] Stats - Main thread: %d, Async thread: %d",
                    mainThread, async
                ));
            }
        }
    }
}
