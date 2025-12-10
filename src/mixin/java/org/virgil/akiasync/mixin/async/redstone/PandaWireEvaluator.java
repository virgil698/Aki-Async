package org.virgil.akiasync.mixin.async.redstone;

import com.google.common.collect.Sets;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PandaWireEvaluator {

    private static final Direction[] FACINGS_VERTICAL = {Direction.DOWN, Direction.UP};
    private static final Direction[] FACINGS_HORIZONTAL = {Direction.WEST, Direction.EAST, Direction.NORTH, Direction.SOUTH};
    private static final Direction[] FACINGS = combineArrays(FACINGS_VERTICAL, FACINGS_HORIZONTAL);
    
    private static Direction[] combineArrays(Direction[] a, Direction[] b) {
        Direction[] result = new Direction[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }
    
    private static final Vec3i[] SURROUNDING_BLOCKS_OFFSET;
    static {
        final Set<Vec3i> set = Sets.newLinkedHashSet();
        for (final Direction facing : FACINGS) {
            set.add(new Vec3i(facing.getStepX(), facing.getStepY(), facing.getStepZ()));
        }
        final Set<Vec3i> offsets = Sets.newLinkedHashSet(set);
        for (final Vec3i neighborOffset : offsets) {
            for (final Vec3i adjacentOffset : offsets) {
                set.add(neighborOffset.offset(adjacentOffset));
            }
        }
        set.remove(Vec3i.ZERO);
        SURROUNDING_BLOCKS_OFFSET = set.toArray(new Vec3i[0]);
    }

    private final ServerLevel level;
    private final RedStoneWireBlock wireBlock;
    private final ArrayDeque<BlockPos> turnOff = new ArrayDeque<>();
    private final ArrayDeque<BlockPos> turnOn = new ArrayDeque<>();
    private final LinkedHashSet<BlockPos> updatedRedstoneWire = new LinkedHashSet<>();
    
    private final Map<BlockPos, Integer> powerCache = new HashMap<>();
    
    private static final Map<ServerLevel, Set<BlockPos>> pendingUpdates = new ConcurrentHashMap<>();
    private static final int BATCH_THRESHOLD = 50;
    
    private final AsyncRedstoneNetworkManager networkManager;
    private final RedstoneNetworkCache networkCache;

    public PandaWireEvaluator(ServerLevel level, RedStoneWireBlock wireBlock) {
        this.level = level;
        this.wireBlock = wireBlock;
        this.networkManager = AsyncRedstoneNetworkManager.getInstance(level);
        this.networkCache = RedstoneNetworkCache.getOrCreate(level);
    }

    public void evaluateWire(BlockPos sourcePos, BlockState sourceState) {
        org.virgil.akiasync.mixin.bridge.Bridge bridge = 
            org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        
        powerCache.clear();
        
        if (bridge != null && bridge.isRedstoneNetworkCacheEnabled()) {
            RedstoneNetworkCache.CachedNetwork cachedNetwork = networkManager.getOrCalculateAsync(sourcePos);
            if (cachedNetwork != null && cachedNetwork.isApplicable(level, sourcePos)) {
                if (applyCachedNetwork(cachedNetwork, sourcePos)) {
                    
                    Set<BlockPos> blocksNeedingUpdate = Sets.newLinkedHashSet();
                    for (BlockPos updatedPos : updatedRedstoneWire) {
                        addBlocksNeedingUpdate(updatedPos, blocksNeedingUpdate);
                    }
                    
                    List<BlockPos> reversed = new ArrayList<>(updatedRedstoneWire);
                    Collections.reverse(reversed);
                    for (BlockPos updatedPos : reversed) {
                        addAllSurroundingBlocks(updatedPos, blocksNeedingUpdate);
                    }
                    
                    blocksNeedingUpdate.removeAll(updatedRedstoneWire);
                    
                    if (blocksNeedingUpdate.size() > BATCH_THRESHOLD) {
                        batchNeighborUpdates(blocksNeedingUpdate);
                    } else {
                        for (BlockPos neighborPos : blocksNeedingUpdate) {
                            level.updateNeighborsAt(neighborPos, wireBlock);
                        }
                    }
                    
                    updatedRedstoneWire.clear();
                    powerCache.clear();
                    return;
                }
            }
        }
        
        calculateCurrentChanges(sourcePos, sourceState);
        
        if (bridge != null && bridge.isRedstoneNetworkCacheEnabled() && !updatedRedstoneWire.isEmpty()) {
            List<BlockPos> affectedWires = new ArrayList<>(updatedRedstoneWire);
            Map<BlockPos, Integer> powerChanges = new HashMap<>(powerCache);
            networkManager.cacheNetwork(sourcePos, affectedWires, powerChanges);
        }
        
        Set<BlockPos> blocksNeedingUpdate = Sets.newLinkedHashSet();
        for (BlockPos updatedPos : updatedRedstoneWire) {
            addBlocksNeedingUpdate(updatedPos, blocksNeedingUpdate);
        }
        
        List<BlockPos> reversed = new ArrayList<>(updatedRedstoneWire);
        Collections.reverse(reversed);
        for (BlockPos updatedPos : reversed) {
            addAllSurroundingBlocks(updatedPos, blocksNeedingUpdate);
        }
        
        blocksNeedingUpdate.removeAll(updatedRedstoneWire);
        
        if (blocksNeedingUpdate.size() > BATCH_THRESHOLD) {
            batchNeighborUpdates(blocksNeedingUpdate);
        } else {

            for (BlockPos neighborPos : blocksNeedingUpdate) {
                level.updateNeighborsAt(neighborPos, wireBlock);
            }
        }
        
        updatedRedstoneWire.clear();
        powerCache.clear();
    }
    
    private boolean applyCachedNetwork(RedstoneNetworkCache.CachedNetwork network, BlockPos triggerPos) {
        try {
            
            for (Map.Entry<BlockPos, Integer> entry : network.powerChanges.entrySet()) {
                BlockPos pos = entry.getKey();
                int power = entry.getValue();
                
                BlockState state = level.getBlockState(pos);
                if (state.getBlock() instanceof RedStoneWireBlock) {
                    BlockState newState = state.setValue(RedStoneWireBlock.POWER, power);
                    
                    level.setBlock(pos, newState, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_NEIGHBORS);
                    updatedRedstoneWire.add(pos);
                    powerCache.put(pos, power);
                }
            }
            
            org.virgil.akiasync.mixin.bridge.Bridge bridge = 
                org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
            if (bridge != null && bridge.isDebugLoggingEnabled()) {
                bridge.debugLog("[AkiAsync-Redstone] Applied cached network: %d wires", 
                    network.affectedWires.size());
            }
            
            return true;
        } catch (Exception e) {
            
            networkManager.invalidate(triggerPos);
            return false;
        }
    }

    private void calculateCurrentChanges(BlockPos sourcePos, BlockState sourceState) {

        if (sourceState.is(wireBlock)) {
            turnOff.add(sourcePos);
        } else {

            checkSurroundingWires(sourcePos);
        }
        
        while (!turnOff.isEmpty()) {
            BlockPos pos = turnOff.poll();
            BlockState state = level.getBlockState(pos);
            if (!state.is(wireBlock)) continue;
            
            int oldPower = getCachedPower(pos, state);
            int blockPower = getBlockSignal(pos);
            int wirePower = getIncomingWireSignal(pos);
            int newPower = Math.max(blockPower, wirePower);
            
            if (newPower < oldPower) {

                if (blockPower > 0 && !turnOn.contains(pos)) {
                    turnOn.add(pos);
                }

                setWireState(pos, state, 0);
            } else if (newPower > oldPower) {
                setWireState(pos, state, newPower);
            }
            
            checkSurroundingWires(pos);
        }
        
        while (!turnOn.isEmpty()) {
            BlockPos pos = turnOn.poll();
            BlockState state = level.getBlockState(pos);
            if (!state.is(wireBlock)) continue;
            
            int oldPower = getCachedPower(pos, state);
            int blockPower = getBlockSignal(pos);
            int wirePower = getIncomingWireSignal(pos);
            int newPower = Math.max(blockPower, wirePower);
            
            if (oldPower != newPower) {
                org.virgil.akiasync.mixin.bridge.Bridge bridge = 
                    org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
                if (bridge != null && bridge.isDebugLoggingEnabled()) {
                    bridge.debugLog("[AkiAsync-Redstone] Wire power change at %s: %d -> %d", 
                        pos, oldPower, newPower);
                }
            }
            
            if (newPower > oldPower) {
                setWireState(pos, state, newPower);
            }
            
            checkSurroundingWires(pos);
        }
        
        turnOff.clear();
    }
    
    private int getCachedPower(BlockPos pos, BlockState state) {
        return powerCache.computeIfAbsent(pos, p -> state.getValue(RedStoneWireBlock.POWER));
    }
    
    private int getBlockSignal(BlockPos pos) {
        int maxSignal = 0;
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
            BlockState neighborState = level.getBlockState(neighborPos);
            if (!neighborState.is(wireBlock)) {
                maxSignal = Math.max(maxSignal, level.getSignal(neighborPos, dir));
            }
        }
        return maxSignal;
    }
    
    private int getIncomingWireSignal(BlockPos pos) {
        int maxPower = 0;
        
        for (Direction dir : FACINGS_HORIZONTAL) {
            BlockPos neighborPos = pos.relative(dir);
            BlockState neighborState = level.getBlockState(neighborPos);
            if (neighborState.is(wireBlock)) {
                int power = getCachedPower(neighborPos, neighborState);
                maxPower = Math.max(maxPower, power - 1);
            }
        }
        
        for (Direction vertDir : FACINGS_VERTICAL) {
            BlockPos vertPos = pos.relative(vertDir);
            boolean solidBlock = level.getBlockState(vertPos).isRedstoneConductor(level, vertPos);
            
            for (Direction horizDir : FACINGS_HORIZONTAL) {
                BlockPos adjacentPos = vertPos.relative(horizDir);
                BlockState adjacentState = level.getBlockState(adjacentPos);
                
                if (adjacentState.is(wireBlock)) {

                    boolean canConnect = (vertDir == Direction.UP && !solidBlock) ||
                                       (vertDir == Direction.DOWN && solidBlock && 
                                        !level.getBlockState(adjacentPos).isRedstoneConductor(level, adjacentPos));
                    
                    if (canConnect) {
                        int power = getCachedPower(adjacentPos, adjacentState);
                        maxPower = Math.max(maxPower, power - 1);
                    }
                }
            }
        }
        
        return maxPower;
    }
    
    private void setWireState(BlockPos pos, BlockState state, int power) {
        BlockState newState = state.setValue(RedStoneWireBlock.POWER, power);
        
        level.setBlock(pos, newState, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        powerCache.put(pos, power);
        updatedRedstoneWire.add(pos);
    }
    
    private void checkSurroundingWires(BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        int ownPower = state.is(wireBlock) ? getCachedPower(pos, state) : 0;
        
        for (Direction dir : FACINGS_HORIZONTAL) {
            addWireToList(pos.relative(dir), ownPower);
        }
        
        for (Direction vertDir : FACINGS_VERTICAL) {
            BlockPos vertPos = pos.relative(vertDir);
            boolean solidBlock = level.getBlockState(vertPos).isRedstoneConductor(level, vertPos);
            
            for (Direction horizDir : FACINGS_HORIZONTAL) {
                BlockPos adjacentPos = vertPos.relative(horizDir);
                
                if (vertDir == Direction.UP && !solidBlock) {
                    addWireToList(adjacentPos, ownPower);
                } else if (vertDir == Direction.DOWN && solidBlock && 
                          !level.getBlockState(adjacentPos).isRedstoneConductor(level, adjacentPos)) {
                    addWireToList(adjacentPos, ownPower);
                }
            }
        }
    }
    
    private void addWireToList(BlockPos pos, int otherPower) {
        BlockState state = level.getBlockState(pos);
        if (!state.is(wireBlock)) return;
        
        int power = getCachedPower(pos, state);
        
        if (power < otherPower - 1 && !turnOn.contains(pos)) {
            turnOn.add(pos);
        }
        
        if (power > otherPower && !turnOff.contains(pos)) {
            turnOff.add(pos);
        }
    }
    
    private void addBlocksNeedingUpdate(BlockPos pos, Set<BlockPos> blocksNeedingUpdate) {
        for (Direction dir : FACINGS) {
            BlockPos neighborPos = pos.relative(dir);
            BlockState neighborState = level.getBlockState(neighborPos);
            
            if (neighborState.is(wireBlock)) continue;
            
            if (!neighborState.isAir()) {
                blocksNeedingUpdate.add(neighborPos);
            }
        }
    }
    
    private void addAllSurroundingBlocks(BlockPos pos, Set<BlockPos> blocksNeedingUpdate) {
        for (Vec3i offset : SURROUNDING_BLOCKS_OFFSET) {
            BlockPos neighborPos = pos.offset(offset);
            blocksNeedingUpdate.add(neighborPos);
        }
    }
    
    private void batchNeighborUpdates(Set<BlockPos> updates) {
        org.virgil.akiasync.mixin.bridge.Bridge bridge = 
            org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        
        if (bridge == null || !bridge.isRedstoneUpdateBatchingEnabled()) {

            for (BlockPos pos : updates) {
                level.updateNeighborsAt(pos, wireBlock);
            }
            return;
        }
        
        pendingUpdates.computeIfAbsent(level, k -> ConcurrentHashMap.newKeySet()).addAll(updates);
        
        level.getServer().execute(() -> {
            Set<BlockPos> pending = pendingUpdates.remove(level);
            if (pending != null && !pending.isEmpty()) {
                if (bridge.isDebugLoggingEnabled()) {
                    bridge.debugLog("[AkiAsync-Redstone] Processing %d batched updates", pending.size());
                }
                
                for (BlockPos pos : pending) {
                    level.updateNeighborsAt(pos, wireBlock);
                }
            }
        });
    }
    
    public static void clearLevelCache(ServerLevel level) {
        pendingUpdates.remove(level);
    }
    
    public static void clearAllCaches() {
        pendingUpdates.clear();
    }
}
