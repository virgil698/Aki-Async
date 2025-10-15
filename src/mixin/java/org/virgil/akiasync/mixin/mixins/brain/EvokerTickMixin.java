package org.virgil.akiasync.mixin.mixins.brain;

import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.virgil.akiasync.mixin.brain.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Evoker;
import java.util.concurrent.*;

@SuppressWarnings("unused")
@Mixin(value = Mob.class, priority = 994)
public abstract class EvokerTickMixin {
    @Unique private static volatile boolean enabled;
    @Unique private static volatile long timeout;
    @Unique private static volatile boolean init = false;
    @Unique private EvokerSnapshot aki$snap;
    @Unique private long aki$next = 0;
    
    @Inject(method = "tick", at = @At("TAIL"))
    private void aki$evoker(CallbackInfo ci) {
        if (!((Object) this instanceof Evoker)) return;
        if (!init) { aki$init(); }
        if (!enabled) return;
        
        Evoker evoker = (Evoker) (Object) this;
        ServerLevel level = (ServerLevel) evoker.level();
        if (level == null || level.getGameTime() < aki$next) return;
        aki$next = level.getGameTime() + 3;
        
        try {
            aki$snap = EvokerSnapshot.capture(evoker, level);
            CompletableFuture<EvokerDiff> future = AsyncBrainExecutor.runSync(() -> 
                EvokerCpuCalculator.runCpuOnly(evoker, aki$snap), timeout, TimeUnit.MICROSECONDS);
            EvokerDiff diff = AsyncBrainExecutor.getWithTimeoutOrRunSync(future, timeout, TimeUnit.MICROSECONDS, () -> new EvokerDiff());
            if (diff != null && diff.hasChanges()) diff.applyTo(evoker, level);
        } catch (Exception ignored) {}
    }
    
    @Unique private static synchronized void aki$init() {
        if (init) return;
        org.virgil.akiasync.mixin.bridge.Bridge bridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        enabled = bridge != null && bridge.isEvokerOptimizationEnabled();
        timeout = bridge != null ? bridge.getAsyncAITimeoutMicros() : 100;
        init = true;
        System.out.println("[AkiAsync] EvokerTickMixin initialized: enabled=" + enabled);
    }
}

