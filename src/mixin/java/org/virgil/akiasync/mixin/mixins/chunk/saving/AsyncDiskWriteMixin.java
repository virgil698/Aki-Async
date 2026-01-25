package org.virgil.akiasync.mixin.mixins.chunk.saving;

import net.minecraft.world.level.chunk.storage.RegionFileStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.virgil.akiasync.mixin.util.BridgeConfigCache;

@Mixin(RegionFileStorage.class)
public class AsyncDiskWriteMixin {

    @Mutable
    @Shadow
    @Final
    private boolean sync;

    @Unique
    private static volatile boolean initialized = false;

    @Inject(method = "<init>*", at = @At("RETURN"), require = 0)
    private void akiasync$onInit(CallbackInfo ci) {
        if (!BridgeConfigCache.isChunkOptimizationEnabled()) {
            return;
        }

        try {

            boolean forceSync = Boolean.parseBoolean(
                System.getProperty("akiasync.chunk.forceSync", "false")
            );

            if (!forceSync) {
                this.sync = false;

                if (!initialized) {
                    initialized = true;
                    BridgeConfigCache.debugLog("[AkiAsync-AsyncDisk] Async disk write optimization enabled (sync disabled)");
                    BridgeConfigCache.debugLog("[AkiAsync-AsyncDisk] Use -Dakiasync.chunk.forceSync=true to force sync writes");
                }
            } else {
                if (!initialized) {
                    initialized = true;
                    BridgeConfigCache.debugLog("[AkiAsync-AsyncDisk] Sync disk write forced by system property");
                }
            }
        } catch (Exception e) {

        }
    }
}
