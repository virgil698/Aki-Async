package org.virgil.akiasync.mixin.util.worldgen;

import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

public class BoxOctree {
    private final AABB bounds;
    private final List<AABB> boxes;
    private BoxOctree[] children;
    private final int depth;

    private static final int MAX_BOXES_PER_NODE = 8;
    private static final int MAX_DEPTH = 8;

    public BoxOctree(AABB bounds) {
        this(bounds, 0);
    }

    private BoxOctree(AABB bounds, int depth) {
        this.bounds = bounds;
        this.boxes = new ArrayList<>();
        this.children = null;
        this.depth = depth;
    }

    public boolean intersects(AABB box) {

        if (!bounds.intersects(box)) {
            return false;
        }

        for (AABB existing : boxes) {
            if (existing.intersects(box)) {
                return true;
            }
        }

        if (children != null) {
            for (BoxOctree child : children) {
                if (child != null && child.intersects(box)) {
                    return true;
                }
            }
        }

        return false;
    }

    public void insert(AABB box) {

        if (!bounds.intersects(box)) {
            return;
        }

        if (boxes.size() < MAX_BOXES_PER_NODE || depth >= MAX_DEPTH) {
            boxes.add(box);
            return;
        }

        if (children == null) {
            subdivide();

            List<AABB> toMove = new ArrayList<>(boxes);
            boxes.clear();

            for (AABB existing : toMove) {
                insertIntoChildren(existing);
            }
        }

        insertIntoChildren(box);
    }

    private void insertIntoChildren(AABB box) {
        boolean inserted = false;

        for (BoxOctree child : children) {
            if (child != null && child.bounds.intersects(box)) {
                child.insert(box);
                inserted = true;
            }
        }

        if (!inserted) {
            boxes.add(box);
        }
    }

    private void subdivide() {
        double minX = bounds.minX;
        double minY = bounds.minY;
        double minZ = bounds.minZ;
        double maxX = bounds.maxX;
        double maxY = bounds.maxY;
        double maxZ = bounds.maxZ;

        double midX = (minX + maxX) / 2.0;
        double midY = (minY + maxY) / 2.0;
        double midZ = (minZ + maxZ) / 2.0;

        children = new BoxOctree[8];

        children[0] = new BoxOctree(new AABB(minX, minY, minZ, midX, midY, midZ), depth + 1);
        children[1] = new BoxOctree(new AABB(midX, minY, minZ, maxX, midY, midZ), depth + 1);
        children[2] = new BoxOctree(new AABB(minX, midY, minZ, midX, maxY, midZ), depth + 1);
        children[3] = new BoxOctree(new AABB(midX, midY, minZ, maxX, maxY, midZ), depth + 1);
        children[4] = new BoxOctree(new AABB(minX, minY, midZ, midX, midY, maxZ), depth + 1);
        children[5] = new BoxOctree(new AABB(midX, minY, midZ, maxX, midY, maxZ), depth + 1);
        children[6] = new BoxOctree(new AABB(minX, midY, midZ, midX, maxY, maxZ), depth + 1);
        children[7] = new BoxOctree(new AABB(midX, midY, midZ, maxX, maxY, maxZ), depth + 1);
    }

    public void clear() {
        boxes.clear();
        if (children != null) {
            for (BoxOctree child : children) {
                if (child != null) {
                    child.clear();
                }
            }
            children = null;
        }
    }

    public int size() {
        int count = boxes.size();

        if (children != null) {
            for (BoxOctree child : children) {
                if (child != null) {
                    count += child.size();
                }
            }
        }

        return count;
    }

    public OctreeStats getStats() {
        OctreeStats stats = new OctreeStats();
        collectStats(stats);
        return stats;
    }

    private void collectStats(OctreeStats stats) {
        stats.nodeCount++;
        stats.totalBoxes += boxes.size();

        if (children != null) {
            stats.subdivisionCount++;
            for (BoxOctree child : children) {
                if (child != null) {
                    child.collectStats(stats);
                }
            }
        } else {
            stats.leafNodeCount++;
        }

        stats.maxDepth = Math.max(stats.maxDepth, depth);
    }

    public static class OctreeStats {
        public int nodeCount = 0;
        public int leafNodeCount = 0;
        public int subdivisionCount = 0;
        public int totalBoxes = 0;
        public int maxDepth = 0;

        @Override
        public String toString() {
            return String.format(
                "OctreeStats{nodes=%d, leaves=%d, subdivisions=%d, boxes=%d, maxDepth=%d}",
                nodeCount, leafNodeCount, subdivisionCount, totalBoxes, maxDepth
            );
        }
    }
}
