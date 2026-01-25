package org.virgil.akiasync.bridge.delegates;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.virgil.akiasync.AkiAsyncPlugin;
import org.virgil.akiasync.compat.FoliaEntityAdapter;
import org.virgil.akiasync.compat.FoliaSchedulerAdapter;
import org.virgil.akiasync.config.ConfigManager;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Delegate class handling structure location related bridge methods.
 * Extracted from AkiAsyncBridge to reduce its complexity.
 */
public class StructureBridgeDelegate {

    private final AkiAsyncPlugin plugin;
    private ConfigManager config;
    private final ExecutorService generalExecutor;

    public StructureBridgeDelegate(AkiAsyncPlugin plugin, ConfigManager config, ExecutorService generalExecutor) {
        this.plugin = plugin;
        this.config = config;
        this.generalExecutor = generalExecutor;
    }

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "ConfigManager is intentionally shared")
    public void updateConfig(ConfigManager newConfig) {
        this.config = newConfig;
    }

    public void handleLocateCommandAsyncStart(net.minecraft.commands.CommandSourceStack sourceStack, net.minecraft.commands.arguments.ResourceOrTagKeyArgument.Result<net.minecraft.world.level.levelgen.structure.Structure> structureResult, net.minecraft.core.HolderSet<net.minecraft.world.level.levelgen.structure.Structure> holderSet) {
        CompletableFuture.supplyAsync(() -> {
            try {
                net.minecraft.server.level.ServerLevel level = sourceStack.getLevel();
                net.minecraft.core.BlockPos startPos = net.minecraft.core.BlockPos.containing(sourceStack.getPosition());

                if (config.isStructureLocationDebugEnabled()) {
                    org.virgil.akiasync.util.DebugLogger.debug("[AkiAsync] Starting async locate command from " + startPos);
                }

                com.mojang.datafixers.util.Pair<net.minecraft.core.BlockPos, net.minecraft.core.Holder<net.minecraft.world.level.levelgen.structure.Structure>> result;

                if (config.isStructureAlgorithmOptimizationEnabled()) {
                    if (config.isStructureLocationDebugEnabled()) {
                        org.virgil.akiasync.util.DebugLogger.debug("[AkiAsync] Using optimized structure search algorithm");
                    }
                    result = org.virgil.akiasync.mixin.async.structure.OptimizedStructureLocator.findNearestStructureOptimized(
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
                org.virgil.akiasync.util.DebugLogger.error("[AkiAsync] Error in async locate command: " + e.getMessage());
                return null;
            }
        }, generalExecutor).whenComplete((foundStructure, asyncThrowable) -> {
            handleLocateCommandResult(sourceStack, foundStructure, asyncThrowable);
        });
    }

    public void handleLocateCommandResult(net.minecraft.commands.CommandSourceStack sourceStack, net.minecraft.core.BlockPos structurePos, Throwable throwable) {
        if (structurePos == null && throwable == null) {
            FoliaSchedulerAdapter.runTask(plugin, () -> {
                try {
                    sourceStack.sendSuccess(() -> net.minecraft.network.chat.Component.literal(
                        "§a[AkiAsync] Structure location started asynchronously..."), false);

                    if (config.isStructureLocationDebugEnabled()) {
                        org.virgil.akiasync.util.DebugLogger.debug("[AkiAsync] Locate command started asynchronously");
                    }

                    FoliaSchedulerAdapter.runTaskLater(plugin, () -> {
                        sourceStack.sendSuccess(() -> net.minecraft.network.chat.Component.literal(
                            "§b[AkiAsync] Async structure location completed (test mode)"), false);
                    }, 20L);

                } catch (Exception e) {
                    org.virgil.akiasync.util.DebugLogger.error("[AkiAsync] Error starting async locate command: " + e.getMessage());
                    e.printStackTrace();
                }
            });
            return;
        }

        FoliaSchedulerAdapter.runTask(plugin, () -> {
            try {
                if (throwable != null) {
                    org.virgil.akiasync.util.DebugLogger.error("[AkiAsync] Locate command failed: " + throwable.getMessage());
                    sourceStack.sendFailure(net.minecraft.network.chat.Component.literal("Structure location failed: " + throwable.getMessage()));
                    return;
                }

                if (structurePos != null) {
                    sourceStack.sendSuccess(() -> net.minecraft.network.chat.Component.literal(
                        "The nearest structure is at " + structurePos.getX() + ", " + structurePos.getY() + ", " + structurePos.getZ()), false);

                    if (config.isStructureLocationDebugEnabled()) {
                        org.virgil.akiasync.util.DebugLogger.debug("[AkiAsync] Locate command completed: structure found at " + structurePos);
                    }
                } else {
                    sourceStack.sendFailure(net.minecraft.network.chat.Component.literal("Could not find that structure nearby"));

                    if (config.isStructureLocationDebugEnabled()) {
                        org.virgil.akiasync.util.DebugLogger.debug("[AkiAsync] Locate command completed: no structure found");
                    }
                }
            } catch (Exception e) {
                org.virgil.akiasync.util.DebugLogger.error("[AkiAsync] Error processing locate command result: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    public void handleDolphinTreasureResult(net.minecraft.world.entity.animal.Dolphin dolphin, net.minecraft.core.BlockPos treasurePos, Throwable throwable) {
        if (treasurePos == null && throwable == null) {
            CompletableFuture.supplyAsync(() -> {
                try {
                    net.minecraft.server.level.ServerLevel level = (net.minecraft.server.level.ServerLevel) dolphin.level();
                    net.minecraft.core.BlockPos startPos = dolphin.blockPosition();

                    if (config.isStructureLocationDebugEnabled()) {
                        org.virgil.akiasync.util.DebugLogger.debug("[AkiAsync] Starting async dolphin treasure hunt from " + startPos);
                    }

                    net.minecraft.core.BlockPos foundTreasure = level.findNearestMapStructure(
                        net.minecraft.tags.StructureTags.DOLPHIN_LOCATED,
                        startPos,
                        config.getDolphinTreasureSearchRadius(),
                        config.isLocateCommandSkipKnownStructures()
                    );

                    return foundTreasure;
                } catch (Exception e) {
                    org.virgil.akiasync.util.DebugLogger.error("[AkiAsync] Error in async dolphin treasure hunt: " + e.getMessage());
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
                    org.virgil.akiasync.util.DebugLogger.error("[AkiAsync] Dolphin treasure hunt failed: " + throwable.getMessage());
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
                            org.virgil.akiasync.util.DebugLogger.debug("[AkiAsync] Dolphin treasure hunt completed: treasure found at " + treasurePos);
                        }
                    } catch (Exception e) {
                        org.virgil.akiasync.util.DebugLogger.error("[AkiAsync] Error setting dolphin treasure position: " + e.getMessage());
                    }
                } else {
                    if (config.isStructureLocationDebugEnabled()) {
                        org.virgil.akiasync.util.DebugLogger.debug("[AkiAsync] Dolphin treasure hunt completed: no treasure found");
                    }
                }
            } catch (Exception e) {
                org.virgil.akiasync.util.DebugLogger.error("[AkiAsync] Error processing dolphin treasure result: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    public void handleChestExplorationMapAsyncStart(net.minecraft.world.item.ItemStack stack, net.minecraft.world.level.storage.loot.LootContext context, net.minecraft.tags.TagKey<net.minecraft.world.level.levelgen.structure.Structure> destination, net.minecraft.core.Holder<net.minecraft.world.level.saveddata.maps.MapDecorationType> mapDecoration, byte zoom, int searchRadius, boolean skipKnownStructures, Object cir) {
        CompletableFuture.supplyAsync(() -> {
            try {
                net.minecraft.server.level.ServerLevel level = context.getLevel();
                net.minecraft.world.phys.Vec3 origin = context.getOptionalParameter(
                    net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN);
                net.minecraft.core.BlockPos startPos = origin != null ?
                    net.minecraft.core.BlockPos.containing(origin) : new net.minecraft.core.BlockPos(0, 64, 0);

                if (config.isStructureLocationDebugEnabled()) {
                    org.virgil.akiasync.util.DebugLogger.debug("[AkiAsync] Starting async chest exploration map creation from " + startPos + " for " + destination.location());
                }

                net.minecraft.core.BlockPos foundStructure = level.findNearestMapStructure(
                    destination, startPos, searchRadius, skipKnownStructures);

                if (config.isStructureLocationDebugEnabled()) {
                    if (foundStructure != null) {
                        org.virgil.akiasync.util.DebugLogger.debug("[AkiAsync] Chest exploration map: structure found at " + foundStructure);
                    } else {
                        org.virgil.akiasync.util.DebugLogger.debug("[AkiAsync] Chest exploration map: no structure found");
                    }
                }

                return foundStructure;
            } catch (Exception e) {
                org.virgil.akiasync.util.DebugLogger.error("[AkiAsync] Error in async chest exploration map creation: " + e.getMessage());
                e.printStackTrace();
                return null;
            }
        }, generalExecutor).whenComplete((foundStructure, asyncThrowable) -> {
            handleChestExplorationMapResult(stack, context, foundStructure, mapDecoration, zoom, asyncThrowable, cir);
        });
    }

    public void handleChestExplorationMapResult(net.minecraft.world.item.ItemStack stack, net.minecraft.world.level.storage.loot.LootContext context, net.minecraft.core.BlockPos structurePos, net.minecraft.core.Holder<net.minecraft.world.level.saveddata.maps.MapDecorationType> mapDecoration, byte zoom, Throwable throwable, Object cir) {

        FoliaSchedulerAdapter.runTask(plugin, () -> {
            try {
                if (throwable != null) {
                    org.virgil.akiasync.util.DebugLogger.error("[AkiAsync] Chest exploration map creation failed: " + throwable.getMessage());
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
                        org.virgil.akiasync.util.DebugLogger.debug("[AkiAsync] Chest exploration map created for structure at " + structurePos);
                    }
                } else {
                    setReturnValue(cir, stack);

                    if (config.isStructureLocationDebugEnabled()) {
                        org.virgil.akiasync.util.DebugLogger.debug("[AkiAsync] No structure found for chest exploration map");
                    }
                }
            } catch (Exception e) {
                org.virgil.akiasync.util.DebugLogger.error("[AkiAsync] Error processing chest exploration map result: " + e.getMessage());
                e.printStackTrace();
                setReturnValue(cir, stack);
            }
        });
    }

    public void handleVillagerTradeMapAsyncStart(net.minecraft.world.item.trading.MerchantOffer offer, net.minecraft.world.entity.Entity trader, net.minecraft.tags.TagKey<net.minecraft.world.level.levelgen.structure.Structure> destination, net.minecraft.core.Holder<net.minecraft.world.level.saveddata.maps.MapDecorationType> destinationType, String displayName, int maxUses, int villagerXp, Object cir) {
        CompletableFuture.supplyAsync(() -> {
            try {
                net.minecraft.server.level.ServerLevel level = (net.minecraft.server.level.ServerLevel) trader.level();
                net.minecraft.core.BlockPos startPos = trader.blockPosition();

                if (config.isStructureLocationDebugEnabled()) {
                    org.virgil.akiasync.util.DebugLogger.debug("[AkiAsync] Starting async villager trade map creation from " + startPos + " for " + destination.location());
                }

                int searchRadius = config.getVillagerTradeMapsSearchRadius();
                boolean skipKnown = config.isVillagerTradeMapsSkipKnownStructures();

                net.minecraft.core.BlockPos foundStructure = level.findNearestMapStructure(
                    destination, startPos, searchRadius, skipKnown);

                if (config.isStructureLocationDebugEnabled()) {
                    if (foundStructure != null) {
                        org.virgil.akiasync.util.DebugLogger.debug("[AkiAsync] Villager trade map: structure found at " + foundStructure);
                    } else {
                        org.virgil.akiasync.util.DebugLogger.debug("[AkiAsync] Villager trade map: no structure found");
                    }
                }

                return foundStructure;
            } catch (Exception e) {
                org.virgil.akiasync.util.DebugLogger.error("[AkiAsync] Error in async villager trade map creation: " + e.getMessage());
                e.printStackTrace();
                return null;
            }
        }, generalExecutor).whenComplete((foundStructure, asyncThrowable) -> {
            handleVillagerTradeMapResult(offer, trader, foundStructure, destinationType, displayName, maxUses, villagerXp, asyncThrowable, cir);
        });
    }

    public void handleVillagerTradeMapResult(net.minecraft.world.item.trading.MerchantOffer offer, net.minecraft.world.entity.Entity trader, net.minecraft.core.BlockPos structurePos, net.minecraft.core.Holder<net.minecraft.world.level.saveddata.maps.MapDecorationType> destinationType, String displayName, int maxUses, int villagerXp, Throwable throwable, Object cir) {

        FoliaEntityAdapter.safeEntityOperation(plugin, (org.bukkit.entity.Entity) trader.getBukkitEntity(), (entity) -> {
            try {
                if (throwable != null) {
                    org.virgil.akiasync.util.DebugLogger.error("[AkiAsync] Villager trade map creation failed: " + throwable.getMessage());
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
                        org.virgil.akiasync.util.DebugLogger.debug("[AkiAsync] Villager trade map created for structure at " + structurePos);
                    }
                } else {
                    setReturnValue(cir, null);

                    if (config.isStructureLocationDebugEnabled()) {
                        org.virgil.akiasync.util.DebugLogger.debug("[AkiAsync] No structure found for villager trade map");
                    }
                }
            } catch (Exception e) {
                org.virgil.akiasync.util.DebugLogger.error("[AkiAsync] Error processing villager trade map result: " + e.getMessage());
                e.printStackTrace();
                setReturnValue(cir, null);
            }
        });
    }

    private void setReturnValue(Object cir, Object value) {
        try {
            cir.getClass().getMethod("setReturnValue", Object.class).invoke(cir, value);
        } catch (Exception e) {
            org.virgil.akiasync.util.DebugLogger.error("[AkiAsync] Failed to set return value: " + e.getMessage());
        }
    }
}
