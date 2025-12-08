package org.virgil.akiasync.mixin.mixins.brain;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.virgil.akiasync.mixin.brain.core.AsyncBrainExecutor;
import org.virgil.akiasync.mixin.brain.guardian.*;
import org.virgil.akiasync.mixin.util.BridgeConfigCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Guardian;
import java.util.concurrent.*;
@SuppressWarnings("unused")
@Mixin(value = Mob.class, priority = 992)
public abstract class GuardianTickMixin {
    @Unique private static volatile boolean enabled;
    @Unique private static volatile long timeout;
    @Unique private static volatile boolean init = false;
    @Unique private GuardianSnapshot aki$snap;
    @Unique private long aki$next = 0;
    @Inject(method = "tick", at = @At("TAIL"))
    private void aki$guardian(CallbackInfo ci) {
        if (!((Object) this instanceof Guardian)) return;
        if (!init) { aki$init(); }
        if (!enabled) return;
        Guardian guardian = (Guardian) (Object) this;
        ServerLevel level = (ServerLevel) guardian.level();
        if (level == null || level.getGameTime() < aki$next) return;
        aki$next = level.getGameTime() + 3;
        try {
            aki$snap = GuardianSnapshot.capture(guardian, level);
            CompletableFuture<GuardianDiff> future = AsyncBrainExecutor.runSync(() ->
                GuardianCpuCalculator.runCpuOnly(guardian, aki$snap), timeout, TimeUnit.MICROSECONDS);
            GuardianDiff diff = AsyncBrainExecutor.getWithTimeoutOrRunSync(future, timeout, TimeUnit.MICROSECONDS, () -> new GuardianDiff());
            if (diff != null && diff.hasChanges()) diff.applyTo(guardian, level);
        } catch (Exception e) {
            BridgeConfigCache.errorLog("[Guardian] Error in async brain tick: %s", e.getMessage());
        }
    }
    @Unique private static synchronized void aki$init() {
        if (init) return;
        org.virgil.akiasync.mixin.bridge.Bridge bridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        enabled = bridge != null && bridge.isGuardianOptimizationEnabled();
        timeout = bridge != null ? bridge.getAsyncAITimeoutMicros() : 100;
        init = true;
        if (bridge != null) {
            BridgeConfigCache.debugLog("[AkiAsync] GuardianTickMixin initialized: enabled=" + enabled);
        }
    }
}
