package org.virgil.akiasync.mixin.pathfinding;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.level.pathfinder.Path;
import org.virgil.akiasync.mixin.util.BridgeConfigCache;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class PlayerPathPrewarmer {
    
    private static final int PREWARM_RADIUS = 32;           
    private static final int MAX_MOBS_PER_BATCH = 5;        
    private static final int MAX_POIS_PER_MOB = 3;          
    private static final long BATCH_DELAY_MS = 100;         
    private static final long TOTAL_PREWARM_TIME_MS = 5000; 
    
    private final ServerPlayer player;
    private final ServerLevel level;
    private final List<BlockPos> nearbyPois;  
    private volatile boolean running = false;
    private ScheduledExecutorService scheduler;
    
    public PlayerPathPrewarmer(ServerPlayer player, List<BlockPos> nearbyPois) {
        this.player = player;
        this.level = player.level();
        this.nearbyPois = nearbyPois;
    }
    
    public void start() {
        if (running) return;
        
        running = true;
        scheduler = new ScheduledThreadPoolExecutor(1, r -> {
            Thread thread = new Thread(r, "PathPrewarmer");
            thread.setDaemon(true);
            return thread;
        });
        
        BridgeConfigCache.debugLog("[PathPrewarmer] Starting prewarm for player");
        
        CompletableFuture.runAsync(this::prewarmPaths, scheduler);
    }
    
    public void stop() {
        running = false;
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                scheduler.awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                scheduler.shutdownNow();
                BridgeConfigCache.debugLog("[PathPrewarmer] Interrupted during shutdown");
            }
        }
    }
    
    public boolean isActive() {
        return running && scheduler != null && !scheduler.isShutdown();
    }
    
    private void prewarmPaths() {
        try {
            
            List<Mob> nearbyMobs = getNearbyMobs();
            if (nearbyMobs.isEmpty()) {
                BridgeConfigCache.debugLog("[PathPrewarmer] No mobs found near player");
                return;
            }
            
            BridgeConfigCache.debugLog("[PathPrewarmer] Found " + nearbyMobs.size() + " mobs to prewarm");
            
            if (nearbyPois.isEmpty()) {
                BridgeConfigCache.debugLog("[PathPrewarmer] No POIs found near player");
                return;
            }
            
            BridgeConfigCache.debugLog("[PathPrewarmer] Found " + nearbyPois.size() + " POIs to prewarm");
            
            int totalBatches = (int) Math.ceil((double) nearbyMobs.size() / MAX_MOBS_PER_BATCH);
            int prewarmCount = 0;
            
            for (int batch = 0; batch < totalBatches && running; batch++) {
                int startIdx = batch * MAX_MOBS_PER_BATCH;
                int endIdx = Math.min(startIdx + MAX_MOBS_PER_BATCH, nearbyMobs.size());
                
                List<Mob> batchMobs = nearbyMobs.subList(startIdx, endIdx);
                prewarmCount += prewarmBatch(batchMobs, nearbyPois);
                
                if (batch < totalBatches - 1 && running) {
                    Thread.sleep(BATCH_DELAY_MS);
                }
            }
            
            BridgeConfigCache.debugLog("[PathPrewarmer] Completed prewarm: " + prewarmCount + " paths cached");
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            BridgeConfigCache.debugLog("[PathPrewarmer] Prewarm interrupted");
        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "PlayerPathPrewarmer", "prewarmPaths", e);
        } finally {
            stop();
        }
    }
    
    private int prewarmBatch(List<Mob> mobs, List<BlockPos> pois) {
        int count = 0;
        
        for (Mob mob : mobs) {
            if (!running || mob.isRemoved()) continue;
            
            BlockPos mobPos = mob.blockPosition();
            
            List<BlockPos> closestPois = findClosestPois(mobPos, pois, MAX_POIS_PER_MOB);
            
            for (BlockPos poi : closestPois) {
                if (!running) break;
                
                try {
                    
                    prewarmPath(mob, mobPos, poi);
                    count++;
                } catch (Exception e) {
                    org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                        "PlayerPathPrewarmer", "prewarmPath", e);
                }
            }
        }
        
        return count;
    }
    
    private void prewarmPath(Mob mob, BlockPos start, BlockPos target) {
        
        if (start.distSqr(target) > PREWARM_RADIUS * PREWARM_RADIUS) {
            return;
        }
        
        PathCacheKey key = new PathCacheKey(start, target);
        
    }
    
    private List<Mob> getNearbyMobs() {
        BlockPos playerPos = player.blockPosition();
        List<Mob> mobs = new ArrayList<>();
        
        level.getEntitiesOfClass(
            PathfinderMob.class,
            player.getBoundingBox().inflate(PREWARM_RADIUS),
            mob -> !mob.isRemoved() && mob.isAlive()
        ).forEach(mobs::add);
        
        mobs.sort(Comparator.comparingDouble(mob -> 
            mob.distanceToSqr(player)
        ));
        
        return mobs;
    }
    
    public static List<BlockPos> fetchNearbyPois(ServerPlayer player, ServerLevel level) {
        BlockPos playerPos = player.blockPosition();
        PoiManager poiManager = level.getPoiManager();
        
        Set<BlockPos> pois = new HashSet<>();
        
        poiManager.getInRange(
            type -> type.is(PoiTypes.MEETING) || 
                    type.is(PoiTypes.HOME) ||
                    type.is(PoiTypes.ARMORER) ||
                    type.is(PoiTypes.BUTCHER) ||
                    type.is(PoiTypes.CARTOGRAPHER) ||
                    type.is(PoiTypes.CLERIC) ||
                    type.is(PoiTypes.FARMER) ||
                    type.is(PoiTypes.FISHERMAN) ||
                    type.is(PoiTypes.FLETCHER) ||
                    type.is(PoiTypes.LEATHERWORKER) ||
                    type.is(PoiTypes.LIBRARIAN) ||
                    type.is(PoiTypes.MASON) ||
                    type.is(PoiTypes.SHEPHERD) ||
                    type.is(PoiTypes.TOOLSMITH) ||
                    type.is(PoiTypes.WEAPONSMITH),
            playerPos,
            PREWARM_RADIUS,
            PoiManager.Occupancy.ANY
        ).map(PoiRecord::getPos).forEach(pois::add);
        
        return new ArrayList<>(pois);
    }
    
    private List<BlockPos> findClosestPois(BlockPos start, List<BlockPos> pois, int count) {
        return pois.stream()
            .sorted(Comparator.comparingDouble(poi -> start.distSqr(poi)))
            .limit(count)
            .collect(Collectors.toList());
    }
}
