package org.virgil.akiasync.mixin.mixins.entity;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.virgil.akiasync.mixin.util.SpatialGrid;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

@SuppressWarnings("unused")
@Mixin(Level.class)
public abstract class SpatialEntityPartitionMixin {
    
    @Unique
    private static volatile boolean enabled = true;
    @Unique
    private static volatile boolean initialized = false;
    @Unique
    private static volatile int gridSize = 4;
    @Unique
    private static volatile int densityThreshold = 50;
    
    @Unique
    private final ConcurrentHashMap<Long, SpatialGrid> spatialGrids = new ConcurrentHashMap<>();
    @Unique
    private long lastGridUpdate = 0;
    @Unique
    private static final long GRID_UPDATE_INTERVAL = 100;
    @Unique
    private final ThreadLocal<Boolean> isUpdatingGrids = ThreadLocal.withInitial(() -> false);
    
    @Inject(
        method = "getEntities(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;",
        at = @At("HEAD"),
        cancellable = true
    )
    private void optimizeDenseEntityQuery(
        Entity except,
        AABB box,
        Predicate<? super Entity> predicate,
        CallbackInfoReturnable<List<Entity>> cir
    ) {
        try {
            
            if (isUpdatingGrids.get()) {
                return;
            }
            
            if (!initialized) {
                akiasync$initSpatialPartition();
            }
            
            if (!enabled || spatialGrids == null) {
                return;
            }
            
            Level level = (Level) (Object) this;
            long currentTime = System.currentTimeMillis();
            
            if (currentTime - lastGridUpdate > GRID_UPDATE_INTERVAL) {
                akiasync$updateSpatialGridsAsync(level);
                lastGridUpdate = currentTime;
            }
            
            long gridKey = akiasync$getGridKey(box);
            SpatialGrid grid = spatialGrids.get(gridKey);
            
            if (grid != null && grid.entityCount >= densityThreshold) {
                List<Entity> result = akiasync$queryWithSpatialPartition(
                    except, box, predicate, grid
                );
                
                if (result != null) {
                    cir.setReturnValue(result);
                }
            }
        } catch (Exception e) {
            
        }
    }
    
    @Unique
    private List<Entity> akiasync$queryWithSpatialPartition(
        Entity except,
        AABB box,
        Predicate<? super Entity> predicate,
        SpatialGrid grid
    ) {
        List<Entity> result = new ArrayList<>();
        
        int minCellX = (int) Math.floor(box.minX / gridSize);
        int maxCellX = (int) Math.floor(box.maxX / gridSize);
        int minCellY = (int) Math.floor(box.minY / gridSize);
        int maxCellY = (int) Math.floor(box.maxY / gridSize);
        int minCellZ = (int) Math.floor(box.minZ / gridSize);
        int maxCellZ = (int) Math.floor(box.maxZ / gridSize);
        
        for (int x = minCellX; x <= maxCellX; x++) {
            for (int y = minCellY; y <= maxCellY; y++) {
                for (int z = minCellZ; z <= maxCellZ; z++) {
                    long cellKey = akiasync$getCellKey(x, y, z);
                    List<Entity> cellEntities = grid.cells.get(cellKey);
                    
                    if (cellEntities != null) {
                        for (Entity entity : cellEntities) {
                            if (entity != except && 
                                !entity.isRemoved() && 
                                entity.getBoundingBox().intersects(box) &&
                                predicate.test(entity)) {
                                result.add(entity);
                            }
                        }
                    }
                }
            }
        }
        
        return result;
    }
    
    @Unique
    private void akiasync$updateSpatialGridsAsync(Level level) {
        if (spatialGrids == null || isUpdatingGrids == null) {
            return;
        }
        
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        
        if (!org.virgil.akiasync.mixin.util.AsyncCollisionProcessor.isAvailable()) {
            
            akiasync$updateSpatialGridsSync(serverLevel);
            return;
        }
        
        org.virgil.akiasync.mixin.bridge.Bridge bridge = 
            org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        
        if (bridge != null && bridge.getCollisionExecutor() != null) {
            bridge.getCollisionExecutor().submit(() -> {
                akiasync$updateSpatialGridsSync(serverLevel);
            });
        }
    }
    
    @Unique
    private void akiasync$updateSpatialGridsSync(ServerLevel serverLevel) {
        if (spatialGrids == null || isUpdatingGrids == null) {
            return;
        }
        
        isUpdatingGrids.set(true);
        try {
            
            spatialGrids.clear();
            
            serverLevel.getAllEntities().forEach(entity -> {
                if (entity == null || entity.isRemoved()) {
                    return;
                }
                
                AABB box = entity.getBoundingBox();
                long gridKey = akiasync$getGridKey(box);
                
                SpatialGrid grid = spatialGrids.computeIfAbsent(
                    gridKey,
                    k -> new SpatialGrid()
                );
                
                grid.entityCount++;
                
                int cellX = (int) Math.floor(entity.getX() / gridSize);
                int cellY = (int) Math.floor(entity.getY() / gridSize);
                int cellZ = (int) Math.floor(entity.getZ() / gridSize);
                long cellKey = akiasync$getCellKey(cellX, cellY, cellZ);
                
                grid.cells.computeIfAbsent(cellKey, k -> new ArrayList<>()).add(entity);
            });
        } catch (Exception e) {
            
        } finally {
            
            isUpdatingGrids.set(false);
        }
    }
    
    @Unique
    private long akiasync$getGridKey(AABB box) {
        
        int chunkX = (int) Math.floor((box.minX + box.maxX) * 0.5 / 16.0);
        int chunkZ = (int) Math.floor((box.minZ + box.maxZ) * 0.5 / 16.0);
        return ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
    }
    
    @Unique
    private long akiasync$getCellKey(int x, int y, int z) {
        return ((long) x << 40) | ((long) (y & 0xFFFFF) << 20) | (z & 0xFFFFF);
    }
    
    @Unique
    private static synchronized void akiasync$initSpatialPartition() {
        if (initialized) return;
        
        org.virgil.akiasync.mixin.bridge.Bridge bridge = 
            org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        
        if (bridge != null) {
            enabled = bridge.isCollisionOptimizationEnabled();
            bridge.debugLog("[AkiAsync] SpatialEntityPartitionMixin initialized: enabled=" + enabled + 
                ", gridSize=" + gridSize + ", densityThreshold=" + densityThreshold);
        }
        
        initialized = true;
    }
}
