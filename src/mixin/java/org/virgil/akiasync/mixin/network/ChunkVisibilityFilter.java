package org.virgil.akiasync.mixin.network;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ChunkVisibilityFilter {

    private static final Map<String, Set<ChunkPos>> VISIBLE_CHUNKS_CACHE = new ConcurrentHashMap<>();

    private static final Map<String, Vec3> LAST_PLAYER_POS = new ConcurrentHashMap<>();

    private static final Map<String, Vec3> LAST_PLAYER_VIEW = new ConcurrentHashMap<>();

    private static final int CACHE_UPDATE_INTERVAL = 10;

    private static final Map<String, Long> LAST_UPDATE_TICK = new ConcurrentHashMap<>();

    private static long currentTick = 0;

    private static long totalChecks = 0;
    private static long visibleChunks = 0;
    private static long occludedChunks = 0;

    public static void tick() {
        currentTick++;

        if (currentTick % 200 == 0) {
            cleanup();
        }
    }

    public static boolean isChunkVisible(ServerPlayer player, ChunkPos chunkPos, ServerLevel level) {
        totalChecks++;

        String playerId = player.getUUID().toString();

        Long lastUpdate = LAST_UPDATE_TICK.get(playerId);
        boolean needUpdate = lastUpdate == null || currentTick - lastUpdate >= CACHE_UPDATE_INTERVAL;

        Vec3 currentPos = player.position();
        Vec3 lastPos = LAST_PLAYER_POS.get(playerId);
        Vec3 currentView = new Vec3(player.getYRot(), player.getXRot(), 0);
        Vec3 lastView = LAST_PLAYER_VIEW.get(playerId);

        if (lastPos != null && lastView != null) {
            double moveDist = currentPos.distanceToSqr(lastPos);
            double viewChange = Math.abs(currentView.x - lastView.x) + Math.abs(currentView.y - lastView.y);

            if (moveDist > 1.0 || viewChange > 10.0) {
                needUpdate = true;
            }
        } else {
            needUpdate = true;
        }

        if (needUpdate) {
            updateVisibleChunks(player, level);
            LAST_UPDATE_TICK.put(playerId, currentTick);
            LAST_PLAYER_POS.put(playerId, currentPos);
            LAST_PLAYER_VIEW.put(playerId, currentView);
        }

        Set<ChunkPos> visibleChunks = VISIBLE_CHUNKS_CACHE.get(playerId);
        if (visibleChunks == null) {

            ChunkVisibilityFilter.visibleChunks++;
            return true;
        }

        boolean visible = visibleChunks.contains(chunkPos);
        if (visible) {
            ChunkVisibilityFilter.visibleChunks++;
        } else {
            occludedChunks++;
        }

        return visible;
    }

    private static void updateVisibleChunks(ServerPlayer player, ServerLevel level) {
        String playerId = player.getUUID().toString();
        Set<ChunkPos> visibleChunks = ConcurrentHashMap.newKeySet();

        Vec3 playerPos = player.position();
        Vec3 viewDir = player.getViewVector(1.0f);

        int viewDistance = player.getServer().getPlayerList().getViewDistance();
        int playerChunkX = (int) playerPos.x >> 4;
        int playerChunkZ = (int) playerPos.z >> 4;

        for (int dx = -viewDistance; dx <= viewDistance; dx++) {
            for (int dz = -viewDistance; dz <= viewDistance; dz++) {
                int chunkX = playerChunkX + dx;
                int chunkZ = playerChunkZ + dz;
                ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);

                if (isChunkInFrustum(player, chunkPos, viewDir)) {

                    if (!isChunkOccluded(player, chunkPos, level)) {
                        visibleChunks.add(chunkPos);
                    }
                }
            }
        }

        VISIBLE_CHUNKS_CACHE.put(playerId, visibleChunks);
    }

    private static boolean isChunkInFrustum(ServerPlayer player, ChunkPos chunkPos, Vec3 viewDir) {
        Vec3 playerPos = player.position();

        double chunkCenterX = (chunkPos.x << 4) + 8.0;
        double chunkCenterZ = (chunkPos.z << 4) + 8.0;
        double chunkCenterY = playerPos.y;

        Vec3 toChunk = new Vec3(
            chunkCenterX - playerPos.x,
            chunkCenterY - playerPos.y,
            chunkCenterZ - playerPos.z
        ).normalize();

        double dot = viewDir.dot(toChunk);

        return dot > -0.3;
    }

    private static boolean isChunkOccluded(ServerPlayer player, ChunkPos chunkPos, ServerLevel level) {
        Vec3 playerPos = player.getEyePosition();

        double chunkCenterX = (chunkPos.x << 4) + 8.0;
        double chunkCenterZ = (chunkPos.z << 4) + 8.0;
        double chunkCenterY = playerPos.y;

        Vec3 chunkCenter = new Vec3(chunkCenterX, chunkCenterY, chunkCenterZ);

        Vec3 direction = chunkCenter.subtract(playerPos);
        double distance = direction.length();
        Vec3 normalizedDir = direction.normalize();

        if (distance < 32.0) {
            return false;
        }

        double step = 4.0;
        int steps = (int) (distance / step);

        if (steps > 16) {
            steps = 16;
        }

        for (int i = 1; i < steps; i++) {
            double t = i * step;
            Vec3 checkPos = playerPos.add(normalizedDir.scale(t));
            BlockPos blockPos = BlockPos.containing(checkPos);

            try {

                var blockState = level.getBlockState(blockPos);

                if (blockState.isSolidRender()) {
                    return true;
                }
            } catch (Exception e) {

                return false;
            }
        }

        return false;
    }

    public static void forceUpdate(ServerPlayer player, ServerLevel level) {
        String playerId = player.getUUID().toString();
        LAST_UPDATE_TICK.remove(playerId);
        updateVisibleChunks(player, level);
    }

    private static void cleanup() {

        long expireTime = currentTick - 1000;

        LAST_UPDATE_TICK.entrySet().removeIf(entry -> entry != null && entry.getValue() != null && entry.getValue() < expireTime);

        Set<String> activePlayerIds = LAST_UPDATE_TICK.keySet();
        VISIBLE_CHUNKS_CACHE.keySet().retainAll(activePlayerIds);
        LAST_PLAYER_POS.keySet().retainAll(activePlayerIds);
        LAST_PLAYER_VIEW.keySet().retainAll(activePlayerIds);
    }

    public static void clearPlayer(java.util.UUID playerId) {
        String id = playerId.toString();
        VISIBLE_CHUNKS_CACHE.remove(id);
        LAST_PLAYER_POS.remove(id);
        LAST_PLAYER_VIEW.remove(id);
        LAST_UPDATE_TICK.remove(id);
    }

    public static void clearAll() {
        VISIBLE_CHUNKS_CACHE.clear();
        LAST_PLAYER_POS.clear();
        LAST_PLAYER_VIEW.clear();
        LAST_UPDATE_TICK.clear();
    }

    public static String getStats() {
        double occlusionRate = totalChecks > 0 ?
            (double) occludedChunks / totalChecks * 100.0 : 0.0;

        return String.format(
            "ChunkVisibilityFilter: checks=%d, visible=%d, occluded=%d (%.1f%%), cached_players=%d",
            totalChecks, visibleChunks, occludedChunks, occlusionRate,
            VISIBLE_CHUNKS_CACHE.size()
        );
    }

    public static void resetStats() {
        totalChecks = 0;
        visibleChunks = 0;
        occludedChunks = 0;
    }

    public static long getCurrentTick() {
        return currentTick;
    }

    public static int getVisibleChunkCount(ServerPlayer player) {
        String playerId = player.getUUID().toString();
        Set<ChunkPos> visibleChunks = VISIBLE_CHUNKS_CACHE.get(playerId);
        return visibleChunks != null ? visibleChunks.size() : 0;
    }
}
