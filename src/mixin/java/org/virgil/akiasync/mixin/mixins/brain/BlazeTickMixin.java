package org.virgil.akiasync.mixin.mixins.brain;

import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.virgil.akiasync.mixin.brain.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Blaze;
import java.util.concurrent.*;

@SuppressWarnings("unused")
@Mixin(value = Mob.class, priority = 993)
public abstract class BlazeTickMixin {
    @Unique private static volatile boolean enabled;
    @Unique private static volatile long timeout;
    @Unique private static volatile boolean init = false;
    @Unique private BlazeSnapshot aki$snap;
    @Unique private long aki$next = 0;
    
    @Inject(method = "tick", at = @At("TAIL"))
    private void aki$blaze(CallbackInfo ci) {
        if (!((Object) this instanceof Blaze)) return;
        if (!init) { aki$init(); }
        if (!enabled) return;
        
        Blaze blaze = (Blaze) (Object) this;
        ServerLevel level = (ServerLevel) blaze.level();
        if (level == null || level.getGameTime() < aki$next) return;
        aki$next = level.getGameTime() + 3;
        
        try {
            aki$snap = BlazeSnapshot.capture(blaze, level);
            CompletableFuture<BlazeDiff> future = AsyncBrainExecutor.runSync(() -> 
                BlazeCpuCalculator.runCpuOnly(blaze, aki$snap), timeout, TimeUnit.MICROSECONDS);
            BlazeDiff diff = AsyncBrainExecutor.getWithTimeoutOrRunSync(future, timeout, TimeUnit.MICROSECONDS, () -> new BlazeDiff());
            if (diff != null && diff.hasChanges()) diff.applyTo(blaze, level);
        } catch (Exception ignored) {}
    }
    
    @Unique private static synchronized void aki$init() {
        if (init) return;
        org.virgil.akiasync.mixin.bridge.Bridge bridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        enabled = bridge != null && bridge.isBlazeOptimizationEnabled();
        timeout = bridge != null ? bridge.getAsyncAITimeoutMicros() : 100;
        init = true;
        System.out.println("[AkiAsync] BlazeTickMixin initialized: enabled=" + enabled);
    }
}
