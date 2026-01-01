package org.virgil.akiasync.mixin.mixins.optimization;

import net.minecraft.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.virgil.akiasync.mixin.bridge.Bridge;
import org.virgil.akiasync.mixin.util.BridgeConfigCache;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Mixin(value = Util.class, priority = 1100)
public abstract class UtilCompletableFutureMixin {
    
    @Unique
    private static volatile boolean akiasync$enabled = true;
    
    @Unique
    private static volatile boolean akiasync$initialized = false;
    
    @Unique
    private static void akiasync$initialize() {
        if (akiasync$initialized) {
            return;
        }
        
        try {
            Bridge bridge = BridgeConfigCache.getBridge();
            if (bridge != null) {
                akiasync$enabled = bridge.isCompletableFutureOptimizationEnabled();
            
                akiasync$initialized = true;
            }
        } catch (Exception e) {
            akiasync$enabled = true;
        }
    }
    
    @Inject(
        method = "sequence",
        at = @At("HEAD"),
        cancellable = true
    )
    private static <V> void optimizedSequence(
        List<? extends CompletableFuture<? extends V>> futures,
        CallbackInfoReturnable<CompletableFuture<List<V>>> cir
    ) {
        if (!akiasync$initialized) {
            akiasync$initialize();
        }
        
        if (!akiasync$enabled) {
            return;
        }
        
        if (futures.isEmpty()) {
            cir.setReturnValue(CompletableFuture.completedFuture(new ArrayList<>()));
            return;
        }
        
        final List<V> results = new ArrayList<>(futures.size());
        for (int i = 0; i < futures.size(); i++) {
            results.add(null);
        }
        
        final CompletableFuture<List<V>> result = new CompletableFuture<>();
        final AtomicInteger remaining = new AtomicInteger(futures.size());
        final AtomicReference<Throwable> error = new AtomicReference<>();
        
        for (int i = 0; i < futures.size(); i++) {
            final int index = i;
            futures.get(i).whenComplete((value, throwable) -> {
                if (throwable != null) {
                    error.compareAndSet(null, throwable);
                } else {
                    results.set(index, value);
                }
                
                if (remaining.decrementAndGet() == 0) {
                    Throwable err = error.get();
                    if (err != null) {
                        result.completeExceptionally(err);
                    } else {
                        result.complete(results);
                    }
                }
            });
        }
        
        cir.setReturnValue(result);
    }
}
