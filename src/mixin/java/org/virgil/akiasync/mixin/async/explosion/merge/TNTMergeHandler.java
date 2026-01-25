package org.virgil.akiasync.mixin.async.explosion.merge;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.virgil.akiasync.mixin.bridge.Bridge;
import org.virgil.akiasync.mixin.bridge.BridgeManager;

import java.util.ArrayList;
import java.util.List;

public class TNTMergeHandler {

    private static double getMergeRadius() {
        Bridge bridge = BridgeManager.getBridge();
        return bridge != null ? bridge.getTNTMergeRadius() : 1.5;
    }

    private static int getMaxFuseDifference() {
        Bridge bridge = BridgeManager.getBridge();
        return bridge != null ? bridge.getTNTMaxFuseDifference() : 5;
    }

    public static List<PrimedTnt> mergeNearbyTNT(ServerLevel level, PrimedTnt source) {
        List<PrimedTnt> toRemove = new ArrayList<>();

        if (!(source instanceof MergeableTNT mergeable)) {
            return toRemove;
        }

        double mergeRadius = getMergeRadius();
        Vec3 sourcePos = source.position();
        AABB searchBox = new AABB(
            sourcePos.x - mergeRadius, sourcePos.y - mergeRadius, sourcePos.z - mergeRadius,
            sourcePos.x + mergeRadius, sourcePos.y + mergeRadius, sourcePos.z + mergeRadius
        );

        List<PrimedTnt> nearbyTNT = level.getEntitiesOfClass(PrimedTnt.class, searchBox);

        for (PrimedTnt other : nearbyTNT) {
            if (other == source || other.isRemoved()) {
                continue;
            }

            if (mergeable.aki$canMergeWith(other)) {
                mergeable.aki$mergeWith(other);
                toRemove.add(other);
            }
        }

        return toRemove;
    }

    public static boolean canMerge(PrimedTnt tnt1, PrimedTnt tnt2) {
        if (tnt1 == tnt2 || tnt1.isRemoved() || tnt2.isRemoved()) {
            return false;
        }

        int fuseDiff = Math.abs(tnt1.getFuse() - tnt2.getFuse());
        if (fuseDiff > getMaxFuseDifference()) {
            return false;
        }

        double distance = tnt1.position().distanceTo(tnt2.position());
        return distance <= getMergeRadius();
    }

    public static float calculateMergedPower(int mergeCount) {
        Bridge bridge = BridgeManager.getBridge();

        float multiplier = bridge != null ? bridge.getTNTMergedPowerMultiplier() : 0.5f;
        float maxPower = bridge != null ? bridge.getTNTMaxPower() : 12.0f;

        return Math.min(4.0f + (mergeCount - 1) * multiplier, maxPower);
    }

    public static int calculateExplosionCycles(int mergeCount) {

        if (mergeCount <= 1) return 1;
        if (mergeCount <= 5) return 2;
        if (mergeCount <= 10) return 3;
        return Math.min(4, 1 + mergeCount / 5);
    }
}
