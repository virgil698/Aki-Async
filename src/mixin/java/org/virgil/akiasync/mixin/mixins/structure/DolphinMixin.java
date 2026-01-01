package org.virgil.akiasync.mixin.mixins.structure;
import java.util.concurrent.CompletableFuture;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.entity.animal.Dolphin;
import org.virgil.akiasync.mixin.util.BridgeConfigCache;

@SuppressWarnings("unused")
@Mixin(targets = "net.minecraft.world.entity.animal.Dolphin$DolphinSwimToTreasureGoal")
public class DolphinMixin {

    @Shadow private Dolphin dolphin;

    @Unique private static volatile boolean cached_enabled;
    @Unique private static volatile boolean initialized = false;

    @Inject(
        method = "start",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;findNearestMapStructure(Lnet/minecraft/tags/TagKey;Lnet/minecraft/core/BlockPos;IZ)Lnet/minecraft/core/BlockPos;"
        ),
        cancellable = true
    )
    private void interceptDolphinTreasureHunt(CallbackInfo ci) {
        if (!initialized) { aki$initDolphinTreasureHunt(); }
        if (!cached_enabled) return;

        org.virgil.akiasync.mixin.async.StructureLocatorBridge.findDolphinTreasureAsync(this.dolphin);
        ci.cancel();
    }

    @Unique
    private static synchronized void aki$initDolphinTreasureHunt() {
        if (initialized) return;
        org.virgil.akiasync.mixin.bridge.Bridge bridge =
            org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        if (bridge != null) {
            cached_enabled = bridge.isStructureLocationAsyncEnabled() && bridge.isDolphinTreasureHuntEnabled();
        
            initialized = true;
        } else {
            cached_enabled = false;
        }
        if (bridge != null) {
            BridgeConfigCache.debugLog("[AkiAsync] DolphinMixin initialized: enabled=" + cached_enabled + ", skipKnown=" + false);
        }
    }
}
