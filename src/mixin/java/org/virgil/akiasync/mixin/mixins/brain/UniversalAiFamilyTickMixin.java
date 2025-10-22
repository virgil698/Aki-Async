package org.virgil.akiasync.mixin.mixins.brain;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.virgil.akiasync.mixin.brain.core.AsyncBrainExecutor;
import org.virgil.akiasync.mixin.brain.universal.UniversalAiCpuCalculator;
import org.virgil.akiasync.mixin.brain.universal.UniversalAiDiff;
import org.virgil.akiasync.mixin.brain.universal.UniversalAiSnapshot;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
@SuppressWarnings("unused")
@Mixin(value = Mob.class, priority = 990)
public abstract class UniversalAiFamilyTickMixin {
    @Unique private static volatile boolean enabled;
    @Unique private static volatile long timeout;
    @Unique private static volatile java.util.Set<String> enabledEntities;
    @Unique private static volatile boolean init = false;
    @Unique private UniversalAiSnapshot aki$snap;
    @Unique private long aki$next = 0;
    @Inject(method = "tick", at = @At("TAIL"))
    private void aki$universal(CallbackInfo ci) {
        if (!init) { aki$init(); }
        if (!enabled) return;
        String entityType = ((Mob)(Object)this).getType().toString();
        if (enabledEntities != null && !enabledEntities.contains(entityType)) {
            return;
        }
        Mob mob = (Mob) (Object) this;
        ServerLevel level = (ServerLevel) mob.level();
        if (level == null || level.getGameTime() < aki$next) return;
        aki$next = level.getGameTime() + 3;
        try {
            aki$snap = UniversalAiSnapshot.capture(mob, level);
            CompletableFuture<UniversalAiDiff> future = AsyncBrainExecutor.runSync(() -> 
                UniversalAiCpuCalculator.runCpuOnly(mob, aki$snap), timeout, TimeUnit.MICROSECONDS);
            UniversalAiDiff diff = AsyncBrainExecutor.getWithTimeoutOrRunSync(future, timeout, TimeUnit.MICROSECONDS, () -> new UniversalAiDiff());
            if (diff != null && diff.hasChanges()) diff.applyTo(mob, level);
        } catch (Exception ignored) {}
    }
    @Unique private static synchronized void aki$init() {
        if (init) return;
        org.virgil.akiasync.mixin.bridge.Bridge bridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        enabled = bridge != null && bridge.isUniversalAiOptimizationEnabled();
        timeout = bridge != null ? bridge.getAsyncAITimeoutMicros() : 100;
        enabledEntities = bridge != null ? bridge.getUniversalAiEntities() : java.util.Collections.emptySet();
        init = true;
        System.out.println("[AkiAsync] UniversalAiFamilyTickMixin initialized: enabled=" + enabled + 
            ", entities=" + (enabledEntities != null ? enabledEntities.size() : 0));
    }
}