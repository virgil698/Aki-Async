package org.virgil.akiasync.mixin.brain.universal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.ExpirableValue;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;

public final class UniversalAiDiff {
    
    private UUID target;
    private int changeCount;
    
    private Map<MemoryModuleType<?>, Object> memoryChanges;
    
    private BlockPos topPoi;
    private Object likedPlayer;
    
    public UniversalAiDiff() {
        this.target = null;
        this.changeCount = 0;
        this.memoryChanges = null;
        this.topPoi = null;
        this.likedPlayer = null;
    }
    
    public void setTarget(UUID id) {
        this.target = id;
        changeCount++;
    }
    
    public void setMemory(MemoryModuleType<?> type, Object value) {
        if (memoryChanges == null) {
            memoryChanges = new HashMap<>();
        }
        memoryChanges.put(type, value);
        changeCount++;
    }
    
    public void setTopPoi(BlockPos pos) {
        this.topPoi = pos;
        changeCount++;
    }
    
    public void setLikedPlayer(Object player) {
        this.likedPlayer = player;
        changeCount++;
    }
    
    public void applyTo(Mob mob, ServerLevel level) {
        
        if (target != null) {
            net.minecraft.world.entity.player.Player player = level.getPlayerByUUID(target);
            if (player != null && !player.isRemoved()) {
                mob.setTarget(player);
            }
        }
        
        if (memoryChanges != null && !memoryChanges.isEmpty()) {
            Brain<?> brain = mob.getBrain();
            if (brain != null) {
                applyBrainMemoryChanges(brain);
            }
        }
        
        if (topPoi != null || likedPlayer != null) {
            Brain<?> brain = mob.getBrain();
            if (brain != null) {
                applyPoiChanges(brain);
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    private void applyBrainMemoryChanges(Brain<?> brain) {
        for (Map.Entry<MemoryModuleType<?>, Object> entry : memoryChanges.entrySet()) {
            try {
                MemoryModuleType<Object> type = (MemoryModuleType<Object>) entry.getKey();
                Object value = entry.getValue();
                
                if (value instanceof ExpirableValue) {
                    brain.setMemory(type, (ExpirableValue<Object>) value);
                } else if (value != null) {
                    brain.setMemory(type, value);
                } else {
                    brain.eraseMemory(type);
                }
            } catch (Exception e) {
                org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                    "UniversalAiDiff", "applyBrainMemoryChanges", e);
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    private void applyPoiChanges(Brain<?> brain) {
        if (topPoi != null) {
            WalkTarget walkTarget = new WalkTarget(topPoi, 1.0f, 1);
            brain.setMemory(MemoryModuleType.WALK_TARGET, walkTarget);
        }
        
        if (likedPlayer != null) {
            brain.setMemory(MemoryModuleType.LIKED_PLAYER, (UUID) likedPlayer);
        }
    }
    
    public boolean hasChanges() { 
        return changeCount > 0; 
    }
    
    public int getChangeCount() {
        return changeCount;
    }
    
    @Override
    public String toString() {
        return String.format("UniversalAiDiff[target=%s, topPoi=%s, likedPlayer=%s, memoryChanges=%d, changes=%d]",
                target != null ? "set" : "null",
                topPoi != null ? "set" : "null",
                likedPlayer != null ? "set" : "null",
                memoryChanges != null ? memoryChanges.size() : 0,
                changeCount);
    }
}
