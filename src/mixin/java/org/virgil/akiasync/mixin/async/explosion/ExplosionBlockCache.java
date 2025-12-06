package org.virgil.akiasync.mixin.async.explosion;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

public class ExplosionBlockCache {
    private final BlockPos pos;
    private final BlockState blockState;
    private final FluidState fluidState;
    private final float resistance;
    private final long cacheTime;
    
    public ExplosionBlockCache(BlockPos pos, BlockState blockState, FluidState fluidState, float resistance) {
        this.pos = pos.immutable();
        this.blockState = blockState;
        this.fluidState = fluidState;
        this.resistance = resistance;
        this.cacheTime = System.currentTimeMillis();
    }
    
    public BlockPos getPos() {
        return pos;
    }
    
    public BlockState getBlockState() {
        return blockState;
    }
    
    public FluidState getFluidState() {
        return fluidState;
    }
    
    public float getResistance() {
        return resistance;
    }
    
    public long getCacheTime() {
        return cacheTime;
    }
    
    public boolean isExpired() {
        return System.currentTimeMillis() - cacheTime > 100;
    }
}
