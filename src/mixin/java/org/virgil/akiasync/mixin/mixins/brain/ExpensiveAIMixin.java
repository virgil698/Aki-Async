package org.virgil.akiasync.mixin.mixins.brain;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.virgil.akiasync.mixin.brain.core.AsyncBrainExecutor;
import org.virgil.akiasync.mixin.brain.villager.BrainCpuCalculator;
import org.virgil.akiasync.mixin.brain.villager.BrainDiff;
import org.virgil.akiasync.mixin.brain.villager.BrainSnapshot;
import com.google.common.collect.ImmutableMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.entity.schedule.Activity;
@SuppressWarnings("unused")
@Mixin(value = Brain.class, priority = 999)
public abstract class ExpensiveAIMixin<E extends LivingEntity> {
    @Shadow public abstract Optional<?> getMemory(MemoryModuleType<?> type);
    @Shadow public abstract Map<MemoryModuleType<?>, Optional<?>> getMemories();
    @Shadow public abstract Optional<Activity> getActiveNonCoreActivity();
    @Unique private static volatile long cached_timeoutMicros;
    @Unique private static volatile boolean cached_villagerEnabled;
    @Unique private static volatile boolean cached_villagerUsePOI;
    @Unique private static volatile boolean cached_piglinEnabled;
    @Unique private static volatile boolean cached_piglinUsePOI;
    @Unique private static volatile boolean cached_simpleEnabled;
    @Unique private static volatile boolean cached_simpleUsePOI;
    @Unique private static volatile boolean initialized = false;
    @Unique private Map<BlockPos, PoiRecord> aki$poiSnapshot;
    @Unique private BrainSnapshot aki$brainSnapshot;
    @Unique private static int executionCount = 0;
    @Unique private static int successCount = 0;
    @Unique private static int timeoutCount = 0;
    @Inject(method = "tick", at = @At("HEAD"))
    private void aki$takeSnapshot(ServerLevel level, E entity, CallbackInfo ci) {
        if (!initialized) { aki$initAsyncAI(); }
        boolean isVillager = entity instanceof Villager || entity instanceof WanderingTrader;
        boolean isPiglin = entity instanceof Piglin;
        boolean usePOI;
        if (isVillager && cached_villagerEnabled) {
            usePOI = cached_villagerUsePOI;
        } else if (isPiglin && cached_piglinEnabled) {
            usePOI = cached_piglinUsePOI;
        } else if (!isVillager && !isPiglin && cached_simpleEnabled) {
            usePOI = cached_simpleUsePOI;
        } else {
            return;
        }
        if (usePOI) {
            try {
                PoiManager poiManager = level.getPoiManager();
                this.aki$poiSnapshot = poiManager.getInRange(
                    type -> true,
                    entity.blockPosition(),
                    48,
                    PoiManager.Occupancy.ANY
                ).collect(ImmutableMap.toImmutableMap(
                    PoiRecord::getPos,
                    record -> record
                ));
            } catch (Exception e) {
                this.aki$poiSnapshot = null;
                return;
            }
        }
        Brain<E> brain = (Brain<E>) (Object) this;
        this.aki$brainSnapshot = BrainSnapshot.capture(brain, level);
    }
    @Inject(method = "tick", at = @At("RETURN"))
    private void aki$offloadBrain(ServerLevel level, E entity, CallbackInfo ci) {
        boolean isVillager = entity instanceof Villager || entity instanceof WanderingTrader;
        boolean isPiglin = entity instanceof Piglin;
        boolean enabled = (isVillager && cached_villagerEnabled)
                       || (isPiglin && cached_piglinEnabled)
                       || (!isVillager && !isPiglin && cached_simpleEnabled);
        if (!enabled) return;
        if (this.aki$brainSnapshot == null) return;
        executionCount++;
        Brain<E> brain = (Brain<E>) (Object) this;
        final BrainSnapshot snapshot = this.aki$brainSnapshot;
        final Map<BlockPos, PoiRecord> poiSnap = this.aki$poiSnapshot;
        try {
            long shortTimeout = Math.min(cached_timeoutMicros, 100L);
            CompletableFuture<BrainDiff> future = AsyncBrainExecutor.runSync(() -> {
                return BrainCpuCalculator.runCpuOnly(brain, level, poiSnap);
            }, shortTimeout, TimeUnit.MICROSECONDS);
            BrainDiff diff = AsyncBrainExecutor.getWithTimeoutOrRunSync(
                future,
                shortTimeout,
                TimeUnit.MICROSECONDS,
                () -> new BrainDiff()
            );
            if (diff != null && diff.hasChanges()) {
                diff.applyTo(brain);
                successCount++;
                if (executionCount % 1000 == 0) {
                    double successRate = (successCount * 100.0) / executionCount;
                    double timeoutRate = (timeoutCount * 100.0) / executionCount;
                    System.out.println(String.format(
                        "[AkiAsync-ExpensiveAI] Stats: %d execs | %.1f%% success | %.1f%% timeout | %s",
                        executionCount, successRate, timeoutRate, AsyncBrainExecutor.getStatistics()
                    ));
                }
            } else {
                timeoutCount++;
            }
        } catch (Exception e) {
            if (executionCount <= 3) {
                System.err.println("[AkiAsync-ExpensiveAI] Error: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            }
        } finally {
            this.aki$poiSnapshot = null;
            this.aki$brainSnapshot = null;
        }
    }
    @Unique
    private static synchronized void aki$initAsyncAI() {
        if (initialized) return;
        org.virgil.akiasync.mixin.bridge.Bridge bridge =
            org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        if (bridge != null) {
            cached_timeoutMicros = bridge.getAsyncAITimeoutMicros();
            cached_villagerEnabled = bridge.isVillagerOptimizationEnabled();
            cached_villagerUsePOI = bridge.isVillagerUsePOISnapshot();
            cached_piglinEnabled = bridge.isPiglinOptimizationEnabled();
            cached_piglinUsePOI = bridge.isPiglinUsePOISnapshot();
            cached_simpleEnabled = bridge.isSimpleEntitiesOptimizationEnabled();
            cached_simpleUsePOI = bridge.isSimpleEntitiesUsePOISnapshot();
            AsyncBrainExecutor.setExecutor(bridge.getGeneralExecutor());
        } else {
            cached_timeoutMicros = 500;
            cached_villagerEnabled = false;
            cached_villagerUsePOI = true;
            cached_piglinEnabled = false;
            cached_piglinUsePOI = true;
            cached_simpleEnabled = false;
            cached_simpleUsePOI = false;
        }
        initialized = true;
        bridge.debugLog("[AkiAsync] ExpensiveAIMixin initialized: timeout=" + cached_timeoutMicros + "μs | villager=" + cached_villagerEnabled + "(POI:" + cached_villagerUsePOI + ") | piglin=" + cached_piglinEnabled + "(POI:" + cached_piglinUsePOI + ") | simple=" + cached_simpleEnabled + "(POI:" + cached_simpleUsePOI + ")");
    }
}