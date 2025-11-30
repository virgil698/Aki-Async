package org.virgil.akiasync.mixin.mixins.entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.Villager;
@SuppressWarnings("unused")
@Mixin(value = Villager.class, priority = 1200)
public class VillagerBreedAsyncMixin {
    @Shadow private int updateMerchantTimer;
    @Shadow private boolean increaseProfessionLevelOnUpdate;

    @Unique private static volatile boolean cached_enabled;
    @Unique private static volatile boolean cached_ageThrottle;
    @Unique private static volatile int cached_interval;
    @Unique private static volatile boolean initialized = false;

    @Inject(method = "customServerAiStep", at = @At("HEAD"), cancellable = true)
    private void aki$optimizedVillagerStep(ServerLevel level, CallbackInfo ci) {
        try {
            if (!initialized) { aki$initVillagerOptimization(); }
            if (!cached_enabled) return;

            Villager villager = (Villager) (Object) this;
            if (villager == null || level == null) {
                return;
            }

        if (this.updateMerchantTimer > 0) {
            return;
        }

        if (villager.isTrading()) {
            return;
        }

        if (this.increaseProfessionLevelOnUpdate) {
            return;
        }

        if (aki$hasImportantState(villager)) {
            return;
        }

        if (aki$isInBreedingMode(villager)) {
            org.virgil.akiasync.mixin.bridge.Bridge bridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
            if (bridge != null) {
                bridge.debugLog("[AkiAsync-Breed] Villager in breeding mode, skipping optimization");
            }
            return;
        }

        BlockPos pos = villager.blockPosition();
        long currentTick = level.getGameTime();

        if (cached_ageThrottle) {
            if (org.virgil.akiasync.mixin.async.villager.VillagerBreedExecutor
                    .isIdle(villager.getUUID(), pos, currentTick)) {
                ci.cancel();
                return;
            }
        }

        if (currentTick % cached_interval != 0) {
            ci.cancel();
            return;
        }

        net.minecraft.world.entity.npc.VillagerData vData = villager.getVillagerData();
        if (vData == null) {
            return;
        }

        if (this.updateMerchantTimer <= 0 && !this.increaseProfessionLevelOnUpdate &&
            net.minecraft.world.entity.npc.VillagerData.canLevelUp(vData.level()) &&
            villager.getVillagerXp() >= net.minecraft.world.entity.npc.VillagerData.getMaxXpPerLevel(vData.level())) {

            org.virgil.akiasync.mixin.bridge.Bridge bridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
            if (bridge != null) {
                bridge.debugLog("[AkiAsync-Debug] Force triggering upgrade for villager with XP=" +
                               villager.getVillagerXp() + " Level=" + vData.level());
            }
            try {
                java.lang.reflect.Method increaseMethod = villager.getClass().getDeclaredMethod("increaseMerchantCareer");
                increaseMethod.setAccessible(true);
                increaseMethod.invoke(villager);

                aki$forceRefreshTrades(villager);

            } catch (NoSuchMethodException e) {
                org.virgil.akiasync.mixin.bridge.Bridge errorBridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
                if (errorBridge != null) {
                    errorBridge.errorLog("[AkiAsync-VillagerBreed] Method not found: " + e.getMessage());
                }
            } catch (IllegalAccessException | java.lang.reflect.InvocationTargetException e) {
                org.virgil.akiasync.mixin.bridge.Bridge errorBridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
                if (errorBridge != null) {
                    errorBridge.errorLog("[AkiAsync-VillagerBreed] Reflection error: " + e.getMessage());
                }
            } catch (Exception e) {
                org.virgil.akiasync.mixin.bridge.Bridge errorBridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
                if (errorBridge != null) {
                    errorBridge.errorLog("[AkiAsync-VillagerBreed] Unexpected error: " + e.getMessage());
                }
            }
        }
        } catch (Exception e) {
            org.virgil.akiasync.mixin.bridge.Bridge errorBridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
            if (errorBridge != null) {
                errorBridge.errorLog("[AkiAsync-VillagerBreed] Critical error in optimizedVillagerStep: " + e.getMessage());
            }
        }
    }

    @Inject(method = "customServerAiStep", at = @At("RETURN"))
    private void aki$afterVillagerStep(ServerLevel level, CallbackInfo ci) {
        try {
            if (!initialized) return;
            if (!cached_enabled) return;

            Villager villager = (Villager) (Object) this;
            if (villager == null) return;

        if (this.updateMerchantTimer == 0 && this.increaseProfessionLevelOnUpdate) {
            org.virgil.akiasync.mixin.bridge.Bridge bridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
            if (bridge != null) {
                bridge.debugLog("[AkiAsync-VillagerBreed] Async task completed for villager");
            }
            aki$forceRefreshTrades(villager);
        }
        } catch (Exception e) {
            org.virgil.akiasync.mixin.bridge.Bridge errorBridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
            if (errorBridge != null) {
                errorBridge.errorLog("[AkiAsync-VillagerBreed] Error in afterVillagerStep: " + e.getMessage());
            }
        }
    }

    @Inject(method = "increaseMerchantCareer", at = @At("RETURN"))
    private void aki$afterUpgrade(CallbackInfo ci) {
        try {
            if (!initialized) return;
            if (!cached_enabled) return;

            Villager villager = (Villager) (Object) this;
            if (villager == null) return;
        org.virgil.akiasync.mixin.bridge.Bridge bridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        if (bridge != null) {
            bridge.debugLog("[AkiAsync-Debug] Village upgraded! Level=" + villager.getVillagerData().level() +
                          " XP=" + villager.getVillagerXp());
        }

        aki$forceRefreshTrades(villager);
        } catch (Exception e) {
            org.virgil.akiasync.mixin.bridge.Bridge errorBridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
            if (errorBridge != null) {
                errorBridge.errorLog("[AkiAsync-VillagerBreed] Error in afterUpgrade: " + e.getMessage());
            }
        }
    }

    @Unique
    private java.util.Optional<?> aki$safeGetMemory(net.minecraft.world.entity.ai.Brain<?> brain, net.minecraft.world.entity.ai.memory.MemoryModuleType<?> memoryType) {
        try {
            return brain.getMemoryInternal(memoryType);
        } catch (Exception e) {
            return null;
        }
    }

    @Unique
    private boolean aki$hasImportantState(Villager villager) {
        try {
            if (villager == null) return true;

            net.minecraft.world.entity.npc.VillagerData villagerData = villager.getVillagerData();
            if (villagerData == null || villagerData.profession() == null) {
                return true;
            }

            if (villagerData.profession().is(net.minecraft.world.entity.npc.VillagerProfession.NONE)) {
                return true;
            }

        if (villager.getUnhappyCounter() > 0) {
            return true;
        }

        if (villager.isBaby()) {
            return true;
        }

        if (villager.shouldRestock()) {
            return true;
        }

        if (villager.getVillagerData().profession().is(net.minecraft.world.entity.npc.VillagerProfession.FARMER)) {
            return true;
        }

        if (aki$hasItemsToShare(villager)) {
            org.virgil.akiasync.mixin.bridge.Bridge bridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
            if (bridge != null) {
                bridge.debugLog("[AkiAsync-Food] Villager has items to share, preserving AI");
            }
            return true;
        }

        int currentLevel = villagerData.level();
        int villagerXp = villager.getVillagerXp();
        if (net.minecraft.world.entity.npc.VillagerData.canLevelUp(currentLevel) &&
            villagerXp >= net.minecraft.world.entity.npc.VillagerData.getMaxXpPerLevel(currentLevel)) {
            org.virgil.akiasync.mixin.bridge.Bridge bridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
            if (bridge != null) {
                bridge.debugLog("[AkiAsync-Debug] Villager should upgrade: Level=" + currentLevel +
                             " XP=" + villagerXp + " Required=" +
                             net.minecraft.world.entity.npc.VillagerData.getMaxXpPerLevel(currentLevel) +
                             " Timer=" + this.updateMerchantTimer + " Flag=" + this.increaseProfessionLevelOnUpdate);
            }
            return true;
        }

        net.minecraft.world.entity.ai.Brain<?> brain = villager.getBrain();
        if (brain == null) {
            return true;
        }

        net.minecraft.world.level.Level level = villager.level();
        if (level == null) {
            return true;
        }
        long currentTime = level.getGameTime();

        java.util.Optional<?> jobSite = aki$safeGetMemory(brain, net.minecraft.world.entity.ai.memory.MemoryModuleType.JOB_SITE);
        if (jobSite != null && jobSite.isPresent()) {
            java.util.Optional<?> lastWorked = aki$safeGetMemory(brain, net.minecraft.world.entity.ai.memory.MemoryModuleType.LAST_WORKED_AT_POI);
            if (lastWorked == null || lastWorked.isEmpty() || (lastWorked.get() instanceof Long && (currentTime - (Long)lastWorked.get()) > 600L)) {
                return true;
            }
        }

        java.util.Optional<?> potentialJobSite = aki$safeGetMemory(brain, net.minecraft.world.entity.ai.memory.MemoryModuleType.POTENTIAL_JOB_SITE);
        if (potentialJobSite != null && potentialJobSite.isPresent()) {
            return true;
        }

        java.util.Optional<?> interactionTarget = aki$safeGetMemory(brain, net.minecraft.world.entity.ai.memory.MemoryModuleType.INTERACTION_TARGET);
        if (interactionTarget != null && interactionTarget.isPresent()) {
            return true;
        }

        java.util.Optional<?> breedTarget = aki$safeGetMemory(brain, net.minecraft.world.entity.ai.memory.MemoryModuleType.BREED_TARGET);
        if (breedTarget != null && breedTarget.isPresent()) {
            org.virgil.akiasync.mixin.bridge.Bridge bridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
            if (bridge != null) {
                bridge.debugLog("[AkiAsync-Breed] Villager has breed target, preserving AI");
            }
            return true;
        }

        java.util.Optional<?> meetingPoint = aki$safeGetMemory(brain, net.minecraft.world.entity.ai.memory.MemoryModuleType.MEETING_POINT);
        if (meetingPoint != null && meetingPoint.isPresent()) {
            return true;
        }

        java.util.Optional<?> home = aki$safeGetMemory(brain, net.minecraft.world.entity.ai.memory.MemoryModuleType.HOME);
        if (home != null && home.isPresent()) {
            java.util.Optional<?> lastSlept = aki$safeGetMemory(brain, net.minecraft.world.entity.ai.memory.MemoryModuleType.LAST_SLEPT);
            if (lastSlept == null || lastSlept.isEmpty() || (lastSlept.get() instanceof Long && (currentTime - (Long)lastSlept.get()) > 24000L)) {
                return true;
            }
        }

        java.util.Optional<?> walkTarget = aki$safeGetMemory(brain, net.minecraft.world.entity.ai.memory.MemoryModuleType.WALK_TARGET);
        if (walkTarget != null && walkTarget.isPresent()) {
            return true;
        }

        java.util.Set<net.minecraft.world.entity.schedule.Activity> activeActivities = brain.getActiveActivities();
        if (activeActivities.contains(net.minecraft.world.entity.schedule.Activity.WORK) ||
            activeActivities.contains(net.minecraft.world.entity.schedule.Activity.MEET) ||
            activeActivities.contains(net.minecraft.world.entity.schedule.Activity.PANIC) ||
            activeActivities.contains(net.minecraft.world.entity.schedule.Activity.REST) ||
            activeActivities.contains(net.minecraft.world.entity.schedule.Activity.PLAY)) {

            if (activeActivities.contains(net.minecraft.world.entity.schedule.Activity.PANIC)) {
                org.virgil.akiasync.mixin.bridge.Bridge bridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
                if (bridge != null) {
                    bridge.debugLog("[AkiAsync-Panic] Villager in panic state, preserving AI for safety");
                }
            }
            return true;
        }

        try {
            java.util.Optional<?> avoidTarget = brain.getMemoryInternal(net.minecraft.world.entity.ai.memory.MemoryModuleType.AVOID_TARGET);
            if (avoidTarget != null && avoidTarget.isPresent()) {
                org.virgil.akiasync.mixin.bridge.Bridge bridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
                if (bridge != null) {
                    bridge.debugLog("[AkiAsync-Avoid] Villager has avoid target, preserving AI for safety");
                }
                return true;
            }
        } catch (Exception e) {
            org.virgil.akiasync.mixin.bridge.Bridge bridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
            if (bridge != null && bridge.isDebugLoggingEnabled()) {
                bridge.debugLog("[AkiAsync-Villager] Failed to check avoid target: " + e.getMessage());
            }
        }

        if (villager.canBreed()) {
            org.virgil.akiasync.mixin.bridge.Bridge bridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
            if (bridge != null) {
                bridge.debugLog("[AkiAsync-Breed] Villager can breed, preserving AI");
            }
            return true;
        }

        return false;
        } catch (Exception e) {
            org.virgil.akiasync.mixin.bridge.Bridge errorBridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
            if (errorBridge != null) {
                errorBridge.errorLog("[AkiAsync-VillagerBreed] Error in hasImportantState: " + e.getMessage());
            }
            return true;
        }
    }

    @Unique
    private boolean aki$isInBreedingMode(Villager villager) {
        try {
            if (villager == null) return false;

            net.minecraft.world.entity.ai.Brain<?> brain = villager.getBrain();
            if (brain == null) {
                return false;
            }

        java.util.Optional<?> breedTarget = aki$safeGetMemory(brain, net.minecraft.world.entity.ai.memory.MemoryModuleType.BREED_TARGET);
        if (breedTarget != null && breedTarget.isPresent()) {
            return true;
        }

        java.util.Set<net.minecraft.world.entity.schedule.Activity> activeActivities = brain.getActiveActivities();
        if (activeActivities.contains(net.minecraft.world.entity.schedule.Activity.MEET)) {
            return true;
        }

        if (villager.canBreed()) {
            java.util.List<Villager> nearbyVillagers = villager.level().getEntitiesOfClass(
                Villager.class,
                villager.getBoundingBox().inflate(8.0),
                v -> v != villager && v.canBreed() && !v.isBaby()
            );
            if (!nearbyVillagers.isEmpty()) {
                org.virgil.akiasync.mixin.bridge.Bridge bridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
                if (bridge != null) {
                    bridge.debugLog("[AkiAsync-Breed] Villager can breed and has nearby partners (" + nearbyVillagers.size() + ")");
                }
                return true;
            }
        }

        java.util.Optional<?> home = aki$safeGetMemory(brain, net.minecraft.world.entity.ai.memory.MemoryModuleType.HOME);
        if (home != null && home.isPresent() && villager.canBreed()) {
            return true;
        }

        java.util.Optional<?> walkTarget = aki$safeGetMemory(brain, net.minecraft.world.entity.ai.memory.MemoryModuleType.WALK_TARGET);
        if (walkTarget != null && walkTarget.isPresent() && villager.canBreed()) {
            return true;
        }

        return false;
        } catch (Exception e) {
            org.virgil.akiasync.mixin.bridge.Bridge errorBridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
            if (errorBridge != null) {
                errorBridge.errorLog("[AkiAsync-VillagerBreed] Error in isInBreedingMode: " + e.getMessage());
            }
            return false;
        }
    }

    @Unique
    private boolean aki$hasItemsToShare(Villager villager) {
        try {
            if (villager == null) return false;

            net.minecraft.world.SimpleContainer inventory = villager.getInventory();
            if (inventory == null) {
                return false;
            }
        boolean isFarmer = villager.getVillagerData().profession().is(net.minecraft.world.entity.npc.VillagerProfession.FARMER);

        for (int i = 0; i < inventory.getContainerSize(); i++) {
            net.minecraft.world.item.ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty()) continue;

            net.minecraft.world.item.Item item = stack.getItem();
            int count = stack.getCount();

            if (item == net.minecraft.world.item.Items.BREAD && count >= 6) {
                return true;
            } else if ((item == net.minecraft.world.item.Items.CARROT ||
                       item == net.minecraft.world.item.Items.POTATO ||
                       item == net.minecraft.world.item.Items.BEETROOT) && count >= 24) {
                return true;
            } else if (isFarmer && item == net.minecraft.world.item.Items.WHEAT && count >= 18) {
                return true;
            }
        }

        return false;
        } catch (Exception e) {
            org.virgil.akiasync.mixin.bridge.Bridge errorBridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
            if (errorBridge != null) {
                errorBridge.errorLog("[AkiAsync-VillagerBreed] Error in hasItemsToShare: " + e.getMessage());
            }
            return false;
        }
    }

    @Unique
    private void aki$forceRefreshTrades(Villager villager) {
        try {
            java.lang.reflect.Method updateTradesMethod = villager.getClass().getDeclaredMethod("updateTrades");
            updateTradesMethod.setAccessible(true);
            updateTradesMethod.invoke(villager);

            net.minecraft.world.entity.player.Player tradingPlayer = villager.getTradingPlayer();
            if (tradingPlayer != null) {
                java.lang.reflect.Method resendMethod = villager.getClass().getDeclaredMethod("resendOffersToTradingPlayer");
                resendMethod.setAccessible(true);
                resendMethod.invoke(villager);

                org.virgil.akiasync.mixin.bridge.Bridge bridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
                if (bridge != null) {
                    bridge.debugLog("[AkiAsync-Debug] Immediately refreshed trades for trading player");
                }
            }

            org.virgil.akiasync.mixin.bridge.Bridge bridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
            if (bridge != null) {
                bridge.debugLog("[AkiAsync-Debug] Trades refreshed immediately after upgrade");
            }

        } catch (Exception e) {
            org.virgil.akiasync.mixin.bridge.Bridge bridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
            if (bridge != null) {
                bridge.errorLog("[AkiAsync-Debug] Failed to refresh trades: " + e.getMessage());
            }
            try {
                java.lang.reflect.Field offersField = villager.getClass().getSuperclass().getDeclaredField("offers");
                offersField.setAccessible(true);
                offersField.set(villager, null);
                org.virgil.akiasync.mixin.bridge.Bridge fallbackBridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
                if (fallbackBridge != null) {
                    fallbackBridge.debugLog("[AkiAsync-Debug] Used fallback trade refresh method");
                }
            } catch (Exception e2) {
                org.virgil.akiasync.mixin.bridge.Bridge errorBridge2 = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
                if (errorBridge2 != null) {
                    errorBridge2.errorLog("[AkiAsync-Debug] Fallback trade refresh also failed: " + e2.getMessage());
                }
            }
        }
    }

    @Unique
    private static long lastInitTime = 0;

    @Unique
    private static synchronized void aki$initVillagerOptimization() {
        if (initialized) return;

        org.virgil.akiasync.mixin.bridge.Bridge bridge =
            org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        if (bridge != null) {
            cached_enabled = bridge.isAsyncVillagerBreedEnabled();
            cached_ageThrottle = bridge.isVillagerAgeThrottleEnabled();
            cached_interval = bridge.getVillagerBreedCheckInterval();
        } else {
            cached_enabled = false;
            cached_ageThrottle = false;
            cached_interval = 5;
        }
        initialized = true;
        lastInitTime = System.currentTimeMillis();

        if (bridge != null) {
            bridge.debugLog("[AkiAsync] VillagerBreedAsyncMixin initialized (upgrade-safe): enabled=" + cached_enabled + " | ageThrottle=" + cached_ageThrottle + " | interval=" + cached_interval);
        }
    }

    @Unique
    private static synchronized void aki$resetInitialization() {
        org.virgil.akiasync.mixin.bridge.Bridge bridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        if (bridge != null) {
            bridge.debugLog("[AkiAsync-Debug] Resetting VillagerBreedAsyncMixin initialization");
        }
        initialized = false;
        cached_enabled = false;
        cached_ageThrottle = false;
        cached_interval = 5;
    }
}
