package org.virgil.akiasync.mixin.async.chunk;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

public final class ChunkSnapshot {
    private final LevelChunk chunk;
    private final Map<BlockPos, BlockState> blockStates;
    private final List<BlockPos> tickingBlocks;

    private ChunkSnapshot(LevelChunk chunk) {
        this.chunk = chunk;
        this.blockStates = new ConcurrentHashMap<>(256);
        this.tickingBlocks = new ArrayList<>(64);
    }

    public static ChunkSnapshot capture(LevelChunk chunk) {
        ChunkSnapshot snapshot = new ChunkSnapshot(chunk);
        for (BlockPos pos : chunk.getBlockEntities().keySet()) {
            BlockState state = chunk.getBlockState(pos);
            snapshot.blockStates.put(pos.immutable(), state);
            if (state.isRandomlyTicking()) {
                snapshot.tickingBlocks.add(pos.immutable());
            }
        }
        return snapshot;
    }

    public LevelChunk getChunk() {
        return chunk;
    }

    public Map<BlockPos, BlockState> getBlockStates() {
        return blockStates;
    }

    public List<BlockPos> getTickingBlocks() {
        return tickingBlocks;
    }
}
