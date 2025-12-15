package org.virgil.akiasync.mixin.mixins.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.LevelEntityGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.virgil.akiasync.mixin.util.EntityDensityTracker;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;

@Mixin(ServerLevel.class)
public abstract class EntityDensityTrackerMixin {
    
    @Shadow
    public abstract LevelEntityGetter<net.minecraft.world.entity.Entity> getEntities();
    
    @Unique
    private long lastDensityUpdate = 0;
    
    @Unique
    private static final long UPDATE_INTERVAL_TICKS = 20; 
    
    @Inject(method = "tick", at = @At("TAIL"))
    private void trackEntityDensity(CallbackInfo ci) {
        ServerLevel level = (ServerLevel) (Object) this;
        long currentTick = level.getGameTime();
        
        if (currentTick - lastDensityUpdate < UPDATE_INTERVAL_TICKS) {
            return;
        }
        
        lastDensityUpdate = currentTick;
        
        try {
            
            Long2IntOpenHashMap chunkEntityCount = new Long2IntOpenHashMap();
            
            LevelEntityGetter<net.minecraft.world.entity.Entity> entities = getEntities();
            if (entities != null) {
                for (net.minecraft.world.entity.Entity entity : entities.getAll()) {
                    if (entity == null || entity.isRemoved()) {
                        continue;
                    }
                    
                    int chunkX = entity.chunkPosition().x;
                    int chunkZ = entity.chunkPosition().z;
                    long chunkKey = net.minecraft.world.level.ChunkPos.asLong(chunkX, chunkZ);
                    
                    chunkEntityCount.put(chunkKey, chunkEntityCount.get(chunkKey) + 1);
                }
            }
            
            for (Long2IntOpenHashMap.Entry entry : chunkEntityCount.long2IntEntrySet()) {
                long chunkKey = entry.getLongKey();
                int chunkX = net.minecraft.world.level.ChunkPos.getX(chunkKey);
                int chunkZ = net.minecraft.world.level.ChunkPos.getZ(chunkKey);
                int entityCount = entry.getIntValue();
                
                EntityDensityTracker.updateChunkDensity(chunkX, chunkZ, entityCount);
            }
            
            if (currentTick % (UPDATE_INTERVAL_TICKS * 60) == 0) {
                EntityDensityTracker.cleanup();
            }
            
        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "EntityDensityTracker", "trackDensity", e);
        }
    }
}
