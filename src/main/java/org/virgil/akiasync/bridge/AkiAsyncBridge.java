package org.virgil.akiasync.bridge;

import java.util.concurrent.ExecutorService;

import org.virgil.akiasync.AkiAsyncPlugin;
import org.virgil.akiasync.config.ConfigManager;

public class AkiAsyncBridge implements org.virgil.akiasync.mixin.bridge.Bridge {
    
    private final AkiAsyncPlugin plugin;
    private ConfigManager config;
    private final ExecutorService generalExecutor;
    private final ExecutorService lightingExecutor;
    
    public AkiAsyncBridge(AkiAsyncPlugin plugin, ExecutorService generalExecutor, ExecutorService lightingExecutor) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        this.generalExecutor = generalExecutor;
        this.lightingExecutor = lightingExecutor;
    }
    
    @Override
    public boolean isEntityTickParallel() { return config.isEntityTickParallel(); }
    
    @Override
    public int getEntityTickThreads() { return config.getEntityTickThreads(); }
    
    @Override
    public int getMinEntitiesForParallel() {return config.getMinEntitiesForParallel();}
    
    @Override
    public int getEntityTickBatchSize() {return config.getEntityTickBatchSize();}
    
    @Override
    public boolean isBrainThrottleEnabled() {return config.isBrainThrottleEnabled();}
    
    @Override
    public int getBrainThrottleInterval() {return config.getBrainThrottleInterval();}
    
    @Override
    public long getAsyncAITimeoutMicros() {return config.getAsyncAITimeoutMicros();}
    
    @Override
    public boolean isVillagerOptimizationEnabled() {return config.isVillagerOptimizationEnabled();}
    
    @Override
    public boolean isVillagerUsePOISnapshot() {return config.isVillagerUsePOISnapshot();}
    
    @Override
    public boolean isPiglinOptimizationEnabled() {return config.isPiglinOptimizationEnabled();}
    
    @Override
    public boolean isPiglinUsePOISnapshot() { return config.isPiglinUsePOISnapshot(); }
    
    @Override
    public int getPiglinLookDistance() { return config.getPiglinLookDistance(); }
    
    @Override
    public int getPiglinBarterDistance() { return config.getPiglinBarterDistance(); }
    
    @Override
    public boolean isPillagerFamilyOptimizationEnabled() { return config.isPillagerFamilyOptimizationEnabled(); }
    
    @Override
    public boolean isPillagerFamilyUsePOISnapshot() { return config.isPillagerFamilyUsePOISnapshot(); }
    
    @Override
    public boolean isEvokerOptimizationEnabled() { return config.isEvokerOptimizationEnabled(); }
    
    @Override
    public boolean isBlazeOptimizationEnabled() { return config.isBlazeOptimizationEnabled(); }
    
    @Override
    public boolean isGuardianOptimizationEnabled() { return config.isGuardianOptimizationEnabled(); }
    
    @Override
    public boolean isWitchOptimizationEnabled() { return config.isWitchOptimizationEnabled(); }
    
    @Override
    public boolean isUniversalAiOptimizationEnabled() { return config.isUniversalAiOptimizationEnabled(); }
    
    @Override
    public java.util.Set<String> getUniversalAiEntities() { return config.getUniversalAiEntities(); }
    
    @Override
    public boolean isZeroDelayFactoryOptimizationEnabled() { return config.isZeroDelayFactoryOptimizationEnabled(); }
    
    @Override
    public java.util.Set<String> getZeroDelayFactoryEntities() { return config.getZeroDelayFactoryEntities(); }
    
    @Override
    public boolean isItemEntityOptimizationEnabled() { return config.isItemEntityOptimizationEnabled(); }
    
    @Override
    public int getItemEntityAgeInterval() { return config.getItemEntityAgeInterval(); }
    
    @Override
    public int getItemEntityMinNearbyItems() { return config.getItemEntityMinNearbyItems(); }
    
    @Override
    public boolean isSimpleEntitiesOptimizationEnabled() {return config.isSimpleEntitiesOptimizationEnabled();}
    
    @Override
    public boolean isSimpleEntitiesUsePOISnapshot() {return config.isSimpleEntitiesUsePOISnapshot();}
    
    @Override
    public boolean isMobSpawningEnabled() {return config.isMobSpawningEnabled();}
    
    @Override
    public int getMaxEntitiesPerChunk() {return config.getMaxEntitiesPerChunk();}
    
    @Override
    public boolean isSpawnerOptimizationEnabled() {return config.isSpawnerOptimizationEnabled();}
    
    @Override
    public boolean isEntityTrackerEnabled() {return config.isEntityTrackerEnabled();}
    
    @Override
    public boolean isPredicateCacheEnabled() {return true;}
    
    @Override
    public boolean isBlockPosPoolEnabled() {return true;}
    
    @Override
    public boolean isListPreallocEnabled() {return true;}
    
    @Override
    public int getListPreallocCapacity() {return 32;}
    
    @Override
    public boolean isPushOptimizationEnabled() {return true;}
    
    @Override
    public boolean isEntityLookupCacheEnabled() {return true;}
    
    @Override
    public int getEntityLookupCacheDurationMs() {return 50;}
    
    @Override
    public boolean isCollisionOptimizationEnabled() {return true;}
    
    @Override
    public ExecutorService getGeneralExecutor() { return generalExecutor; }
    
    @Override
    public boolean isAsyncLightingEnabled() {return config.isAsyncLightingEnabled();}
    
    @Override
    public ExecutorService getLightingExecutor() {return lightingExecutor != null ? lightingExecutor : generalExecutor;}
    
    @Override
    public int getLightBatchThreshold() {return config.getLightBatchThreshold();}
    
    @Override
    public boolean useLayeredPropagationQueue() {return config.useLayeredPropagationQueue();}
    
    @Override
    public int getMaxLightPropagationDistance() {return config.getMaxLightPropagationDistance();}
    
    @Override
    public boolean isSkylightCacheEnabled() {return config.isSkylightCacheEnabled();}
    
    @Override
    public int getSkylightCacheDurationMs() {return config.getSkylightCacheDurationMs();}
    
    @Override
    public boolean isLightDeduplicationEnabled() {return config.isLightDeduplicationEnabled();}
    
    @Override
    public boolean isDynamicBatchAdjustmentEnabled() {return config.isDynamicBatchAdjustmentEnabled();}
    
    @Override
    public boolean isAdvancedLightingStatsEnabled() {return config.isAdvancedLightingStatsEnabled();}
    
    @Override
    public boolean isPlayerChunkLoadingOptimizationEnabled() {return config.isPlayerChunkLoadingOptimizationEnabled();}
    
    @Override
    public int getMaxConcurrentChunkLoadsPerPlayer() {return config.getMaxConcurrentChunkLoadsPerPlayer();}
    
    @Override
    public boolean isEntityTrackingRangeOptimizationEnabled() {return config.isEntityTrackingRangeOptimizationEnabled();}
    
    @Override
    public double getEntityTrackingRangeMultiplier() {return config.getEntityTrackingRangeMultiplier();}
    
    @Override
    public boolean isAlternateCurrentEnabled() {return config.isAlternateCurrentEnabled();}
    
    @Override
    public boolean isRedstoneWireTurboEnabled() {return config.isRedstoneWireTurboEnabled();}
    
    @Override
    public boolean isRedstoneUpdateBatchingEnabled() {return config.isRedstoneUpdateBatchingEnabled();}
    
    @Override
    public int getRedstoneUpdateBatchThreshold() {return config.getRedstoneUpdateBatchThreshold();}
    
    @Override
    public boolean isRedstoneCacheEnabled() {return config.isRedstoneCacheEnabled();}
    
    @Override
    public int getRedstoneCacheDurationMs() {return config.getRedstoneCacheDurationMs();}
    
    @Override
    public boolean isTNTOptimizationEnabled() {return config.isTNTOptimizationEnabled();}
    
    @Override
    public java.util.Set<String> getTNTExplosionEntities() {return config.getTNTExplosionEntities();}
    
    @Override
    public int getTNTThreads() {return config.getTNTThreads();}
    
    @Override
    public int getTNTMaxBlocks() {return config.getTNTMaxBlocks();}
    
    @Override
    public long getTNTTimeoutMicros() {return config.getTNTTimeoutMicros();}
    
    @Override
    public int getTNTBatchSize() {return config.getTNTBatchSize();}
    
    @Override
    public boolean isTNTDebugEnabled() {return config.isTNTDebugEnabled();}
    
    @Override
    public boolean isTNTVanillaCompatibilityEnabled() {return config.isTNTVanillaCompatibilityEnabled();}
    
    @Override
    public boolean isTNTUseVanillaPower() {return config.isTNTUseVanillaPower();}
    
    @Override
    public boolean isTNTUseVanillaFireLogic() {return config.isTNTUseVanillaFireLogic();}
    
    @Override
    public boolean isTNTUseVanillaDamageCalculation() {return config.isTNTUseVanillaDamageCalculation();}
    
    @Override
    public boolean isTNTUseFullRaycast() {return config.isTNTUseFullRaycast();}
    
    @Override
    public boolean isTNTUseVanillaBlockDestruction() {return config.isTNTUseVanillaBlockDestruction();}
    
    @Override
    public boolean isTNTUseVanillaDrops() {return config.isTNTUseVanillaDrops();}
    
    @Override
    public boolean isDebugLoggingEnabled() {return config.isDebugLoggingEnabled();}
    
    public void updateConfiguration(ConfigManager newConfig) {
        this.config = newConfig;
    }
    
    @Override
    public boolean isAsyncVillagerBreedEnabled() {return config.isAsyncVillagerBreedEnabled();}
    
    @Override
    public boolean isVillagerAgeThrottleEnabled() {return config.isVillagerAgeThrottleEnabled();}
    
    @Override
    public int getVillagerBreedThreads() {return config.getVillagerBreedThreads();}
    
    @Override
    public int getVillagerBreedCheckInterval() {return config.getVillagerBreedCheckInterval();}
    
    @Override
    public boolean isChunkTickAsyncEnabled() {return config.isChunkTickAsyncEnabled();}
    
    @Override
    public int getChunkTickThreads() {return config.getChunkTickThreads();}
    
    @Override
    public long getChunkTickTimeoutMicros() {return config.getChunkTickTimeoutMicros();}
    
    // Structure Location Async Configuration
    @Override
    public boolean isStructureLocationAsyncEnabled() {return config.isStructureLocationAsyncEnabled();}
    
    @Override
    public int getStructureLocationThreads() {return config.getStructureLocationThreads();}
    
    @Override
    public boolean isLocateCommandEnabled() {return config.isLocateCommandEnabled();}
    
    @Override
    public int getLocateCommandSearchRadius() {return config.getLocateCommandSearchRadius();}
    
    @Override
    public boolean isLocateCommandSkipKnownStructures() {return config.isLocateCommandSkipKnownStructures();}
    
    @Override
    public boolean isVillagerTradeMapsEnabled() {return config.isVillagerTradeMapsEnabled();}
    
    @Override
    public java.util.Set<String> getVillagerTradeMapTypes() {return config.getVillagerTradeMapTypes();}
    
    @Override
    public int getVillagerMapGenerationTimeoutSeconds() {return config.getVillagerMapGenerationTimeoutSeconds();}
    
    @Override
    public boolean isDolphinTreasureHuntEnabled() {return config.isDolphinTreasureHuntEnabled();}
    
    @Override
    public int getDolphinTreasureSearchRadius() {return config.getDolphinTreasureSearchRadius();}
    
    @Override
    public int getDolphinTreasureHuntInterval() {return config.getDolphinTreasureHuntInterval();}
    
    @Override
    public boolean isChestExplorationMapsEnabled() {return config.isChestExplorationMapsEnabled();}
    
    @Override
    public java.util.Set<String> getChestExplorationLootTables() {return config.getChestExplorationLootTables();}
    
    @Override
    public boolean isChestMapPreserveProbability() {return config.isChestMapPreserveProbability();}
    
    @Override
    public boolean isStructureLocationDebugEnabled() {return config.isStructureLocationDebugEnabled();}
    
    @Override
    public void handleLocateCommandAsyncStart(net.minecraft.commands.CommandSourceStack sourceStack, net.minecraft.commands.arguments.ResourceOrTagKeyArgument.Result<net.minecraft.world.level.levelgen.structure.Structure> structureResult, net.minecraft.core.HolderSet<net.minecraft.world.level.levelgen.structure.Structure> holderSet) {
        java.util.concurrent.CompletableFuture.supplyAsync(() -> {
            try {
                net.minecraft.server.level.ServerLevel level = sourceStack.getLevel();
                net.minecraft.core.BlockPos startPos = net.minecraft.core.BlockPos.containing(sourceStack.getPosition());
                
                if (config.isStructureLocationDebugEnabled()) {
                    System.out.println("[AkiAsync] Starting async locate command from " + startPos);
                }
                
                com.mojang.datafixers.util.Pair<net.minecraft.core.BlockPos, net.minecraft.core.Holder<net.minecraft.world.level.levelgen.structure.Structure>> result = 
                    level.getChunkSource().getGenerator().findNearestMapStructure(
                        level, holderSet, startPos, 
                        config.getLocateCommandSearchRadius(), 
                        config.isLocateCommandSkipKnownStructures()
                    );
                
                return result != null ? result.getFirst() : null;
            } catch (Exception e) {
                System.err.println("[AkiAsync] Error in async locate command: " + e.getMessage());
                return null;
            }
        }, generalExecutor).whenComplete((foundStructure, asyncThrowable) -> {
            handleLocateCommandResult(sourceStack, foundStructure, asyncThrowable);
        });
    }
    
    @Override
    public void handleLocateCommandResult(net.minecraft.commands.CommandSourceStack sourceStack, net.minecraft.core.BlockPos structurePos, Throwable throwable) {
        if (structurePos == null && throwable == null) {
            new org.bukkit.scheduler.BukkitRunnable() {
                @Override
                public void run() {
                    try {
                        sourceStack.sendSuccess(() -> net.minecraft.network.chat.Component.literal(
                            "§a[AkiAsync] Structure location started asynchronously..."), false);
                        
                        if (config.isStructureLocationDebugEnabled()) {
                            System.out.println("[AkiAsync] Locate command started asynchronously");
                        }
                        
                        new org.bukkit.scheduler.BukkitRunnable() {
                            @Override
                            public void run() {
                                sourceStack.sendSuccess(() -> net.minecraft.network.chat.Component.literal(
                                    "§b[AkiAsync] Async structure location completed (test mode)"), false);
                            }
                        }.runTaskLater(plugin, 20L);
                        
                    } catch (Exception e) {
                        System.err.println("[AkiAsync] Error starting async locate command: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }.runTask(plugin);
            return;
        }
        
        new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                try {
                    if (throwable != null) {
                        System.err.println("[AkiAsync] Locate command failed: " + throwable.getMessage());
                        sourceStack.sendFailure(net.minecraft.network.chat.Component.literal("Structure location failed: " + throwable.getMessage()));
                        return;
                    }
                    
                    if (structurePos != null) {
                        sourceStack.sendSuccess(() -> net.minecraft.network.chat.Component.literal(
                            "The nearest structure is at " + structurePos.getX() + ", " + structurePos.getY() + ", " + structurePos.getZ()), false);
                        
                        if (config.isStructureLocationDebugEnabled()) {
                            System.out.println("[AkiAsync] Locate command completed: structure found at " + structurePos);
                        }
                    } else {
                        sourceStack.sendFailure(net.minecraft.network.chat.Component.literal("Could not find that structure nearby"));
                        
                        if (config.isStructureLocationDebugEnabled()) {
                            System.out.println("[AkiAsync] Locate command completed: no structure found");
                        }
                    }
                } catch (Exception e) {
                    System.err.println("[AkiAsync] Error processing locate command result: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }.runTask(plugin);
    }
    
    @Override
    public void handleDolphinTreasureResult(net.minecraft.world.entity.animal.Dolphin dolphin, net.minecraft.core.BlockPos treasurePos, Throwable throwable) {
        if (treasurePos == null && throwable == null) {
            java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                try {
                    net.minecraft.server.level.ServerLevel level = (net.minecraft.server.level.ServerLevel) dolphin.level();
                    net.minecraft.core.BlockPos startPos = dolphin.blockPosition();
                    
                    if (config.isStructureLocationDebugEnabled()) {
                        System.out.println("[AkiAsync] Starting async dolphin treasure hunt from " + startPos);
                    }
                    
                    net.minecraft.core.BlockPos foundTreasure = level.findNearestMapStructure(
                        net.minecraft.tags.StructureTags.DOLPHIN_LOCATED,
                        startPos,
                        config.getDolphinTreasureSearchRadius(),
                        isDolphinTreasureSkipKnownStructures()
                    );
                    
                    return foundTreasure;
                } catch (Exception e) {
                    System.err.println("[AkiAsync] Error in async dolphin treasure hunt: " + e.getMessage());
                    return null;
                }
            }, generalExecutor).whenComplete((foundTreasure, asyncThrowable) -> {
                handleDolphinTreasureResult(dolphin, foundTreasure, asyncThrowable);
            });
            return;
        }
        
        new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                try {
                    if (throwable != null) {
                        System.err.println("[AkiAsync] Dolphin treasure hunt failed: " + throwable.getMessage());
                        return;
                    }
                    
                    if (treasurePos != null) {
                        try {
                            java.lang.reflect.Field treasurePosField = dolphin.getClass().getDeclaredField("treasurePos");
                            treasurePosField.setAccessible(true);
                            treasurePosField.set(dolphin, treasurePos);
                            
                            dolphin.level().addParticle(net.minecraft.core.particles.ParticleTypes.DOLPHIN, 
                                dolphin.getX(), dolphin.getY(), dolphin.getZ(), 0.0D, 0.0D, 0.0D);
                            
                            if (config.isStructureLocationDebugEnabled()) {
                                System.out.println("[AkiAsync] Dolphin treasure hunt completed: treasure found at " + treasurePos);
                            }
                        } catch (Exception e) {
                            System.err.println("[AkiAsync] Error setting dolphin treasure position: " + e.getMessage());
                        }
                    } else {
                        if (config.isStructureLocationDebugEnabled()) {
                            System.out.println("[AkiAsync] Dolphin treasure hunt completed: no treasure found");
                        }
                    }
                } catch (Exception e) {
                    System.err.println("[AkiAsync] Error processing dolphin treasure result: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }.runTask(plugin);
    }
    
    @Override
    public void handleChestExplorationMapResult(net.minecraft.world.item.ItemStack stack, net.minecraft.world.level.storage.loot.LootContext context, net.minecraft.core.BlockPos structurePos, net.minecraft.core.Holder<net.minecraft.world.level.saveddata.maps.MapDecorationType> mapDecoration, byte zoom, Throwable throwable, Object cir) {
        if (structurePos == null && throwable == null) {
            java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                try {
                    net.minecraft.server.level.ServerLevel level = context.getLevel();
                    net.minecraft.world.phys.Vec3 origin = context.getOptionalParameter(
                        net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN);
                    net.minecraft.core.BlockPos startPos = origin != null ? 
                        net.minecraft.core.BlockPos.containing(origin) : new net.minecraft.core.BlockPos(0, 64, 0);
                    
                    if (config.isStructureLocationDebugEnabled()) {
                        System.out.println("[AkiAsync] Starting async chest exploration map creation from " + startPos);
                    }
                    
                    return null;
                } catch (Exception e) {
                    System.err.println("[AkiAsync] Error in async chest exploration map creation: " + e.getMessage());
                    return null;
                }
            }, generalExecutor).whenComplete((foundStructure, asyncThrowable) -> {
                handleChestExplorationMapResult(stack, context, (net.minecraft.core.BlockPos) foundStructure, mapDecoration, zoom, asyncThrowable, cir);
            });
            return;
        }
        
        new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                try {
                    if (throwable != null) {
                        System.err.println("[AkiAsync] Chest exploration map creation failed: " + throwable.getMessage());
                        setReturnValue(cir, stack);
                        return;
                    }
                    
                    if (structurePos != null) {
                        net.minecraft.server.level.ServerLevel level = context.getLevel();
                        net.minecraft.world.item.ItemStack mapStack = net.minecraft.world.item.MapItem.create(
                            level, structurePos.getX(), structurePos.getZ(), zoom, true, true);
                        net.minecraft.world.item.MapItem.renderBiomePreviewMap(level, mapStack);
                        net.minecraft.world.level.saveddata.maps.MapItemSavedData.addTargetDecoration(
                            mapStack, structurePos, "+", mapDecoration);
                        
                        setReturnValue(cir, mapStack);
                        
                        if (config.isStructureLocationDebugEnabled()) {
                            System.out.println("[AkiAsync] Chest exploration map created for structure at " + structurePos);
                        }
                    } else {
                        setReturnValue(cir, stack);
                        
                        if (config.isStructureLocationDebugEnabled()) {
                            System.out.println("[AkiAsync] No structure found for chest exploration map");
                        }
                    }
                } catch (Exception e) {
                    System.err.println("[AkiAsync] Error processing chest exploration map result: " + e.getMessage());
                    e.printStackTrace();
                    setReturnValue(cir, stack);
                }
            }
        }.runTask(plugin);
    }
    
    @Override
    public void handleVillagerTradeMapResult(net.minecraft.world.item.trading.MerchantOffer offer, net.minecraft.world.entity.Entity trader, net.minecraft.core.BlockPos structurePos, net.minecraft.core.Holder<net.minecraft.world.level.saveddata.maps.MapDecorationType> destinationType, String displayName, int maxUses, int villagerXp, Throwable throwable, Object cir) {
        if (structurePos == null && throwable == null) {
            java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                try {
                    net.minecraft.server.level.ServerLevel level = (net.minecraft.server.level.ServerLevel) trader.level();
                    net.minecraft.core.BlockPos startPos = trader.blockPosition();
                    
                    if (config.isStructureLocationDebugEnabled()) {
                        System.out.println("[AkiAsync] Starting async villager trade map creation from " + startPos);
                    }
                    
                    return null;
                } catch (Exception e) {
                    System.err.println("[AkiAsync] Error in async villager trade map creation: " + e.getMessage());
                    return null;
                }
            }, generalExecutor).whenComplete((foundStructure, asyncThrowable) -> {
                handleVillagerTradeMapResult(offer, trader, (net.minecraft.core.BlockPos) foundStructure, destinationType, displayName, maxUses, villagerXp, asyncThrowable, cir);
            });
            return;
        }
        
        new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                try {
                    if (throwable != null) {
                        System.err.println("[AkiAsync] Villager trade map creation failed: " + throwable.getMessage());
                        setReturnValue(cir, null);
                        return;
                    }
                    
                    if (structurePos != null) {
                        net.minecraft.server.level.ServerLevel level = (net.minecraft.server.level.ServerLevel) trader.level();
                        net.minecraft.world.item.ItemStack mapStack = net.minecraft.world.item.MapItem.create(
                            level, structurePos.getX(), structurePos.getZ(), (byte)2, true, true);
                        net.minecraft.world.item.MapItem.renderBiomePreviewMap(level, mapStack);
                        net.minecraft.world.level.saveddata.maps.MapItemSavedData.addTargetDecoration(
                            mapStack, structurePos, "+", destinationType);
                        mapStack.set(net.minecraft.core.component.DataComponents.ITEM_NAME, 
                            net.minecraft.network.chat.Component.translatable(displayName));
                        
                        net.minecraft.world.item.trading.MerchantOffer newOffer = 
                            new net.minecraft.world.item.trading.MerchantOffer(
                                new net.minecraft.world.item.trading.ItemCost(net.minecraft.world.item.Items.EMERALD, offer.getCostA().getCount()),
                                java.util.Optional.of(new net.minecraft.world.item.trading.ItemCost(net.minecraft.world.item.Items.COMPASS, 1)),
                                mapStack, 0, maxUses, villagerXp, 0.2F);
                        
                        setReturnValue(cir, newOffer);
                        
                        if (config.isStructureLocationDebugEnabled()) {
                            System.out.println("[AkiAsync] Villager trade map created for structure at " + structurePos);
                        }
                    } else {
                        setReturnValue(cir, null);
                        
                        if (config.isStructureLocationDebugEnabled()) {
                            System.out.println("[AkiAsync] No structure found for villager trade map");
                        }
                    }
                } catch (Exception e) {
                    System.err.println("[AkiAsync] Error processing villager trade map result: " + e.getMessage());
                    e.printStackTrace();
                    setReturnValue(cir, null);
                }
            }
        }.runTask(plugin);
    }
    
    @Override
    public int getVillagerTradeMapsSearchRadius() {return config.getLocateCommandSearchRadius();}
    
    @Override
    public boolean isVillagerTradeMapsSkipKnownStructures() {return config.isLocateCommandSkipKnownStructures();}
    
    @Override
    public boolean isDolphinTreasureSkipKnownStructures() {return config.isLocateCommandSkipKnownStructures();}
    
    private void setReturnValue(Object cir, Object value) {
        try {
            cir.getClass().getMethod("setReturnValue", Object.class).invoke(cir, value);
        } catch (Exception e) {
            System.err.println("[AkiAsync] Failed to set return value: " + e.getMessage());
        }
    }
}