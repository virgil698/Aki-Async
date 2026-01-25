package org.virgil.akiasync.bridge.delegates;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.virgil.akiasync.config.ConfigManager;

/**
 * Delegate class handling Jigsaw optimization related bridge methods.
 * Extracted from AkiAsyncBridge to reduce its complexity.
 */
public class JigsawBridgeDelegate {

    private ConfigManager config;

    public JigsawBridgeDelegate(ConfigManager config) {
        this.config = config;
    }

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "ConfigManager is intentionally shared")
    public void updateConfig(ConfigManager newConfig) {
        this.config = newConfig;
    }

    public boolean isJigsawOptimizationEnabled() {
        return config != null && config.isJigsawOptimizationEnabled();
    }

    public void initializeJigsawOctree(net.minecraft.world.phys.AABB bounds) {
        if (config != null) {
            org.virgil.akiasync.mixin.util.worldgen.OctreeHolder.set(
                new org.virgil.akiasync.mixin.util.worldgen.BoxOctree(bounds)
            );
        }
    }

    public boolean hasJigsawOctree() {
        return org.virgil.akiasync.mixin.util.worldgen.OctreeHolder.isSet();
    }

    public void insertIntoJigsawOctree(net.minecraft.world.phys.AABB box) {
        org.virgil.akiasync.mixin.util.worldgen.BoxOctree octree =
            org.virgil.akiasync.mixin.util.worldgen.OctreeHolder.get();
        if (octree != null) {
            octree.insert(box);
        }
    }

    public boolean jigsawOctreeIntersects(net.minecraft.world.phys.AABB box) {
        org.virgil.akiasync.mixin.util.worldgen.BoxOctree octree =
            org.virgil.akiasync.mixin.util.worldgen.OctreeHolder.get();
        return octree != null && octree.intersects(box);
    }

    public void clearJigsawOctree() {
        org.virgil.akiasync.mixin.util.worldgen.OctreeHolder.clear();
    }

    public String getJigsawOctreeStats() {
        org.virgil.akiasync.mixin.util.worldgen.BoxOctree octree =
            org.virgil.akiasync.mixin.util.worldgen.OctreeHolder.get();
        if (octree != null) {
            return octree.getStats().toString();
        }
        return null;
    }
}
