package org.virgil.akiasync.mixin.mixins.recipe;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.virgil.akiasync.mixin.util.BridgeConfigCache;
import org.virgil.akiasync.mixin.bridge.Bridge;
import org.virgil.akiasync.mixin.bridge.BridgeManager;

import java.util.Optional;

@SuppressWarnings("unused")
@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class FurnaceRecipeCacheMixin {

    @Unique private static volatile boolean enabled;
    @Unique private static volatile int cacheSize;
    @Unique private static volatile boolean applyToBlastFurnace;
    @Unique private static volatile boolean applyToSmoker;
    @Unique private static volatile boolean fixBurnTimeBug;
    @Unique private static volatile boolean initialized = false;
    @Unique private static int cacheHits = 0;
    @Unique private static int cacheMisses = 0;
    @Unique private static int totalLookups = 0;

    @Unique private ItemStack akiasync$cachedInput = ItemStack.EMPTY;
    @Unique private long akiasync$lastCacheTime = 0;

    @Shadow
    @Final
    public RecipeType<? extends AbstractCookingRecipe> recipeType;

    @Shadow
    public double cookSpeedMultiplier;

    @Inject(method = "serverTick", at = @At("HEAD"))
    private static void onServerTick(ServerLevel level, BlockPos pos, BlockState state,
                                     AbstractFurnaceBlockEntity blockEntity, CallbackInfo ci) {
        if (!initialized) {
            akiasync$initFurnaceCache();
        }
    }

    @Unique private RecipeHolder<? extends AbstractCookingRecipe> akiasync$cachedRecipe;
    @Unique private int akiasync$cachedCookTime = 0;

    @Inject(method = "getTotalCookTime", at = @At("HEAD"), cancellable = true)
    private static void optimizeCookTimeCalculation(ServerLevel level, AbstractFurnaceBlockEntity furnace,
                                                   RecipeType<? extends AbstractCookingRecipe> recipeType,
                                                   double cookSpeedMultiplier,
                                                   CallbackInfoReturnable<Integer> cir) {
        if (!initialized) {
            akiasync$initFurnaceCache();
        }

        if (!enabled) return;

        if (!akiasync$shouldApplyCacheStatic(furnace)) return;

        try {
            FurnaceRecipeCacheMixin mixin = (FurnaceRecipeCacheMixin) (Object) furnace;

            totalLookups++;

            ItemStack inputItem = furnace.getItem(0);
            if (mixin.akiasync$isCacheValidSimple(inputItem)) {
                if (mixin.akiasync$cachedRecipe != null && mixin.akiasync$cachedCookTime > 0) {
                    cacheHits++;

                    int finalCookTime = (int) Math.ceil(mixin.akiasync$cachedCookTime / cookSpeedMultiplier);
                    cir.setReturnValue(finalCookTime);

                    if (totalLookups % 1000 == 0) {
                        akiasync$logCacheStatsStatic();
                    }
                    return;
                }
            }

            cacheMisses++;
        } catch (Throwable t) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "FurnaceRecipeCache", "optimizeCookTime",
                t instanceof Exception ? (Exception) t : new RuntimeException(t));
        }
    }

    @Inject(method = "getTotalCookTime", at = @At("RETURN"))
    private static void cacheRecipeResult(ServerLevel level, AbstractFurnaceBlockEntity furnace,
                                         RecipeType<? extends AbstractCookingRecipe> recipeType,
                                         double cookSpeedMultiplier,
                                         CallbackInfoReturnable<Integer> cir) {
        if (!enabled) return;
        if (!akiasync$shouldApplyCacheStatic(furnace)) return;

        try {
            FurnaceRecipeCacheMixin mixin = (FurnaceRecipeCacheMixin) (Object) furnace;
            ItemStack inputItem = furnace.getItem(0);

            if (!inputItem.isEmpty()) {
                SingleRecipeInput input = new SingleRecipeInput(inputItem);
                var recipe = level.recipeAccess().getRecipeFor(recipeType, input, level);

                if (recipe.isPresent()) {
                    RecipeHolder<? extends AbstractCookingRecipe> recipeHolder = recipe.get();
                    mixin.akiasync$cachedRecipe = recipeHolder;
                    mixin.akiasync$cachedCookTime = recipeHolder.value().cookingTime();
                    mixin.akiasync$cachedInput = inputItem.copy();
                    mixin.akiasync$lastCacheTime = System.currentTimeMillis();
                }
            }
        } catch (Throwable t) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "FurnaceRecipeCache", "cacheRecipeResult",
                t instanceof Exception ? (Exception) t : new RuntimeException(t));
        }
    }

    @Inject(method = "setItem", at = @At("HEAD"))
    private void clearCacheOnItemChange(int slot, ItemStack stack, CallbackInfo ci) {
        if (!enabled) return;

        if (slot == 0) {
            akiasync$clearCache();
        }
    }

    @Unique
    private boolean akiasync$isCacheValidSimple(ItemStack input) {
        if (input == null || akiasync$cachedInput == null || akiasync$cachedInput.isEmpty()) return false;

        if (!ItemStack.isSameItemSameComponents(akiasync$cachedInput, input)) {
            return false;
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime - akiasync$lastCacheTime > 60000) {
            return false;
        }

        return true;
    }

    @Unique
    private void akiasync$clearCache() {
        akiasync$cachedInput = ItemStack.EMPTY;
        akiasync$cachedCookTime = 0;
        akiasync$lastCacheTime = 0;
    }

    @Unique
    private boolean akiasync$shouldApplyCache() {
        return akiasync$shouldApplyCacheStatic((AbstractFurnaceBlockEntity) (Object) this);
    }

    @Unique
    private static boolean akiasync$shouldApplyCacheStatic(AbstractFurnaceBlockEntity furnace) {
        try {
            RecipeType<?> type = furnace.recipeType;
            if (type == null) {
                return true;
            }

            if (type == RecipeType.SMELTING) {
                return true;
            }
            if (type == RecipeType.BLASTING) {
                return applyToBlastFurnace;
            }
            if (type == RecipeType.SMOKING) {
                return applyToSmoker;
            }

            return true;
        } catch (Throwable t) {
            return true;
        }
    }

    @Unique
    private static void akiasync$logCacheStatsStatic() {
        try {
            double hitRate = totalLookups > 0 ?
                (double) cacheHits / totalLookups * 100 : 0;

            BridgeConfigCache.debugLog(
                "[AkiAsync-FurnaceCache] Stats: Lookups=%d, Hits=%d, Misses=%d, HitRate=%.2f%%",
                totalLookups, cacheHits, cacheMisses, hitRate
            );
        } catch (Throwable t) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "FurnaceRecipeCache", "logCacheStats",
                t instanceof Exception ? (Exception) t : new RuntimeException(t));
        }
    }

    @Unique
    private static synchronized void akiasync$initFurnaceCache() {
        if (initialized) return;

        Bridge bridge = BridgeManager.getBridge();

        if (bridge != null) {
            enabled = bridge.isFurnaceRecipeCacheEnabled();
            cacheSize = bridge.getFurnaceRecipeCacheSize();
            applyToBlastFurnace = bridge.isFurnaceCacheApplyToBlastFurnace();
            applyToSmoker = bridge.isFurnaceCacheApplyToSmoker();
            fixBurnTimeBug = bridge.isFurnaceFixBurnTimeBug();

            BridgeConfigCache.debugLog("[AkiAsync] FurnaceRecipeCache initialized:");
            BridgeConfigCache.debugLog("  - Enabled: " + enabled);
            BridgeConfigCache.debugLog("  - Cache size: " + cacheSize);
            BridgeConfigCache.debugLog("  - Apply to blast furnace: " + applyToBlastFurnace);
            BridgeConfigCache.debugLog("  - Apply to smoker: " + applyToSmoker);
            BridgeConfigCache.debugLog("  - Fix burn time bug: " + fixBurnTimeBug);
        } else {
            enabled = false;
        }

        initialized = true;
    }
}
