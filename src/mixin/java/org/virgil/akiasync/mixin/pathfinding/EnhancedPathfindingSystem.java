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

/**
 * 增强版寻路系统
 * 
 * 特性：
 * 1. 三级优先级队列（高/中/低）
 * 2. 多层缓存机制（热缓存/温缓存/冷缓存）
 * 3. 预热机制（玩家加入时预计算常用路径）
 * 4. 流量控制（限制并发请求数）
 * 5. 智能去重（相似路径复用）
 * 
 * @author AkiAsync
 */
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
    
    /**
     * 提交寻路请求
     */
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
    
    /**
     * 根据优先级添加到队列
     */
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
    
    /**
     * 每 tick 处理队列
     */
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
    }
    
    /**
     * 处理单个请求
     */
    private static void processRequest(PathRequest request) {
        activeRequests.incrementAndGet();
        
        CompletableFuture.runAsync(() -> {
            try {
                Path path = request.compute();
                request.getFuture().complete(path);
            } catch (Exception e) {
                request.getFuture().completeExceptionally(e);
                BridgeConfigCache.debugLog("[PathfindingSystem] Request failed: " + e.getMessage());
            } finally {
                activeRequests.decrementAndGet();
            }
        }, AsyncPathProcessor.getExecutor());
    }
    
    /**
     * 玩家加入时预热路径（主线程调用）
     * 
     * 此方法必须在主线程调用，因为需要访问 POI 数据
     */
    public static void prewarmPlayerPathsMainThread(ServerPlayer player) {
        if (!enabled) return;
        
        ServerLevel level = player.level();
        List<BlockPos> nearbyPois = PlayerPathPrewarmer.fetchNearbyPois(player, level);
        
        PlayerPathPrewarmer prewarmer = new PlayerPathPrewarmer(player, nearbyPois);
        playerPrewarmers.put(player.getUUID(), prewarmer);
        prewarmer.start();
    }
    
    /**
     * @deprecated 使用 prewarmPlayerPathsMainThread 代替
     */
    @Deprecated
    public static void prewarmPlayerPaths(ServerPlayer player) {
        prewarmPlayerPathsMainThread(player);
    }
    
    /**
     * 玩家离开时清理
     */
    public static void cleanupPlayer(UUID playerUUID) {
        PlayerPathPrewarmer prewarmer = playerPrewarmers.remove(playerUUID);
        if (prewarmer != null) {
            prewarmer.stop();
        }
    }
    
    /**
     * 获取统计信息
     */
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
    
    /**
     * 配置更新
     */
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
    
    /**
     * 清空所有队列和缓存
     */
    public static void clear() {
        highPriorityQueue.clear();
        mediumPriorityQueue.clear();
        lowPriorityQueue.clear();
        pendingRequests.clear();
        cache.clear();
        playerPrewarmers.values().forEach(PlayerPathPrewarmer::stop);
        playerPrewarmers.clear();
    }
    
    /**
     * 路径计算函数接口
     */
    @FunctionalInterface
    public interface PathComputeFunction {
        Path compute() throws Exception;
    }
    
    /**
     * 路径请求
     */
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
    
    /**
     * 请求去重 Key
     */
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
