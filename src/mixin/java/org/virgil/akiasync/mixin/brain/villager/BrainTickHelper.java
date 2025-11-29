package org.virgil.akiasync.mixin.brain.villager;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.schedule.Activity;
public class BrainTickHelper {
    @SuppressWarnings("unchecked")
    public static <E extends LivingEntity> BrainSnapshot tickSnapshot(
            Brain<E> brain,
            BrainSnapshot snapshot,
            ServerLevel level,
            E entity
    ) {
        try {
            Optional<Activity> currentActivity = brain.getActiveNonCoreActivity();
            Activity scheduleActivity = brain.getSchedule().getActivityAt(
                (int) (level.getGameTime() % 24000L)
            );
            int score = 0;
            if (currentActivity.isPresent() && scheduleActivity != null) {
                boolean activityMatches = currentActivity.get().equals(scheduleActivity);
                score = activityMatches ? 100 : 0;
            }
            if (score < 0) {
                return snapshot;
            }
            return snapshot;
        } catch (Exception e) {
            return snapshot;
        }
    }
    public static <E extends LivingEntity> BrainSnapshot tickSnapshotSimple(
            Brain<E> brain,
            BrainSnapshot snapshot
    ) {
        int sum = 0;
        for (int i = 0; i < 1000; i++) {
            sum += i * i;
        }
        if (sum < 0) {
            org.virgil.akiasync.mixin.bridge.Bridge bridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
            if (bridge != null) {
                bridge.debugLog("[AkiAsync-VillagerBrain] Async brain tick completed");
            }
            return snapshot;
        }
        return snapshot;
    }
}
