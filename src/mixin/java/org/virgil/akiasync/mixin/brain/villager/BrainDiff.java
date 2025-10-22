package org.virgil.akiasync.mixin.brain.villager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
public final class BrainDiff {
    private BlockPos topPoi;
    private Object likedPlayer;
    private int changeCount;
    public BrainDiff() {
        this.changeCount = 0;
    }
    public void setTopPoi(BlockPos pos) {
        this.topPoi = pos;
        this.changeCount++;
    }
    public void setLikedPlayer(Object player) {
        this.likedPlayer = player;
        this.changeCount++;
    }
    @SuppressWarnings("unchecked")
    public <E extends LivingEntity> void applyTo(Brain<E> brain) {
        if (topPoi != null) {
            WalkTarget walkTarget = new WalkTarget(topPoi, 1.0f, 1);
            brain.setMemory(MemoryModuleType.WALK_TARGET, walkTarget);
        }
        if (likedPlayer != null) {
            brain.setMemory(MemoryModuleType.LIKED_PLAYER, (java.util.UUID) likedPlayer);
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
        return String.format("BrainDiff[topPoi=%s, likedPlayer=%s, changes=%d]",
                topPoi != null ? "set" : "null",
                likedPlayer != null ? "set" : "null",
                changeCount);
    }
}