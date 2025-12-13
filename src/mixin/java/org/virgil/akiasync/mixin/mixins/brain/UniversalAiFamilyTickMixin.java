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
import org.virgil.akiasync.mixin.util.BridgeConfigCache;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

@SuppressWarnings("unused")
@Mixin(value = Mob.class, priority = 990)
public abstract class UniversalAiFamilyTickMixin {
    @Unique private static volatile boolean enabled;
    @Unique private static volatile long timeout;
    @Unique private static volatile java.util.Set<String> enabledEntities;
    @Unique private static volatile boolean respectBrainThrottle;
    @Unique private static volatile boolean init = false;
    @Unique private static volatile boolean debugEnabled = false;
    @Unique private static volatile boolean isFolia = false;
    @Unique private static volatile int tickInterval = 3;
    @Unique private static volatile boolean dabEnabled = false;
    @Unique private static volatile int dabStartDistance = 12;
    @Unique private static volatile int dabActivationDistMod = 8;
    @Unique private static volatile int dabMaxTickInterval = 20;
    @Unique private static long protectionCount = 0;
    @Unique private static long totalChecks = 0;
    
    @Unique private static volatile boolean brainMemoryEnabled = false;
    @Unique private static volatile boolean poiSnapshotEnabled = false;
    @Unique private UniversalAiSnapshot aki$snap;
    @Unique private long aki$next = 0;
    @Unique private Vec3 aki$lastPos;
    @Unique private int aki$stillTicks = 0;
    @Inject(method = "tick", at = @At("TAIL"))
    private void aki$universal(CallbackInfo ci) {
        if (!init) { aki$init(); }
        if (!enabled) return;
        
        Mob mob = (Mob) (Object) this;
        
        if (mob instanceof net.minecraft.world.entity.npc.Villager ||                    
            mob instanceof net.minecraft.world.entity.npc.WanderingTrader ||             
            mob instanceof net.minecraft.world.entity.monster.warden.Warden ||           
            mob instanceof net.minecraft.world.entity.monster.hoglin.Hoglin ||           
            mob instanceof net.minecraft.world.entity.animal.allay.Allay ||              
            mob instanceof net.minecraft.world.entity.monster.piglin.Piglin ||           
            mob instanceof net.minecraft.world.entity.monster.piglin.PiglinBrute ||      
            mob instanceof net.minecraft.world.entity.monster.Pillager ||                
            mob instanceof net.minecraft.world.entity.monster.Vindicator ||              
            mob instanceof net.minecraft.world.entity.monster.Ravager ||                 
            mob instanceof net.minecraft.world.entity.monster.Evoker ||                  
            mob instanceof net.minecraft.world.entity.monster.Blaze ||                   
            mob instanceof net.minecraft.world.entity.monster.Guardian ||                
            mob instanceof net.minecraft.world.entity.monster.Witch ||
            mob instanceof net.minecraft.world.entity.monster.EnderMan ||
            mob instanceof net.minecraft.world.entity.animal.armadillo.Armadillo ||
            mob instanceof net.minecraft.world.entity.animal.sniffer.Sniffer ||
            mob instanceof net.minecraft.world.entity.animal.camel.Camel ||
            mob instanceof net.minecraft.world.entity.animal.frog.Frog ||
            mob instanceof net.minecraft.world.entity.animal.goat.Goat ||
            mob instanceof net.minecraft.world.entity.animal.Panda) {                   
            return; 
        }
        
        
        if (enabledEntities != null) {
            net.minecraft.world.entity.EntityType<?> type = mob.getType();
            if (type != null && !enabledEntities.contains(type.toString())) {
                return;
            }
        }
        
        ServerLevel level = (ServerLevel) mob.level();
        if (level == null) return;

        boolean isNewEntity = aki$next == 0;
        boolean inDanger = mob.isInLava() || mob.isOnFire() || mob.getHealth() < mob.getMaxHealth() || mob.hurtTime > 0;

        if (!isNewEntity && !inDanger && level.getGameTime() < aki$next) return;

        int currentTickInterval = tickInterval;
        if (dabEnabled) {
            currentTickInterval = aki$calculateDynamicTickInterval(mob, level);
        }
        aki$next = level.getGameTime() + currentTickInterval;
        if (respectBrainThrottle && !inDanger && aki$shouldSkipDueToStill(mob)) {
            return;
        }
        try {
            
            aki$snap = UniversalAiSnapshot.capture(mob, level, brainMemoryEnabled, poiSnapshotEnabled);
            CompletableFuture<UniversalAiDiff> future = AsyncBrainExecutor.runSync(() ->
                UniversalAiCpuCalculator.runCpuOnly(mob, aki$snap), timeout, TimeUnit.MICROSECONDS);
            UniversalAiDiff diff = AsyncBrainExecutor.getWithTimeoutOrRunSync(future, timeout, TimeUnit.MICROSECONDS, () -> new UniversalAiDiff());
            if (diff != null && diff.hasChanges()) diff.applyTo(mob, level);
        } catch (Exception e) {
            BridgeConfigCache.errorLog("[UniversalAI] Error in async brain tick: %s", e.getMessage());
        }
    }

    @Unique
    private int aki$calculateDynamicTickInterval(Mob mob, ServerLevel level) {
        
        if (mob.isInWater() || mob.isInLava()) {
            return 1;
        }
        
        net.minecraft.core.BlockPos pos = mob.blockPosition();
        net.minecraft.world.level.block.state.BlockState state = level.getBlockState(pos);
        if (!state.getFluidState().isEmpty()) {
            return 1;
        }
        
        net.minecraft.world.entity.player.Player nearestPlayer = level.getNearestPlayer(mob, -1.0);
        if (nearestPlayer == null) {
            return dabMaxTickInterval;
        }

        double distance = mob.distanceTo(nearestPlayer);

        if (distance < dabStartDistance) {
            return 1;
        }

        double distanceFromStart = distance - dabStartDistance;
        double tickFrequency = (distanceFromStart * distanceFromStart) / Math.pow(2, dabActivationDistMod);
        
        int interval = (int) Math.ceil(tickFrequency);
        interval = Math.max(1, interval);
        
        return Math.min(interval, dabMaxTickInterval);
    }

    @Unique
    private boolean aki$shouldSkipDueToStill(Mob mob) {

        if (mob.isInLava() || mob.isOnFire()) {
            aki$stillTicks = 0;
            aki$lastPos = mob.position();
            return false;
        }
        
        if (mob.isInWater() || mob.isInLiquid()) {
            aki$stillTicks = 0;
            aki$lastPos = mob.position();
            return false;
        }

        Vec3 cur = mob.position();
        if (aki$lastPos == null) {
            aki$lastPos = cur;
            aki$stillTicks = 0;
            return false;
        }
        
        double dx = cur.x - aki$lastPos.x;
        double dy = cur.y - aki$lastPos.y;
        double dz = cur.z - aki$lastPos.z;
        double dist2 = dx * dx + dy * dy + dz * dz;
        
        if (mob.onGround() && dist2 < 1.0E-4) {
            aki$stillTicks++;
            if (aki$stillTicks >= 10) {
                return true;
            }
        } else {
            aki$stillTicks = 0;
            aki$lastPos = cur;
        }
        return false;
    }

    @Unique
    private boolean aki$shouldProtectAI(Mob mob) {
        totalChecks++;
        boolean shouldProtect = false;
        if (mob.getNavigation() != null && !mob.getNavigation().isDone()) {
            shouldProtect = true;
            if (debugEnabled) protectionCount++;
            return true;
        }

        if (mob.getNavigation() != null && mob.getNavigation().isInProgress()) {
            return true;
        }

        if (mob.getTarget() != null) {
            return true;
        }

        if (mob.isInLava() || mob.isOnFire()) {
            return true;
        }

        if (mob.getHealth() < mob.getMaxHealth() || mob.hurtTime > 0) {
            return true;
        }

        if (mob.onGround() || mob.isInLiquid() || mob.isPassenger()) {
            
            java.util.List<net.minecraft.world.entity.Entity> nearbyEntities =
                org.virgil.akiasync.mixin.brain.core.AiQueryHelper.getNearbyAnyEntities(
                    mob, 3.0, entity ->
                        entity instanceof net.minecraft.world.entity.vehicle.AbstractMinecart
                );

            if (!nearbyEntities.isEmpty()) {
                return true;
            }
        }

        if (mob.isPassenger() || mob.getVehicle() != null) {
            return true;
        }

        if (mob.getMoveControl() != null && mob.getMoveControl().hasWanted()) {
            return true;
        }

        if (mob instanceof net.minecraft.world.entity.monster.Monster) {
            
            java.util.List<net.minecraft.world.entity.player.Player> nearbyPlayers =
                org.virgil.akiasync.mixin.brain.core.AiQueryHelper.getNearbyPlayers(mob, 8.0);

            if (!nearbyPlayers.isEmpty()) {
                return true;
            }

            java.util.List<net.minecraft.world.entity.Entity> nearbyVehicles =
                org.virgil.akiasync.mixin.brain.core.AiQueryHelper.getNearbyAnyEntities(
                    mob, 3.0, entity ->
                        entity instanceof net.minecraft.world.entity.vehicle.AbstractBoat ||
                        entity instanceof net.minecraft.world.entity.animal.horse.AbstractHorse
                );

            if (!nearbyVehicles.isEmpty()) {
                return true;
            }
        }

        if (debugEnabled && totalChecks % 10000 == 0) {
            double protectionRate = (protectionCount * 100.0) / totalChecks;
            BridgeConfigCache.debugLog(
                "[AkiAsync-UniversalAI] Protection stats: %d/%d checks (%.2f%% protected)",
                protectionCount, totalChecks, protectionRate
            );
        }

        return false;
    }
    
    @Unique private static synchronized void aki$init() {
        if (init) return;

        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            isFolia = true;
        } catch (ClassNotFoundException e) {
            isFolia = false;
        }

        org.virgil.akiasync.mixin.bridge.Bridge bridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        enabled = bridge != null && bridge.isUniversalAiOptimizationEnabled();
        timeout = bridge != null ? bridge.getAsyncAITimeoutMicros() : 100;
        enabledEntities = bridge != null ? bridge.getUniversalAiEntities() : java.util.Collections.emptySet();
        respectBrainThrottle = bridge != null && bridge.isBrainThrottleEnabled();
        debugEnabled = BridgeConfigCache.isDebugLoggingEnabled();

        if (bridge != null) {
            dabEnabled = bridge.isDabEnabled();
            dabStartDistance = bridge.getDabStartDistance();
            dabActivationDistMod = bridge.getDabActivationDistMod();
            dabMaxTickInterval = bridge.getDabMaxTickInterval();
            
            brainMemoryEnabled = bridge.isBrainMemoryOptimizationEnabled();
            poiSnapshotEnabled = bridge.isPoiSnapshotEnabled();
        }

        if (isFolia) {
            tickInterval = 2;
            if (bridge != null) {
                BridgeConfigCache.debugLog("[AkiAsync] UniversalAiFamilyTickMixin initialized in Folia mode:");
                BridgeConfigCache.debugLog("  - Enabled: " + enabled);
                BridgeConfigCache.debugLog("  - Tick interval: " + tickInterval + " (reduced from 3 for region parallelism)");
                BridgeConfigCache.debugLog("  - Respect brain throttle: " + respectBrainThrottle);
            }
        } else {
            tickInterval = 3;
            if (bridge != null) {
                BridgeConfigCache.debugLog("[AkiAsync] UniversalAiFamilyTickMixin initialized:");
                BridgeConfigCache.debugLog("  - Enabled: " + enabled);
                BridgeConfigCache.debugLog("  - Base tick interval: " + tickInterval);
                BridgeConfigCache.debugLog("  - DAB enabled: " + dabEnabled);
                if (dabEnabled) {
                    BridgeConfigCache.debugLog("  - DAB start distance: " + dabStartDistance);
                    BridgeConfigCache.debugLog("  - DAB activation dist mod: " + dabActivationDistMod);
                    BridgeConfigCache.debugLog("  - DAB max tick interval: " + dabMaxTickInterval);
                }
                BridgeConfigCache.debugLog("  - Brain memory optimization: " + brainMemoryEnabled);
                BridgeConfigCache.debugLog("  - POI snapshot: " + poiSnapshotEnabled);
            }
        }

        init = true;
    }
}
