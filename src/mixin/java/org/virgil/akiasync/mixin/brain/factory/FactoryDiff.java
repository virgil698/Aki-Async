package org.virgil.akiasync.mixin.brain.factory;

import java.util.List;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Factory differential (safe reflection)
 * 
 * @author Virgil
 */
public final class FactoryDiff {
    
    private Object recipe;
    private List<ItemStack> inventory;
    private int changeCount;
    
    public FactoryDiff() {}
    
    public void setRecipe(Object r) { this.recipe = r; changeCount++; }
    public void setInventory(List<ItemStack> inv) { this.inventory = inv; changeCount++; }
    
    public void applyTo(BlockEntity be) {
        if (recipe != null) {
            org.virgil.akiasync.mixin.util.REFLECTIONS.setField(be, "recipe", recipe);
        }
        if (inventory != null) {
            org.virgil.akiasync.mixin.util.REFLECTIONS.setField(be, "inventory", inventory);
        }
    }
    
    public boolean hasChanges() { return changeCount > 0; }
}

