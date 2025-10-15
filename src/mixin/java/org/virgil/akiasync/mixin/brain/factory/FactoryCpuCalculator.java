package org.virgil.akiasync.mixin.brain.factory;

import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;

/**
 * Factory CPU calculator (Furnace/Hopper/Chest scoring)
 * 
 * @author Virgil
 */
public final class FactoryCpuCalculator {
    
    public static FactoryDiff runCpuOnly(BlockEntity be, FactorySnapshot snap) {
        FactoryDiff diff = new FactoryDiff();
        
        // Furnace: recipe cache
        if (be instanceof AbstractFurnaceBlockEntity && snap.recipeCache() != null) {
            diff.setRecipe(snap.recipeCache());
        }
        
        // Hopper: inventory transfer
        else if (be instanceof HopperBlockEntity && !snap.inventory().isEmpty()) {
            diff.setInventory(snap.inventory());
        }
        
        // Chest: inventory storage
        else if (be instanceof ChestBlockEntity && snap.inventory().size() > 10) {
            diff.setInventory(snap.inventory());
        }
        
        return diff;
    }
}

