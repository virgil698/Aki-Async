package org.virgil.akiasync.mixin.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class BlockTickTask {
    public final BlockPos pos;
    public final Block block;
    public final BlockState state;
    public final BlockTickCategory category;

    public BlockTickTask(BlockPos pos, Block block, BlockState state, BlockTickCategory category) {
        this.pos = pos;
        this.block = block;
        this.state = state;
        this.category = category;
    }
}
