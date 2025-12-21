package org.virgil.akiasync.mixin.pathfinding;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.pathfinder.Path;
import org.virgil.akiasync.mixin.util.BridgeConfigCache;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class EnhancedPathfindingSystem {
    
    private static volatile boolean enabled = true;
    private static volatile int maxConcurrentRequests = 50;
    private static volatile int maxRequestsPerTick = 20;
    private static volatile int highPriorityDistance = 16;
    private static volatile int mediumPriorityDistance = 48;
    
    private static final AtomicInteger activeRequests = new AtomicInteger(0);
    private static final AtomicLong totalRequests = new AtomicLong(0);
    private static final AtomicLong cacheHits = new AtomicLong(0);
    private static final AtomicLong queueRejects = new AtomicLong(0);
    
    private static final PriorityBlockingQueue<PathRequest> highPriorityQueue = 
        new PriorityBlockingQueue<>(100, Comparator.comparingDouble(PathRequest::getPriority));
    private static final PriorityBlockingQueue<PathRequest> mediumPriorityQueue = 
        new PriorityBlockingQueue<>(200, Comparator.comparingDouble(PathRequest::getPriority));
    private static final PriorityBlockingQueue<PathRequest> lowPriorityQueue = 
        new PriorityBlockingQueue<>(300, Comparator.comparingDouble(PathRequest::getPriority));
    
    private static final MultiLayerPathCache cache = new MultiLayerPathCache();
    
    private static final Map<PathRequestKey, CompletableFuture<Path>> pendingRequests = 
        new ConcurrentHashMap<>();
    
    private static final Map<UUID, PlayerPathPrewarmer> playerPrewarmers = 
        new ConcurrentHashMap<>();
    
    public static CompletableFuture<Path> submitPathRequest(
            Mob mob, 
            BlockPos start, 
            BlockPos target,
            PathComputeFunction computeFunction) {
        
        if (!enabled) {
            return CompletableFuture.completedFuture(null);
        }
        
        totalRequests.incrementAndGet();
        
        Path cachedPath = cache.get(start, target);
        if (cachedPath != null) {
            cacheHits.incrementAndGet();
            return CompletableFuture.completedFuture(cachedPath);
        }
        
        PathRequestKey requestKey = new PathRequestKey(start, target);
        CompletableFuture<Path> existingRequest = pendingRequests.get(requestKey);
        if (existingRequest != null) {
            cacheHits.incrementAndGet();
            return existingRequest;
        }
        
        if (activeRequests.get() >= maxConcurrentRequests) {
            queueRejects.incrementAndGet();
            return CompletableFuture.completedFuture(null);
        }
        
        PathRequest request = new PathRequest(mob, start, target, computeFunction);
        
        addToQueue(request);
        
        CompletableFuture<Path> future = new CompletableFuture<>();
        request.setFuture(future);
        pendingRequests.put(requestKey, future);
        
        future.whenComplete((path, throwable) -> {
            pendingRequests.remove(requestKey);
            if (path != null && path.canReach()) {
                cache.put(start, target, path);
            }
        });
        
        return future;
    }
    
    private static void addToQueue(PathRequest request) {
        double distance = request.getDistanceToNearestPlayer();
        
        if (distance < highPriorityDistance) {
            request.setPriority(1.0 / (distance + 1));
            highPriorityQueue.offer(request);
        } else if (distance < mediumPriorityDistance) {
            request.setPriority(1.0 / (distance + 1));
            mediumPriorityQueue.offer(request);
        } else {
            request.setPriority(1.0 / (distance + 1));
            lowPriorityQueue.offer(request);
        }
    }
    
    public static void processTick() {
        if (!enabled) return;
        
        int processed = 0;
        
        while (processed < maxRequestsPerTick && !highPriorityQueue.isEmpty()) {
            PathRequest request = highPriorityQueue.poll();
            if (request != null) {
                processRequest(request);
                processed++;
            }
        }
        
        while (processed < maxRequestsPerTick && !mediumPriorityQueue.isEmpty()) {
            PathRequest request = mediumPriorityQueue.poll();
            if (request != null) {
                processRequest(request);
                processed++;
            }
        }
        
        while (processed < maxRequestsPerTick && !lowPriorityQueue.isEmpty()) {
            PathRequest request = lowPriorityQueue.poll();
            if (request != null) {
                processRequest(request);
                processed++;
            }
        }
        
        cache.cleanupExpired();
        
        if (totalRequests.get() % 6000 == 0) {
            cleanupStaleData();
        }
    }
    
    private static void processRequest(PathRequest request) {
        activeRequests.incrementAndGet();
        
        CompletableFuture.runAsync(() -> {
            try {
                Path path = request.compute();
                request.getFuture().complete(path);
            } catch (Exception e) {
                org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                    "EnhancedPathfindingSystem", "processRequest", e);
                request.getFuture().completeExceptionally(e);
            } finally {
                activeRequests.decrementAndGet();
            }
        }, AsyncPathProcessor.getExecutor());
    }
    
    public static void prewarmPlayerPathsMainThread(ServerPlayer player) {
        if (!enabled) return;
        
        ServerLevel level = player.level();
        List<BlockPos> nearbyPois = PlayerPathPrewarmer.fetchNearbyPois(player, level);
        
        PlayerPathPrewarmer prewarmer = new PlayerPathPrewarmer(player, nearbyPois);
        playerPrewarmers.put(player.getUUID(), prewarmer);
        prewarmer.start();
    }
    
    @Deprecated
    public static void prewarmPlayerPaths(ServerPlayer player) {
        prewarmPlayerPathsMainThread(player);
    }
    
    public static void cleanupPlayer(UUID playerUUID) {
        PlayerPathPrewarmer prewarmer = playerPrewarmers.remove(playerUUID);
        if (prewarmer != null) {
            prewarmer.stop();
        }
    }
    
    public static String getStatistics() {
        long total = totalRequests.get();
        long hits = cacheHits.get();
        long rejects = queueRejects.get();
        double hitRate = total > 0 ? (hits * 100.0 / total) : 0.0;
        
        return String.format(
            "Pathfinding: Active=%d/%d | Queue(H=%d,M=%d,L=%d) | Total=%d | Cache=%.1f%% | Rejects=%d | %s",
            activeRequests.get(), maxConcurrentRequests,
            highPriorityQueue.size(), mediumPriorityQueue.size(), lowPriorityQueue.size(),
            total, hitRate, rejects,
            cache.getStatistics()
        );
    }
    
    public static void updateConfig(
            boolean enabled,
            int maxConcurrent,
            int maxPerTick,
            int highPriorityDist,
            int mediumPriorityDist) {
        
        EnhancedPathfindingSystem.enabled = enabled;
        EnhancedPathfindingSystem.maxConcurrentRequests = maxConcurrent;
        EnhancedPathfindingSystem.maxRequestsPerTick = maxPerTick;
        EnhancedPathfindingSystem.highPriorityDistance = highPriorityDist;
        EnhancedPathfindingSystem.mediumPriorityDistance = mediumPriorityDist;
    }
    
    public static void clear() {
        highPriorityQueue.clear();
        mediumPriorityQueue.clear();
        lowPriorityQueue.clear();
        pendingRequests.clear();
        cache.clear();
        playerPrewarmers.values().forEach(PlayerPathPrewarmer::stop);
        playerPrewarmers.clear();
    }
    
    public static void cleanupStaleData() {
        long now = System.currentTimeMillis();
        
        pendingRequests.entrySet().removeIf(entry -> {
            CompletableFuture<Path> future = entry.getValue();
            return future.isDone() || future.isCancelled() || future.isCompletedExceptionally();
        });
        
        playerPrewarmers.entrySet().removeIf(entry -> {
            PlayerPathPrewarmer prewarmer = entry.getValue();
            return !prewarmer.isActive();
        });
        
        cache.cleanupExpired();
    }
    
    @FunctionalInterface
    public interface PathComputeFunction {
        Path compute() throws Exception;
    }
    
    private static class PathRequest {
        private final Mob mob;
        private final BlockPos start;
        private final BlockPos target;
        private final PathComputeFunction computeFunction;
        private final long createTime;
        private double priority;
        private CompletableFuture<Path> future;
        
        PathRequest(Mob mob, BlockPos start, BlockPos target, PathComputeFunction computeFunction) {
            this.mob = mob;
            this.start = start;
            this.target = target;
            this.computeFunction = computeFunction;
            this.createTime = System.currentTimeMillis();
            this.priority = 0.0;
        }
        
        double getDistanceToNearestPlayer() {
            if (mob == null || mob.level() == null) return Double.MAX_VALUE;
            
            ServerLevel level = (ServerLevel) mob.level();
            double minDistance = Double.MAX_VALUE;
            
            for (ServerPlayer player : level.players()) {
                double distance = mob.distanceToSqr(player);
                if (distance < minDistance) {
                    minDistance = distance;
                }
            }
            
            return Math.sqrt(minDistance);
        }
        
        Path compute() throws Exception {
            return computeFunction.compute();
        }
        
        double getPriority() {
            return priority;
        }
        
        void setPriority(double priority) {
            this.priority = priority;
        }
        
        CompletableFuture<Path> getFuture() {
            return future;
        }
        
        void setFuture(CompletableFuture<Path> future) {
            this.future = future;
        }
    }
    
    private static class PathRequestKey {
        private final long startHash;
        private final long targetHash;
        private final int hashCode;
        
        PathRequestKey(BlockPos start, BlockPos target) {
            this.startHash = encodePos(start);
            this.targetHash = encodePos(target);
            this.hashCode = Objects.hash(startHash, targetHash);
        }
        
        private long encodePos(BlockPos pos) {
            return ((long) pos.getX() << 42) | ((long) pos.getY() << 21) | (long) pos.getZ();
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PathRequestKey)) return false;
            PathRequestKey that = (PathRequestKey) o;
            return startHash == that.startHash && targetHash == that.targetHash;
        }
        
        @Override
        public int hashCode() {
            return hashCode;
        }
    }
}
