package org.virgil.akiasync.mixin.mixins.structure;
import java.util.concurrent.CompletableFuture;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.Holder;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;
import net.minecraft.world.level.storage.loot.functions.ExplorationMapFunction;
import org.virgil.akiasync.mixin.util.BridgeConfigCache;

@SuppressWarnings("unused")
@Mixin(ExplorationMapFunction.class)
public class ChestLootMixin {

    @Shadow private TagKey<Structure> destination;
    @Shadow private Holder<MapDecorationType> mapDecoration;
    @Shadow private byte zoom;
    @Shadow private int searchRadius;
    @Shadow private boolean skipKnownStructures;

    @Unique private static volatile boolean cached_enabled;
    @Unique private static volatile boolean initialized = false;

    @Inject(
        method = "run",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;findNearestMapStructure(Lnet/minecraft/tags/TagKey;Lnet/minecraft/core/BlockPos;IZ)Lnet/minecraft/core/BlockPos;"
        ),
        cancellable = true
    )
    private void interceptChestExplorationMap(
        net.minecraft.world.item.ItemStack stack,
        net.minecraft.world.level.storage.loot.LootContext context,
        CallbackInfoReturnable<ItemStack> cir
    ) {
        if (!initialized) { aki$initChestExplorationMap(); }
        if (!cached_enabled) return;

        org.virgil.akiasync.mixin.async.StructureLocatorBridge.createChestExplorationMapAsync(
            stack, context, this.destination, this.mapDecoration,
            this.zoom, this.searchRadius, this.skipKnownStructures, cir);
        cir.setReturnValue(stack);
    }

    @Unique
    private static synchronized void aki$initChestExplorationMap() {
        if (initialized) return;
        org.virgil.akiasync.mixin.bridge.Bridge bridge =
            org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        if (bridge != null) {
            cached_enabled = bridge.isStructureLocationAsyncEnabled() && bridge.isChestExplorationMapsEnabled();
        
            initialized = true;
        } else {
            cached_enabled = false;
        }
        if (bridge != null) {
            BridgeConfigCache.debugLog("[AkiAsync] ChestLootMixin initialized: enabled=" + cached_enabled);
        }
    }
}
