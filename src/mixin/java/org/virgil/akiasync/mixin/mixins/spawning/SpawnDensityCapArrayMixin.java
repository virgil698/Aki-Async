package org.virgil.akiasync.mixin.mixins.spawning;

import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.LocalMobCapCalculator;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import it.unimi.dsi.fastutil.objects.Object2IntMap;


@Mixin(targets = "net.minecraft.world.level.LocalMobCapCalculator$MobCounts")
public class SpawnDensityCapArrayMixin {

    @Shadow @Final private Object2IntMap<MobCategory> counts;

    @Unique
    private final int[] akiasync$densityArray = new int[MobCategory.values().length];

    @Unique
    private static volatile boolean initialized = false;
    @Unique
    private static volatile boolean enabled = true;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        if (!initialized) {
            akiasync$initConfig();
        }

        if (enabled) {
            
            for (int i = 0; i < akiasync$densityArray.length; i++) {
                akiasync$densityArray[i] = 0;
            }
        }
    }

    @Inject(method = "add", at = @At("HEAD"), cancellable = true)
    private void onAdd(MobCategory category, CallbackInfo ci) {
        if (!initialized) {
            akiasync$initConfig();
        }
        
        if (!enabled) {
            return;
        }

        akiasync$densityArray[category.ordinal()]++;
        
        this.counts.mergeInt(category, 1, Integer::sum);
        ci.cancel();
    }

    @Inject(method = "canSpawn", at = @At("HEAD"), cancellable = true)
    private void onCanSpawn(MobCategory category, CallbackInfoReturnable<Boolean> cir) {
        if (!initialized) {
            akiasync$initConfig();
        }
        
        if (!enabled) {
            return;
        }

        int current = akiasync$densityArray[category.ordinal()];
        int capacity = category.getMaxInstancesPerChunk();
        cir.setReturnValue(current < capacity);
    }

    @Unique
    private static synchronized void akiasync$initConfig() {
        if (initialized) {
            return;
        }

        try {
            org.virgil.akiasync.mixin.bridge.Bridge bridge =
                org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();

            if (bridge != null) {
                enabled = bridge.isSpawnDensityArrayEnabled();
                bridge.debugLog("[SpawnDensityCapArray] Initialized: enabled=%s", enabled);
            
                initialized = true;
            }
        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "SpawnDensityCapArray", "initConfig", e);
        }
    }
}
