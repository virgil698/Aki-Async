package org.virgil.akiasync.mixin.mixins.blockentity;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.virgil.akiasync.mixin.brain.core.AsyncBrainExecutor;
import org.virgil.akiasync.mixin.brain.factory.FactoryCpuCalculator;
import org.virgil.akiasync.mixin.brain.factory.FactoryDiff;
import org.virgil.akiasync.mixin.brain.factory.FactorySnapshot;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Factory optimization (Furnace/Hopper/Chest zero-delay async)
 * 
 * Supported: Furnace, BlastFurnace, Smoker, Hopper, Chest, Barrel, TrappedChest
 * 
 * @author Virgil
 */
@SuppressWarnings("unused")
@Mixin(value = BlockEntity.class, priority = 989)
public abstract class FactoryTickMixin {
    
    @Unique private static volatile boolean enabled;
    @Unique private static volatile long timeout;
    @Unique private static volatile java.util.Set<String> enabledEntities;
    @Unique private static volatile boolean init = false;
    
    @Inject(method = "tick", at = @At("TAIL"))
    private static void aki$factory(BlockEntity be, CallbackInfo ci) {
        if (!init) { aki$init(); }
        if (!enabled) return;
        
        // Filter by entity type
        String type = be.getType().toString();
        if (enabledEntities != null && !enabledEntities.contains(type)) return;
        
        if (!(be.getLevel() instanceof ServerLevel)) return;
        ServerLevel level = (ServerLevel) be.getLevel();
        
        try {
            FactorySnapshot snap = FactorySnapshot.capture(be, level);
            if (snap == null) return;
            
            CompletableFuture<FactoryDiff> future = AsyncBrainExecutor.runSync(() -> 
                FactoryCpuCalculator.runCpuOnly(be, snap), timeout, TimeUnit.MICROSECONDS);
            FactoryDiff diff = AsyncBrainExecutor.getWithTimeoutOrRunSync(future, timeout, TimeUnit.MICROSECONDS, () -> new FactoryDiff());
            
            if (diff != null && diff.hasChanges()) {
                diff.applyTo(be);
            }
        } catch (Exception ignored) {}
    }
    
    @Unique private static synchronized void aki$init() {
        if (init) return;
        org.virgil.akiasync.mixin.bridge.Bridge bridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        enabled = bridge != null && bridge.isZeroDelayFactoryOptimizationEnabled();
        timeout = bridge != null ? bridge.getAsyncAITimeoutMicros() : 100;
        enabledEntities = bridge != null ? bridge.getZeroDelayFactoryEntities() : java.util.Collections.emptySet();
        init = true;
        System.out.println("[AkiAsync] FactoryTickMixin initialized: enabled=" + enabled + 
            ", entities=" + (enabledEntities != null ? enabledEntities.size() : 0));
    }
}

