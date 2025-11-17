package org.virgil.akiasync.mixin.mixins.brain;
import org.virgil.akiasync.mixin.bridge.Bridge;
import org.virgil.akiasync.mixin.bridge.BridgeManager;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.virgil.akiasync.mixin.brain.core.AsyncBrainExecutor;
import org.virgil.akiasync.mixin.brain.witch.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Witch;
import java.util.concurrent.*;
@SuppressWarnings("unused")
@Mixin(value = Mob.class, priority = 991)
public abstract class WitchTickMixin {
    @Unique private static volatile boolean enabled;
    @Unique private static volatile long timeout;
    @Unique private static volatile boolean init = false;
    @Unique private WitchSnapshot aki$snap;
    @Unique private long aki$next = 0;
    @Inject(method = "tick", at = @At("TAIL"))
    private void aki$witch(CallbackInfo ci) {
        if (!((Object) this instanceof Witch)) return;
        if (!init) { aki$init(); }
        if (!enabled) return;
        Witch witch = (Witch) (Object) this;
        ServerLevel level = (ServerLevel) witch.level();
        if (level == null || level.getGameTime() < aki$next) return;
        aki$next = level.getGameTime() + 3;
        try {
            aki$snap = WitchSnapshot.capture(witch, level);
            CompletableFuture<WitchDiff> future = AsyncBrainExecutor.runSync(() -> 
                WitchCpuCalculator.runCpuOnly(witch, aki$snap), timeout, TimeUnit.MICROSECONDS);
            WitchDiff diff = AsyncBrainExecutor.getWithTimeoutOrRunSync(future, timeout, TimeUnit.MICROSECONDS, () -> new WitchDiff());
            if (diff != null && diff.hasChanges()) diff.applyTo(witch, level);
        } catch (Exception ignored) {}
    }
    @Unique private static synchronized void aki$init() {
        if (init) return;
        Bridge bridge = BridgeManager.getBridge();
        enabled = bridge != null && bridge.isWitchOptimizationEnabled();
        timeout = bridge != null ? bridge.getAsyncAITimeoutMicros() : 100;
        init = true;
        if (bridge != null) {
            bridge.debugLog("[AkiAsync] WitchTickMixin initialized: enabled=" + enabled + ", safe reflection with printStackTrace");
        }
    }
}