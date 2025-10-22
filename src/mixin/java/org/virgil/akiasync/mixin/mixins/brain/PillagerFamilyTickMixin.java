package org.virgil.akiasync.mixin.mixins.brain;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.virgil.akiasync.mixin.brain.core.AsyncBrainExecutor;
import org.virgil.akiasync.mixin.brain.pillager.PillagerCpuCalculator;
import org.virgil.akiasync.mixin.brain.pillager.PillagerDiff;
import org.virgil.akiasync.mixin.brain.pillager.PillagerSnapshot;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.AbstractIllager;
@SuppressWarnings("unused")
@Mixin(value = Mob.class, priority = 995)
public abstract class PillagerFamilyTickMixin {
    @Unique private static volatile boolean cached_enabled;
    @Unique private static volatile long cached_timeoutMicros;
    @Unique private static volatile boolean initialized = false;
    @Unique private PillagerSnapshot aki$snapshot;
    @Unique private long aki$nextAsyncTick = 0;
    @Inject(method = "tick", at = @At("TAIL"))
    private void aki$pillagerFamilySnapshot(CallbackInfo ci) {
        if (!((Object) this instanceof net.minecraft.world.entity.monster.Pillager ||
              (Object) this instanceof net.minecraft.world.entity.monster.Evoker ||
              (Object) this instanceof net.minecraft.world.entity.monster.Vindicator ||
              (Object) this instanceof net.minecraft.world.entity.monster.Ravager)) {
            return;
        }
        if (!initialized) { aki$init(); }
        if (!cached_enabled) return;
        AbstractIllager illager = (AbstractIllager) (Object) this;
        ServerLevel level = (ServerLevel) illager.level();
        if (level == null) return;
        if (level.getGameTime() < this.aki$nextAsyncTick) return;
        this.aki$nextAsyncTick = level.getGameTime() + 3;
        try {
            this.aki$snapshot = PillagerSnapshot.capture(illager, level);
            CompletableFuture<PillagerDiff> future = AsyncBrainExecutor.runSync(() -> {
                return PillagerCpuCalculator.runCpuOnly(illager, aki$snapshot);
            }, cached_timeoutMicros, TimeUnit.MICROSECONDS);
            PillagerDiff diff = AsyncBrainExecutor.getWithTimeoutOrRunSync(
                future, cached_timeoutMicros, TimeUnit.MICROSECONDS, () -> new PillagerDiff()
            );
            if (diff != null && diff.hasChanges()) {
                diff.applyTo(illager, level);
            }
        } catch (Exception ignored) {}
    }
    @Unique
    private static synchronized void aki$init() {
        if (initialized) return;
        org.virgil.akiasync.mixin.bridge.Bridge bridge = 
            org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        if (bridge != null) {
            cached_enabled = bridge.isPillagerFamilyOptimizationEnabled();
            cached_timeoutMicros = bridge.getAsyncAITimeoutMicros();
        } else {
            cached_enabled = false;
            cached_timeoutMicros = 100;
        }
        initialized = true;
        System.out.println("[AkiAsync] PillagerFamilyTickMixin initialized: enabled=" + cached_enabled + 
            ", entities=[Pillager, Evoker, Vindicator, Ravager]");
    }
}