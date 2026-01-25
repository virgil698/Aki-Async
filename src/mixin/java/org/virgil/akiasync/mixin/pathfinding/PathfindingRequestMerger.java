package org.virgil.akiasync.mixin.pathfinding;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.pathfinder.Path;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class PathfindingRequestMerger {

    public static class PathRequest {
        public final Mob entity;
        public final BlockPos start;
        public final BlockPos target;
        public final CompletableFuture<Path> future;
        public final long timestamp;

        public PathRequest(Mob entity, BlockPos start, BlockPos target) {
            this.entity = entity;
            this.start = start;
            this.target = target;
            this.future = new CompletableFuture<>();
            this.timestamp = System.currentTimeMillis();
        }
    }

    public static long encodePosition(BlockPos pos) {
        return encodePosition(pos.getX(), pos.getY(), pos.getZ());
    }

    public static long encodePosition(int x, int y, int z) {

        long encodedX = (long) x & 0x1FFFFF;
        long encodedY = (long) y & 0x1FFFFF;
        long encodedZ = (long) z & 0x1FFFFF;

        return (encodedX << 42) | (encodedY << 21) | encodedZ;
    }

    public static int decodeX(long encoded) {
        int x = (int) ((encoded >> 42) & 0x1FFFFF);

        if ((x & 0x100000) != 0) {
            x |= 0xFFE00000;
        }
        return x;
    }

    public static int decodeY(long encoded) {
        int y = (int) ((encoded >> 21) & 0x1FFFFF);
        if ((y & 0x100000) != 0) {
            y |= 0xFFE00000;
        }
        return y;
    }

    public static int decodeZ(long encoded) {
        int z = (int) (encoded & 0x1FFFFF);
        if ((z & 0x100000) != 0) {
            z |= 0xFFE00000;
        }
        return z;
    }

    public static long createPathKey(BlockPos start, BlockPos target) {
        long startEncoded = encodePosition(start);
        long targetEncoded = encodePosition(target);

        return startEncoded ^ (targetEncoded * 31);
    }

    private final Long2ObjectOpenHashMap<List<PathRequest>> pendingRequests;

    private static final int MAX_BATCH_SIZE = 50;

    private static final long MAX_WAIT_TIME = 50;

    public PathfindingRequestMerger() {
        this.pendingRequests = new Long2ObjectOpenHashMap<>();
    }

    public CompletableFuture<Path> addRequest(Mob entity, BlockPos start, BlockPos target) {
        long key = createPathKey(start, target);

        PathRequest request = new PathRequest(entity, start, target);

        synchronized (pendingRequests) {
            List<PathRequest> requests = pendingRequests.get(key);
            if (requests == null) {
                requests = new ArrayList<>();
                pendingRequests.put(key, requests);
            }
            requests.add(request);

            if (requests.size() >= MAX_BATCH_SIZE || shouldFlush(requests)) {
                flushRequests(key, requests);
            }
        }

        return request.future;
    }

    private boolean shouldFlush(List<PathRequest> requests) {
        if (requests.isEmpty()) {
            return false;
        }

        long oldestTimestamp = requests.get(0).timestamp;
        return System.currentTimeMillis() - oldestTimestamp > MAX_WAIT_TIME;
    }

    private void flushRequests(long key, List<PathRequest> requests) {
        if (requests.isEmpty()) {
            return;
        }

        pendingRequests.remove(key);

        PathRequest representative = requests.get(0);

        CompletableFuture.supplyAsync(() -> {
            try {

                return computePath(representative.entity, representative.start, representative.target);
            } catch (Exception e) {
                org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                    "PathfindingRequestMerger", "computePath", e);
                return null;
            }
        }).thenAccept(path -> {

            for (PathRequest request : requests) {
                request.future.complete(path);
            }
        });
    }

    private Path computePath(Mob entity, BlockPos start, BlockPos target) {

        return null;
    }

    public void flushAll() {
        synchronized (pendingRequests) {
            for (Long2ObjectOpenHashMap.Entry<List<PathRequest>> entry : pendingRequests.long2ObjectEntrySet()) {
                flushRequests(entry.getLongKey(), entry.getValue());
            }
            pendingRequests.clear();
        }
    }

    public String getStats() {
        synchronized (pendingRequests) {
            int totalRequests = 0;
            for (List<PathRequest> requests : pendingRequests.values()) {
                totalRequests += requests.size();
            }
            return String.format("Pending groups: %d, Total requests: %d",
                pendingRequests.size(), totalRequests);
        }
    }
}
