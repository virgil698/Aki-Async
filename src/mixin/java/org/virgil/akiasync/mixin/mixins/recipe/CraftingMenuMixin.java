package org.virgil.akiasync.mixin.mixins.recipe;

import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings("unused")
@Mixin(CraftingMenu.class)
public abstract class CraftingMenuMixin {
    
    @Unique private static volatile boolean enabled;
    @Unique private static volatile int cacheSize;
    @Unique private static volatile boolean optimizeBatchCrafting;
    @Unique private static volatile boolean reduceNetworkTraffic;
    @Unique private static volatile boolean initialized = false;
    @Unique private static int cacheHits = 0;
    @Unique private static int cacheMisses = 0;
    @Unique private static int totalLookups = 0;
    @Unique private NonNullList<ItemStack> akiasync$cachedInput;
    @Unique private long akiasync$lastCacheTime = 0;
    
    @Inject(method = "slotsChanged", at = @At("HEAD"), cancellable = true)
    private void optimizeSlotsChanged(Container inventory, CallbackInfo ci) {
        if (!initialized) {
            akiasync$initCraftingCache();
        }
        
        if (!enabled) return;
        
        try {
            if (!(inventory instanceof CraftingContainer craftingContainer)) {
                return;
            }
            
            totalLookups++;
            
            if (akiasync$isCacheValid(craftingContainer)) {
                cacheHits++;
                
                if (totalLookups % 500 == 0) {
                    akiasync$logCacheStats();
                }
                
                ci.cancel();
                return;
            }
            
            cacheMisses++;
            akiasync$updateCache(craftingContainer);
            
        } catch (Throwable t) {
        }
    }
    
    @Unique
    private boolean akiasync$isCacheValid(CraftingContainer container) {
        if (akiasync$cachedInput == null) {
            return false;
        }
        
        long currentTime = System.currentTimeMillis();
        if (currentTime - akiasync$lastCacheTime > 60000) {
            return false;
        }
        
        if (akiasync$cachedInput.size() != container.getContainerSize()) {
            return false;
        }
        
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack cached = akiasync$cachedInput.get(i);
            ItemStack current = container.getItem(i);
            
            if (!ItemStack.matches(cached, current)) {
                return false;
            }
        }
        
        return true;
    }
    
    @Unique
    private void akiasync$updateCache(CraftingContainer container) {
        akiasync$cachedInput = NonNullList.withSize(container.getContainerSize(), ItemStack.EMPTY);
        for (int i = 0; i < container.getContainerSize(); i++) {
            akiasync$cachedInput.set(i, container.getItem(i).copy());
        }
        akiasync$lastCacheTime = System.currentTimeMillis();
    }
    
    @Unique
    private void akiasync$logCacheStats() {
        try {
            org.virgil.akiasync.mixin.bridge.Bridge bridge = 
                org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
            
            if (bridge != null) {
                double hitRate = totalLookups > 0 ? 
                    (double) cacheHits / totalLookups * 100 : 0;
                
                bridge.debugLog(
                    "[AkiAsync-CraftingCache] Stats: Lookups=%d, Hits=%d, Misses=%d, HitRate=%.2f%%",
                    totalLookups, cacheHits, cacheMisses, hitRate
                );
            }
        } catch (Throwable t) {
        }
    }
    
    @Unique
    private static synchronized void akiasync$initCraftingCache() {
        if (initialized) return;
        
        org.virgil.akiasync.mixin.bridge.Bridge bridge = 
            org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        
        if (bridge != null) {
            enabled = bridge.isCraftingRecipeCacheEnabled();
            cacheSize = bridge.getCraftingRecipeCacheSize();
            optimizeBatchCrafting = bridge.isCraftingOptimizeBatchCrafting();
            reduceNetworkTraffic = bridge.isCraftingReduceNetworkTraffic();
            
            bridge.debugLog("[AkiAsync] CraftingRecipeCache initialized:");
            bridge.debugLog("  - Enabled: " + enabled);
            bridge.debugLog("  - Cache size: " + cacheSize);
            bridge.debugLog("  - Optimize batch crafting: " + optimizeBatchCrafting);
            bridge.debugLog("  - Reduce network traffic: " + reduceNetworkTraffic);
        } else {
            enabled = false;
        }
        
        initialized = true;
    }
}
