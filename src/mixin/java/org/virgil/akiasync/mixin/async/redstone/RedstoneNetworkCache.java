package org.virgil.akiasync.mixin.async.redstone;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RedstoneNetworkCache {
    
    private static final Map<ServerLevel, RedstoneNetworkCache> LEVEL_CACHES = new ConcurrentHashMap<>();
    
    private final ServerLevel level;
    private final Map<BlockPos, CachedNetwork> networkCache = new ConcurrentHashMap<>();
    private final Map<BlockPos, Long> lastAccessTime = new ConcurrentHashMap<>();
    
    private long lastCleanupTime = 0;
    private static final long CLEANUP_INTERVAL = 600;
    
    private RedstoneNetworkCache(ServerLevel level) {
        this.level = level;
    }
    
    public static RedstoneNetworkCache getOrCreate(ServerLevel level) {
        return LEVEL_CACHES.computeIfAbsent(level, RedstoneNetworkCache::new);
    }
    
    public CachedNetwork getNetwork(BlockPos pos) {
        CachedNetwork network = networkCache.get(pos);
        if (network != null && !network.isInvalidated()) {
            lastAccessTime.put(pos, level.getGameTime());
            return network;
        }
        return null;
    }
    
    public void cacheNetwork(BlockPos sourcePos, List<BlockPos> affectedWires, 
                            Map<BlockPos, Integer> powerChanges) {
        CachedNetwork network = new CachedNetwork(sourcePos, affectedWires, powerChanges);
        
        for (BlockPos pos : affectedWires) {
            networkCache.put(pos, network);
            lastAccessTime.put(pos, level.getGameTime());
        }
    }
    
    public void invalidate(BlockPos pos) {
        CachedNetwork network = networkCache.remove(pos);
        if (network != null) {
            network.invalidate();

            for (BlockPos affectedPos : network.affectedWires) {
                networkCache.remove(affectedPos);
                lastAccessTime.remove(affectedPos);
            }
        }
    }
    
    public void invalidateNearby(BlockPos pos, int radius) {
        List<BlockPos> toRemove = new ArrayList<>();
        for (BlockPos cachedPos : networkCache.keySet()) {
            if (cachedPos.distManhattan(pos) <= radius) {
                toRemove.add(cachedPos);
            }
        }
        toRemove.forEach(this::invalidate);
    }
    
    public void expire(long currentTime) {
        if (currentTime - lastCleanupTime < CLEANUP_INTERVAL) {
            return;
        }
        
        org.virgil.akiasync.mixin.bridge.Bridge bridge = 
            org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        int expireTicks = bridge != null ? bridge.getRedstoneNetworkCacheExpireTicks() : 600;
        
        List<BlockPos> toRemove = new ArrayList<>();
        for (Map.Entry<BlockPos, Long> entry : lastAccessTime.entrySet()) {
            if (currentTime - entry.getValue() > expireTicks) {
                toRemove.add(entry.getKey());
            }
        }
        
        for (BlockPos pos : toRemove) {
            networkCache.remove(pos);
            lastAccessTime.remove(pos);
        }
        
        lastCleanupTime = currentTime;
        
        if (bridge != null && bridge.isDebugLoggingEnabled() && !toRemove.isEmpty()) {
            bridge.debugLog("[AkiAsync-Redstone] Expired %d cached networks", toRemove.size());
        }
    }
    
    public void clear() {
        networkCache.clear();
        lastAccessTime.clear();
    }
    
    public String getStats() {
        return String.format("Networks: %d, Positions: %d", 
            networkCache.values().stream().distinct().count(),
            networkCache.size());
    }
    
    public static void clearAllCaches() {
        for (RedstoneNetworkCache cache : LEVEL_CACHES.values()) {
            cache.clear();
        }
        LEVEL_CACHES.clear();
    }
    
    public static void clearLevelCache(ServerLevel level) {
        RedstoneNetworkCache cache = LEVEL_CACHES.remove(level);
        if (cache != null) {
            cache.clear();
        }
    }
    
    public static class CachedNetwork {
        public final BlockPos sourcePos;
        public final List<BlockPos> affectedWires;
        public final Map<BlockPos, Integer> powerChanges;
        private volatile boolean invalidated = false;
        
        public CachedNetwork(BlockPos sourcePos, List<BlockPos> affectedWires, 
                           Map<BlockPos, Integer> powerChanges) {
            this.sourcePos = sourcePos;
            this.affectedWires = new ArrayList<>(affectedWires);
            this.powerChanges = new HashMap<>(powerChanges);
        }
        
        public void invalidate() {
            invalidated = true;
        }
        
        public boolean isInvalidated() {
            return invalidated;
        }
        
        public boolean isApplicable(ServerLevel level, BlockPos triggerPos) {
            if (invalidated) return false;
            
            if (!affectedWires.contains(triggerPos) && !triggerPos.equals(sourcePos)) {
                return false;
            }
            
            for (BlockPos pos : affectedWires) {
                BlockState state = level.getBlockState(pos);
                if (!(state.getBlock() instanceof net.minecraft.world.level.block.RedStoneWireBlock)) {
                    return false;
                }
            }
            
            return true;
        }
    }
}
