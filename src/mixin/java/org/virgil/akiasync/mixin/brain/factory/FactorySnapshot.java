package org.virgil.akiasync.mixin.brain.factory;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;

/**
 * Factory snapshot (Furnace/Hopper/Chest)
 * 
 * @author Virgil
 */
public final class FactorySnapshot {
    
    private final List<ItemStack> inventory;
    private final Object recipeCache;
    private final List<BlockPos> nearbyHoppers;
    private final BlockPos selfPos;
    
    private FactorySnapshot(List<ItemStack> inv, Object recipe, List<BlockPos> hoppers, BlockPos self) {
        this.inventory = inv;
        this.recipeCache = recipe;
        this.nearbyHoppers = hoppers;
        this.selfPos = self;
    }
    
    public static FactorySnapshot capture(BlockEntity be, ServerLevel level) {
        // Inventory snapshot
        List<ItemStack> inventory = be instanceof Container ? 
            IntStream.range(0, ((Container) be).getContainerSize())
                .mapToObj(i -> ((Container) be).getItem(i).copy())
                .collect(Collectors.toList()) : 
            java.util.Collections.emptyList();
        
        // Recipe cache (for Furnace types)
        Object recipeCache = null;
        if (be instanceof AbstractFurnaceBlockEntity) {
            recipeCache = level.getServer().getRecipeManager();
        }
        
        // Nearby hoppers (1x1x1 range for transfer)
        BlockPos pos = be.getBlockPos();
        List<BlockPos> hoppers = be instanceof HopperBlockEntity ? 
            java.util.List.of(pos.above(), pos.below()) : 
            java.util.Collections.emptyList();
        
        return new FactorySnapshot(inventory, recipeCache, hoppers, pos);
    }
    
    public List<ItemStack> inventory() { return inventory; }
    public Object recipeCache() { return recipeCache; }
    public List<BlockPos> nearbyHoppers() { return nearbyHoppers; }
    public BlockPos selfPos() { return selfPos; }
}

