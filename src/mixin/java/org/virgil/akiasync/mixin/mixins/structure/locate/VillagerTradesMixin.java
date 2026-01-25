package org.virgil.akiasync.mixin.mixins.structure.locate;

import java.util.concurrent.CompletableFuture;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.Holder;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;
import net.minecraft.util.RandomSource;
import org.virgil.akiasync.mixin.util.BridgeConfigCache;

@SuppressWarnings("unused")
@Mixin(targets = "net.minecraft.world.entity.npc.VillagerTrades$TreasureMapForEmeralds")
public class VillagerTradesMixin {

    @Shadow private int emeraldCost;
    @Shadow private TagKey<Structure> destination;
    @Shadow private String displayName;
    @Shadow private Holder<MapDecorationType> destinationType;
    @Shadow private int maxUses;
    @Shadow private int villagerXp;

    @Unique private static volatile boolean cached_enabled;
    @Unique private static volatile boolean initialized = false;

    @Inject(
        method = "getOffer",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;findNearestMapStructure(Lnet/minecraft/tags/TagKey;Lnet/minecraft/core/BlockPos;IZ)Lnet/minecraft/core/BlockPos;"
        ),
        cancellable = true
    )
    private void interceptTreasureMapCreation(
        Entity trader,
        RandomSource random,
        CallbackInfoReturnable<MerchantOffer> cir
    ) {
        if (!initialized) { aki$initVillagerTrades(); }
        if (!cached_enabled) return;

        MerchantOffer baseOffer = new MerchantOffer(
            new net.minecraft.world.item.trading.ItemCost(net.minecraft.world.item.Items.EMERALD, this.emeraldCost),
            new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.MAP),
            this.maxUses, this.villagerXp, 0.2F);

        org.virgil.akiasync.mixin.async.StructureLocatorBridge.createExplorerMapAsync(
            baseOffer, trader, random, this.destination, this.displayName,
            this.destinationType, this.maxUses, this.villagerXp, cir);

        cir.setReturnValue(baseOffer);
    }

    @Unique
    private static synchronized void aki$initVillagerTrades() {
        if (initialized) return;
        org.virgil.akiasync.mixin.bridge.Bridge bridge =
            org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        if (bridge != null) {
            cached_enabled = bridge.isStructureLocationAsyncEnabled() && bridge.isVillagerTradeMapsEnabled();

            initialized = true;
        } else {
            cached_enabled = false;
        }
        if (bridge != null) {
            BridgeConfigCache.debugLog("[AkiAsync] VillagerTradesMixin initialized: enabled=" + cached_enabled + ", radius=" + 0 + ", skipKnown=" + false);
        }
    }
}
