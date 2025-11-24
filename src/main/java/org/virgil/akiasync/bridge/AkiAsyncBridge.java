package org.virgil.akiasync.bridge;

import java.util.concurrent.ExecutorService;

import org.virgil.akiasync.AkiAsyncPlugin;
import org.virgil.akiasync.config.ConfigManager;
import org.virgil.akiasync.compat.FoliaSchedulerAdapter;
import org.virgil.akiasync.compat.FoliaEntityAdapter;

public class AkiAsyncBridge implements org.virgil.akiasync.mixin.bridge.Bridge {
    
    private final AkiAsyncPlugin plugin;
    private ConfigManager config;
    private final ExecutorService generalExecutor;
    private final ExecutorService lightingExecutor;
    private final ExecutorService tntExecutor;
    private final ExecutorService chunkTickExecutor;
    private final ExecutorService villagerBreedExecutor;
    private final ExecutorService brainExecutor;
    
    public AkiAsyncBridge(AkiAsyncPlugin plugin, ExecutorService generalExecutor, ExecutorService lightingExecutor, ExecutorService tntExecutor, ExecutorService chunkTickExecutor, ExecutorService villagerBreedExecutor, ExecutorService brainExecutor) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        this.generalExecutor = generalExecutor;
        this.lightingExecutor = lightingExecutor;
        this.tntExecutor = tntExecutor;
        this.chunkTickExecutor = chunkTickExecutor;
        this.villagerBreedExecutor = villagerBreedExecutor;
        this.brainExecutor = brainExecutor;
    }
    
    @Override
    public boolean isNitoriOptimizationsEnabled() { 
        return config != null ? config.isNitoriOptimizationsEnabled() : true; 
    }
    
    @Override
    public boolean isVirtualThreadEnabled() { 
        return config != null ? config.isVirtualThreadEnabled() : true; 
    }
    
    @Override
    public boolean isWorkStealingEnabled() { 
        return config != null ? config.isWorkStealingEnabled() : true; 
    }
    
    @Override
    public boolean isBlockPosCacheEnabled() { 
        return config != null ? config.isBlockPosCacheEnabled() : true; 
    }
    
    @Override
    public boolean isOptimizedCollectionsEnabled() { 
        return config != null ? config.isOptimizedCollectionsEnabled() : true; 
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
    public boolean isBlockEntityParallelTickEnabled() { return config.isBlockEntityParallelTickEnabled(); }
    
    @Override
    public int getBlockEntityParallelMinBlockEntities() { return config.getBlockEntityParallelMinBlockEntities(); }
    
    @Override
    public int getBlockEntityParallelBatchSize() { return config.getBlockEntityParallelBatchSize(); }
    
    @Override
    public boolean isBlockEntityParallelProtectContainers() { return config.isBlockEntityParallelProtectContainers(); }
    
    @Override
    public int getBlockEntityParallelTimeoutMs() { return config.getBlockEntityParallelTimeoutMs(); }
    
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
    public int getEntityTrackerQueueSize() {return config.getMaxQueueSize();}
    
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
    public ExecutorService getTNTExecutor() { return tntExecutor != null ? tntExecutor : generalExecutor; }
    
    @Override
    public ExecutorService getChunkTickExecutor() { return chunkTickExecutor != null ? chunkTickExecutor : generalExecutor; }
    
    @Override
    public ExecutorService getVillagerBreedExecutor() { return villagerBreedExecutor != null ? villagerBreedExecutor : generalExecutor; }
    
    @Override
    public ExecutorService getBrainExecutor() { return brainExecutor != null ? brainExecutor : generalExecutor; }
    
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
    public boolean isBeeFixEnabled() {return config.isBeeFixEnabled();}
    
    @Override
    public boolean isTNTUseFullRaycast() {return config.isTNTUseFullRaycast();}
    
    @Override
    public boolean isTNTUseVanillaBlockDestruction() {return config.isTNTUseVanillaBlockDestruction();}
    
    @Override
    public boolean isTNTUseVanillaDrops() {return config.isTNTUseVanillaDrops();}
    
    @Override
    public boolean isDebugLoggingEnabled() {return config.isDebugLoggingEnabled();}
    
    @Override
    public boolean isFoliaEnvironment() {return org.virgil.akiasync.util.FoliaUtils.isFoliaEnvironment();}
    
    @Override
    public boolean isOwnedByCurrentRegion(net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos pos) {
        net.minecraft.world.phys.Vec3 vec = net.minecraft.world.phys.Vec3.atCenterOf(pos);
        return org.virgil.akiasync.util.FoliaUtils.isOwnedByCurrentRegion(level, vec);
    }
    
    @Override
    public void scheduleRegionTask(net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos pos, Runnable task) {
        net.minecraft.world.phys.Vec3 vec = net.minecraft.world.phys.Vec3.atCenterOf(pos);
        org.virgil.akiasync.util.FoliaUtils.scheduleRegionTask(level, vec, task);
    }
    
    @Override
    public boolean canAccessEntityDirectly(net.minecraft.world.entity.Entity entity) {
        return org.virgil.akiasync.compat.FoliaRegionContext.canAccessEntityDirectly(entity);
    }
    
    @Override
    public boolean canAccessBlockPosDirectly(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos) {
        return org.virgil.akiasync.compat.FoliaRegionContext.canAccessBlockPosDirectly(level, pos);
    }
    
    @Override
    public void safeExecute(Runnable task, String context) {
        org.virgil.akiasync.util.ExceptionHandler.safeExecute(task, context);
    }
    
    @Override
    public String checkExecutorHealth(java.util.concurrent.ExecutorService executor, String name) {
        org.virgil.akiasync.util.ExecutorHealthChecker.HealthStatus status = 
            org.virgil.akiasync.util.ExecutorHealthChecker.checkHealth(executor, name);
        return status.toString();
    }
    
    @Override
    public String getBlockId(net.minecraft.world.level.block.Block block) {
        try {
            net.minecraft.resources.ResourceLocation key = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block);
            return key != null ? key.toString() : "unknown";
        } catch (Exception e) {
            return block.getClass().getSimpleName().toLowerCase();
        }
    }
    
    public void updateConfiguration(ConfigManager newConfig) {
        this.config = newConfig;
        org.virgil.akiasync.util.DebugLogger.updateDebugState(newConfig.isDebugLoggingEnabled());
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
    public boolean isStructureAlgorithmOptimizationEnabled() {return config.isStructureAlgorithmOptimizationEnabled();}
    
    @Override
    public String getStructureSearchPattern() {return config.getStructureSearchPattern();}
    
    @Override
    public boolean isStructureCachingEnabled() {return config.isStructureCachingEnabled();}
    
    @Override
    public boolean isStructurePrecomputationEnabled() {return config.isStructurePrecomputationEnabled();}
    
    @Override
    public boolean isBiomeAwareSearchEnabled() {return config.isBiomeAwareSearchEnabled();}
    
    @Override
    public int getStructureCacheMaxSize() {return config.getStructureCacheMaxSize();}
    
    @Override
    public long getStructureCacheExpirationMinutes() {return config.getStructureCacheExpirationMinutes();}
    
    @Override
    public boolean isDataPackOptimizationEnabled() {return config.isDataPackOptimizationEnabled();}
    
    @Override
    public int getDataPackFileLoadThreads() {return config.getDataPackFileLoadThreads();}
    
    @Override
    public int getDataPackZipProcessThreads() {return config.getDataPackZipProcessThreads();}
    
    @Override
    public int getDataPackBatchSize() {return config.getDataPackBatchSize();}
    
    @Override
    public long getDataPackCacheExpirationMinutes() {return config.getDataPackCacheExpirationMinutes();}
    
    @Override
    public boolean isDataPackDebugEnabled() {return config.isDataPackDebugEnabled();}
    
    @Override
    public void handleLocateCommandAsyncStart(net.minecraft.commands.CommandSourceStack sourceStack, net.minecraft.commands.arguments.ResourceOrTagKeyArgument.Result<net.minecraft.world.level.levelgen.structure.Structure> structureResult, net.minecraft.core.HolderSet<net.minecraft.world.level.levelgen.structure.Structure> holderSet) {
        java.util.concurrent.CompletableFuture.supplyAsync(() -> {
            try {
                net.minecraft.server.level.ServerLevel level = sourceStack.getLevel();
                net.minecraft.core.BlockPos startPos = net.minecraft.core.BlockPos.containing(sourceStack.getPosition());
                
                if (config.isStructureLocationDebugEnabled()) {
                    System.out.println("[AkiAsync] Starting async locate command from " + startPos);
                }
                
                com.mojang.datafixers.util.Pair<net.minecraft.core.BlockPos, net.minecraft.core.Holder<net.minecraft.world.level.levelgen.structure.Structure>> result;
                
                if (config.isStructureAlgorithmOptimizationEnabled()) {
                    if (config.isStructureLocationDebugEnabled()) {
                        System.out.println("[AkiAsync] Using optimized structure search algorithm");
                    }
                    result = org.virgil.akiasync.async.structure.OptimizedStructureLocator.findNearestStructureOptimized(
                        level, holderSet, startPos,
                        config.getLocateCommandSearchRadius(),
                        config.isLocateCommandSkipKnownStructures()
                    );
                } else {
                    result = level.getChunkSource().getGenerator().findNearestMapStructure(
                        level, holderSet, startPos, 
                        config.getLocateCommandSearchRadius(), 
                        config.isLocateCommandSkipKnownStructures()
                    );
                }
                
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
            FoliaSchedulerAdapter.runTask(plugin, () -> {
                try {
                    sourceStack.sendSuccess(() -> net.minecraft.network.chat.Component.literal(
                        "§a[AkiAsync] Structure location started asynchronously..."), false);
                    
                    if (config.isStructureLocationDebugEnabled()) {
                        System.out.println("[AkiAsync] Locate command started asynchronously");
                    }
                    
                    FoliaSchedulerAdapter.runTaskLater(plugin, () -> {
                        sourceStack.sendSuccess(() -> net.minecraft.network.chat.Component.literal(
                            "§b[AkiAsync] Async structure location completed (test mode)"), false);
                    }, 20L);
                    
                } catch (Exception e) {
                    System.err.println("[AkiAsync] Error starting async locate command: " + e.getMessage());
                    e.printStackTrace();
                }
            });
            return;
        }
        
        FoliaSchedulerAdapter.runTask(plugin, () -> {
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
        });
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
        
        FoliaEntityAdapter.safeEntityOperation(plugin, (org.bukkit.entity.Entity) dolphin.getBukkitEntity(), (entity) -> {
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
        });
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
        
        FoliaSchedulerAdapter.runTask(plugin, () -> {
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
        });
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
        
        FoliaEntityAdapter.safeEntityOperation(plugin, (org.bukkit.entity.Entity) trader.getBukkitEntity(), (entity) -> {
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
        });
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
    
    @Override
    public void debugLog(String message) {
        org.virgil.akiasync.util.DebugLogger.debug(message);
    }
    
    @Override
    public void debugLog(String format, Object... args) {
        org.virgil.akiasync.util.DebugLogger.debug(format, args);
    }
    
    @Override
    public void errorLog(String message) {
        org.virgil.akiasync.util.DebugLogger.error(message);
    }
    
    @Override
    public void errorLog(String format, Object... args) {
        org.virgil.akiasync.util.DebugLogger.error(format, args);
    }
    
    @Override
    public boolean isVirtualEntity(net.minecraft.world.entity.Entity entity) {
        return org.virgil.akiasync.util.VirtualEntityDetector.isVirtualEntity(entity);
    }
    
    @Override
    public boolean isSecureSeedEnabled() {
        return config != null && config.isSecureSeedEnabled();
    }
    
    @Override
    public boolean isSecureSeedProtectStructures() {
        return config != null && config.isSecureSeedProtectStructures();
    }
    
    @Override
    public boolean isSecureSeedProtectOres() {
        return config != null && config.isSecureSeedProtectOres();
    }
    
    @Override
    public boolean isSecureSeedProtectSlimes() {
        return config != null && config.isSecureSeedProtectSlimes();
    }
    
    @Override
    public int getSecureSeedBits() {
        return config != null ? config.getSecureSeedBits() : 1024;
    }
    
    @Override
    public boolean isSecureSeedDebugLogging() {
        return config != null && config.isSecureSeedDebugLogging();
    }
}