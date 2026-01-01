package org.virgil.akiasync.mixin.mixins.chunk;

import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.virgil.akiasync.mixin.bridge.Bridge;
import org.virgil.akiasync.mixin.util.BridgeConfigCache;


@Mixin(ChunkPos.class)
public class ChunkPosOptimizationMixin {
    
    @Unique
    private static boolean akiasync$initialized = false;
    
    @Unique
    private static boolean akiasync$enabled = true;
    
    @Shadow @Final public int x;
    @Shadow @Final public int z;
    
    @Unique
    private static void akiasync$initialize() {
        if (akiasync$initialized) {
            return;
        }
        
        try {
            Bridge bridge = BridgeConfigCache.getBridge();
            if (bridge != null) {
                akiasync$enabled = bridge.isChunkPosOptimizationEnabled();
            
                akiasync$initialized = true;
            }
        } catch (Exception e) {
            
            akiasync$enabled = true;
        }
    }
    
    
    @Overwrite
    @Override
    public boolean equals(Object obj) {
        
        if (!akiasync$initialized) {
            akiasync$initialize();
        }
        
        
        if (!akiasync$enabled) {
            if (!(obj instanceof ChunkPos)) return false;
            ChunkPos that = (ChunkPos) obj;
            return this.x == that.x && this.z == that.z;
        }
        
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        
        ChunkPos that = (ChunkPos) obj;
        return this.x == that.x && this.z == that.z;
    }
}
