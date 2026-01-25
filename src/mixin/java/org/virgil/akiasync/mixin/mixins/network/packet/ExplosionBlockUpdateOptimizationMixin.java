package org.virgil.akiasync.mixin.mixins.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@SuppressWarnings("unused")
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ExplosionBlockUpdateOptimizationMixin {

    @Shadow
    public ServerPlayer player;

    @Unique
    private static volatile boolean enabled = true;
    @Unique
    private static volatile boolean initialized = false;
    @Unique
    private static volatile int blockChangeThreshold = 512;

    @Unique
    private final Map<Long, List<BlockUpdateEntry>> pendingBlockUpdates = new ConcurrentHashMap<>();
    @Unique
    private final Map<Long, AtomicInteger> chunkBlockCounts = new ConcurrentHashMap<>();

    @Unique
    private static final AtomicLong totalBlockUpdates = new AtomicLong(0);
    @Unique
    private static final AtomicLong optimizedChunks = new AtomicLong(0);
    @Unique
    private static final AtomicLong savedBlockUpdates = new AtomicLong(0);

    @Unique
    private record BlockUpdateEntry(Object packet, Object listener) {}

    @Unique
    public boolean akiasync$handleBlockUpdate(Object packet, Object listener) {
        if (!initialized) {
            akiasync$init();
        }

        if (!enabled || player == null) {
            return false;
        }

        try {
            if (packet instanceof ClientboundBlockUpdatePacket blockPacket) {
                return akiasync$queueBlockUpdate(blockPacket, listener);
            } else if (packet instanceof ClientboundSectionBlocksUpdatePacket sectionPacket) {
                return akiasync$queueSectionUpdate(sectionPacket, listener);
            }
        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "ExplosionBlockUpdateOptimization", "handleBlockUpdate", e);
        }

        return false;
    }

    @Unique
    private boolean akiasync$queueBlockUpdate(ClientboundBlockUpdatePacket packet, Object listener) {
        BlockPos pos = packet.getPos();
        long chunkKey = ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);

        pendingBlockUpdates.computeIfAbsent(chunkKey, k -> new ArrayList<>())
            .add(new BlockUpdateEntry(packet, listener));
        chunkBlockCounts.computeIfAbsent(chunkKey, k -> new AtomicInteger(0)).incrementAndGet();
        totalBlockUpdates.incrementAndGet();

        return true;
    }

    @Unique
    private boolean akiasync$queueSectionUpdate(ClientboundSectionBlocksUpdatePacket packet, Object listener) {
        try {
            java.lang.reflect.Field sectionPosField = packet.getClass().getDeclaredField("sectionPos");
            sectionPosField.setAccessible(true);
            Object sectionPos = sectionPosField.get(packet);

            java.lang.reflect.Method chunkMethod = sectionPos.getClass().getMethod("chunk");
            Object chunkPos = chunkMethod.invoke(sectionPos);

            java.lang.reflect.Method toLongMethod = chunkPos.getClass().getMethod("toLong");
            long chunkKey = (Long) toLongMethod.invoke(chunkPos);

            java.lang.reflect.Field positionsField = packet.getClass().getDeclaredField("positions");
            positionsField.setAccessible(true);
            short[] positions = (short[]) positionsField.get(packet);
            int count = positions != null ? positions.length : 1;

            pendingBlockUpdates.computeIfAbsent(chunkKey, k -> new ArrayList<>())
                .add(new BlockUpdateEntry(packet, listener));
            chunkBlockCounts.computeIfAbsent(chunkKey, k -> new AtomicInteger(0)).addAndGet(count);
            totalBlockUpdates.addAndGet(count);

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Unique
    public void akiasync$processPendingBlockUpdates(java.util.function.BiConsumer<Object, Object> sender) {
        if (pendingBlockUpdates.isEmpty()) {
            return;
        }

        Map<Long, List<BlockUpdateEntry>> processing = new HashMap<>(pendingBlockUpdates);
        pendingBlockUpdates.clear();

        for (Map.Entry<Long, List<BlockUpdateEntry>> entry : processing.entrySet()) {
            long chunkKey = entry.getKey();
            List<BlockUpdateEntry> updates = entry.getValue();
            AtomicInteger countAtomic = chunkBlockCounts.remove(chunkKey);
            int totalChanges = countAtomic != null ? countAtomic.get() : updates.size();

            if (totalChanges >= blockChangeThreshold) {
                if (akiasync$sendChunkUpdate(chunkKey, sender)) {
                    optimizedChunks.incrementAndGet();
                    savedBlockUpdates.addAndGet(totalChanges - 1);
                    continue;
                }
            }

            for (BlockUpdateEntry update : updates) {
                sender.accept(update.packet(), update.listener());
            }
        }

        chunkBlockCounts.clear();
    }

    @Unique
    private boolean akiasync$sendChunkUpdate(long chunkKey, java.util.function.BiConsumer<Object, Object> sender) {
        try {
            int x = ChunkPos.getX(chunkKey);
            int z = ChunkPos.getZ(chunkKey);

            if (player == null || player.level() == null) {
                return false;
            }

            LevelChunk chunk = player.level().getChunkIfLoaded(x, z);
            if (chunk == null) {
                return false;
            }

            ClientboundLevelChunkWithLightPacket chunkPacket = new ClientboundLevelChunkWithLightPacket(
                chunk, player.level().getLightEngine(), null, null
            );
            sender.accept(chunkPacket, null);
            return true;

        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "ExplosionBlockUpdateOptimization", "sendChunkUpdate", e);
            return false;
        }
    }

    @Unique
    public boolean akiasync$hasPendingUpdates() {
        return !pendingBlockUpdates.isEmpty();
    }

    @Unique
    public int akiasync$getPendingUpdateCount() {
        return pendingBlockUpdates.values().stream().mapToInt(List::size).sum();
    }

    @Unique
    private static synchronized void akiasync$init() {
        if (initialized) {
            return;
        }

        try {
            org.virgil.akiasync.mixin.bridge.Bridge bridge =
                org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();

            if (bridge != null) {
                enabled = bridge.isExplosionBlockUpdateOptimizationEnabled();
                blockChangeThreshold = bridge.getExplosionBlockChangeThreshold();

                bridge.debugLog("[ExplosionBlockUpdateOptimization] Initialized: enabled=%s, threshold=%d",
                    enabled, blockChangeThreshold);

                initialized = true;
            }
        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "ExplosionBlockUpdateOptimization", "init", e);
        }
    }

    @Unique
    private static boolean akiasync$isEnabled() {
        if (!initialized) {
            akiasync$init();
        }
        return enabled;
    }

    @Unique
    private static String akiasync$getStatistics() {
        long total = totalBlockUpdates.get();
        long optimized = optimizedChunks.get();
        long saved = savedBlockUpdates.get();

        double saveRate = total > 0 ? (double) saved / total * 100 : 0;

        return String.format(
            "ExplosionBlockUpdateOptimization: enabled=%s, threshold=%d, totalUpdates=%d, optimizedChunks=%d, savedUpdates=%d (%.1f%%)",
            enabled, blockChangeThreshold, total, optimized, saved, saveRate
        );
    }

    @Unique
    private static void akiasync$resetStatistics() {
        totalBlockUpdates.set(0);
        optimizedChunks.set(0);
        savedBlockUpdates.set(0);
    }

    @Unique
    private static void akiasync$reload() {
        initialized = false;
    }
}
