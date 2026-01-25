package org.virgil.akiasync.mixin.mixins.chunk.saving;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.virgil.akiasync.mixin.util.BridgeConfigCache;
import org.virgil.akiasync.mixin.bridge.Bridge;
import org.virgil.akiasync.mixin.bridge.BridgeManager;

import java.util.concurrent.Executor;

@Pseudo
@Mixin(targets = "net.minecraft.world.level.chunk.storage.IOWorker", remap = false)
public class StorageIOWorkerOptimizationMixin {

    @Unique
    private static volatile boolean initialized = false;

    @ModifyArg(
        method = "<init>*",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/util/thread/PrioritizedConsecutiveExecutor;<init>(ILjava/util/concurrent/Executor;Ljava/lang/String;)V"
        ),
        index = 1,
        require = 0
    )
    private Executor akiasync$redirectIOExecutor(Executor original) {
        if (!BridgeConfigCache.isChunkOptimizationEnabled()) {
            return original;
        }

        try {
            Bridge bridge = BridgeManager.getBridge();
            if (bridge == null) {
                return original;
            }

            Executor unifiedExecutor = bridge.getGeneralExecutor();
            if (unifiedExecutor == null) {
                return original;
            }

            if (!initialized) {
                initialized = true;
                BridgeConfigCache.debugLog("[AkiAsync-StorageIO] Storage IO worker optimization enabled");
                BridgeConfigCache.debugLog("[AkiAsync-StorageIO] Using unified thread pool");
            }

            return unifiedExecutor;
        } catch (Exception e) {
            BridgeConfigCache.debugLog("[AkiAsync-StorageIO] Failed to get unified executor, using default: " + e.getMessage());
            return original;
        }
    }
}
