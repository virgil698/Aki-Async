package org.virgil.akiasync.mixin.async.explosion.merge;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;


public class TNTMergeHandler {
    private static final double MERGE_RADIUS = 1.5;
    private static final int MAX_FUSE_DIFFERENCE = 5;

    
    public static List<PrimedTnt> mergeNearbyTNT(ServerLevel level, PrimedTnt source) {
        List<PrimedTnt> toRemove = new ArrayList<>();
        
        if (!(source instanceof MergeableTNT mergeable)) {
            return toRemove;
        }

        Vec3 sourcePos = source.position();
        AABB searchBox = new AABB(
            sourcePos.x - MERGE_RADIUS, sourcePos.y - MERGE_RADIUS, sourcePos.z - MERGE_RADIUS,
            sourcePos.x + MERGE_RADIUS, sourcePos.y + MERGE_RADIUS, sourcePos.z + MERGE_RADIUS
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
        if (fuseDiff > MAX_FUSE_DIFFERENCE) {
            return false;
        }


        double distance = tnt1.position().distanceTo(tnt2.position());
        return distance <= MERGE_RADIUS;
    }

    
    public static float calculateMergedPower(int mergeCount) {
        org.virgil.akiasync.mixin.bridge.Bridge bridge = 
            org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        
        float multiplier = bridge != null ? bridge.getTNTMergedPowerMultiplier() : 0.5f;
        


        return Math.min(4.0f + (mergeCount - 1) * multiplier, 12.0f);
    }
    
    
    public static int calculateExplosionCycles(int mergeCount) {

        if (mergeCount <= 1) return 1;
        if (mergeCount <= 5) return 2;
        if (mergeCount <= 10) return 3;
        return Math.min(4, 1 + mergeCount / 5);
    }
}
