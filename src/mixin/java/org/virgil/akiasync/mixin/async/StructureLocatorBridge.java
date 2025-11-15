package org.virgil.akiasync.mixin.async;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.ResourceOrTagKeyArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Dolphin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;
import net.minecraft.world.level.storage.loot.LootContext;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class StructureLocatorBridge {
    
    private static ExecutorService executorService;
    private static final AtomicInteger ACTIVE_TASKS = new AtomicInteger(0);
    private static boolean initialized = false;
    
    public static void initialize() {
        org.virgil.akiasync.mixin.bridge.Bridge bridge = 
            org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        if (bridge != null && bridge.isStructureLocationAsyncEnabled()) {
            int threads = bridge.getStructureLocationThreads();
            executorService = Executors.newFixedThreadPool(threads, r -> {
                Thread t = new Thread(r, "AkiAsync-StructureLocator-" + System.currentTimeMillis());
                t.setDaemon(true);
                return t;
            });
            initialized = true;
            
            if (bridge.isStructureLocationDebugEnabled()) {
                System.out.println("[AkiAsync] StructureLocatorBridge initialized with " + threads + " threads");
            }
        }
    }
    
    public static void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            System.out.println("[AkiAsync] StructureLocatorBridge shutdown. Active tasks: " + ACTIVE_TASKS.get());
        }
        initialized = false;
    }
    
    public static void locateStructureAsync(
        CommandSourceStack sourceStack, 
        ResourceOrTagKeyArgument.Result<Structure> structureResult,
        HolderSet<Structure> holderSet
    ) {
        org.virgil.akiasync.mixin.bridge.Bridge bridge = 
            org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        if (bridge != null) {
            bridge.handleLocateCommandAsyncStart(sourceStack, structureResult, holderSet);
        }
    }
    
    public static void findDolphinTreasureAsync(Dolphin dolphin) {
        org.virgil.akiasync.mixin.bridge.Bridge bridge = 
            org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        if (bridge != null) {
            bridge.handleDolphinTreasureResult(dolphin, null, null);
        }
    }
    
    public static void createChestExplorationMapAsync(
        ItemStack stack, 
        LootContext context,
        TagKey<Structure> destination,
        Holder<MapDecorationType> mapDecoration,
        byte zoom,
        int searchRadius,
        boolean skipKnownStructures,
        CallbackInfoReturnable<ItemStack> cir
    ) {
        org.virgil.akiasync.mixin.bridge.Bridge bridge = 
            org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        if (bridge != null) {
            bridge.handleChestExplorationMapResult(stack, context, null, mapDecoration, zoom, null, cir);
        }
    }
    
    public static void createExplorerMapAsync(
        MerchantOffer offer,
        Entity trader,
        RandomSource random,
        TagKey<Structure> destination,
        String displayName,
        Holder<MapDecorationType> destinationType,
        int maxUses,
        int villagerXp,
        CallbackInfoReturnable<MerchantOffer> cir
    ) {
        org.virgil.akiasync.mixin.bridge.Bridge bridge = 
            org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        if (bridge != null) {
            bridge.handleVillagerTradeMapResult(offer, trader, null, destinationType, displayName, maxUses, villagerXp, null, cir);
        }
    }
    
    public static int getActiveTasks() {
        return ACTIVE_TASKS.get();
    }
}
