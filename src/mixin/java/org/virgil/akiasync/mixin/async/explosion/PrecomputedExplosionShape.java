package org.virgil.akiasync.mixin.async.explosion;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class PrecomputedExplosionShape {
    
    private static final Vec3[] PRECOMPUTED_RAYS;
    
    private static final BlockOffset[] PRECOMPUTED_BLOCKS;
    
    private static final Int2ObjectOpenHashMap<List<BlockOffset>> BLOCKS_BY_DISTANCE;
    
    static {
        
        List<Vec3> rays = new ArrayList<>();
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    double dx = (x / 15.0 * 2.0 - 1.0);
                    double dy = (y / 15.0 * 2.0 - 1.0);
                    double dz = (z / 15.0 * 2.0 - 1.0);
                    
                    double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    if (length > 0) {
                        rays.add(new Vec3(dx / length, dy / length, dz / length));
                    }
                }
            }
        }
        PRECOMPUTED_RAYS = rays.toArray(new Vec3[0]);
        
        List<BlockOffset> blocks = new ArrayList<>();
        BLOCKS_BY_DISTANCE = new Int2ObjectOpenHashMap<>();
        
        for (int x = -4; x <= 4; x++) {
            for (int y = -4; y <= 4; y++) {
                for (int z = -4; z <= 4; z++) {
                    double distance = Math.sqrt(x * x + y * y + z * z);
                    if (distance <= 4.0) {
                        BlockOffset offset = new BlockOffset(x, y, z, (float) distance);
                        blocks.add(offset);
                        
                        int distKey = (int) (distance * 10); 
                        BLOCKS_BY_DISTANCE.computeIfAbsent(distKey, k -> new ArrayList<>()).add(offset);
                    }
                }
            }
        }
        
        blocks.sort((a, b) -> Float.compare(a.distance, b.distance));
        PRECOMPUTED_BLOCKS = blocks.toArray(new BlockOffset[0]);
    }
    
    public static class BlockOffset {
        public final int x, y, z;
        public final float distance;
        
        public BlockOffset(int x, int y, int z, float distance) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.distance = distance;
        }
        
        public BlockPos apply(BlockPos center) {
            return center.offset(x, y, z);
        }
    }
    
    public static Vec3[] getPrecomputedRays() {
        return PRECOMPUTED_RAYS;
    }
    
    public static BlockOffset[] getPrecomputedBlocks() {
        return PRECOMPUTED_BLOCKS;
    }
    
    public static BlockOffset[] getBlocksForPower(float power) {
        if (power <= 0) {
            return new BlockOffset[0];
        }
        
        if (power == 1.0f) {
            return PRECOMPUTED_BLOCKS;
        }
        
        float maxDistance = 4.0f * power;
        List<BlockOffset> result = new ArrayList<>();
        
        for (BlockOffset offset : PRECOMPUTED_BLOCKS) {
            float scaledDistance = offset.distance / power;
            if (scaledDistance <= 4.0f) {
                result.add(offset);
            }
        }
        
        return result.toArray(new BlockOffset[0]);
    }
    
    public static boolean isOccluded(
            double centerX, double centerY, double centerZ,
            double blockAX, double blockAY, double blockAZ,
            double blockBX, double blockBY, double blockBZ,
            double threshold) {
        
        double cax = blockAX - centerX;
        double cay = blockAY - centerY;
        double caz = blockAZ - centerZ;
        
        double cbx = blockBX - centerX;
        double cby = blockBY - centerY;
        double cbz = blockBZ - centerZ;
        
        double distA = cax * cax + cay * cay + caz * caz;
        double distB = cbx * cbx + cby * cby + cbz * cbz;
        if (distB < distA) {
            return false;
        }
        
        double crossX = cay * cbz - caz * cby;
        double crossY = caz * cbx - cax * cbz;
        double crossZ = cax * cby - cay * cbx;
        double crossLength = Math.sqrt(crossX * crossX + crossY * crossY + crossZ * crossZ);
        double caLength = Math.sqrt(distA);
        
        if (caLength < 1e-6) {
            return false; 
        }
        
        double distance = crossLength / caLength;
        
        return distance < threshold;
    }
    
    public static float calculateAttenuation(
            double centerX, double centerY, double centerZ,
            double blockX, double blockY, double blockZ,
            double rayDirX, double rayDirY, double rayDirZ) {
        
        double cbx = blockX - centerX;
        double cby = blockY - centerY;
        double cbz = blockZ - centerZ;
        
        double crossX = rayDirY * cbz - rayDirZ * cby;
        double crossY = rayDirZ * cbx - rayDirX * cbz;
        double crossZ = rayDirX * cby - rayDirY * cbx;
        double distance = Math.sqrt(crossX * crossX + crossY * crossY + crossZ * crossZ);
        
        return (float) Math.exp(-distance * 0.5);
    }
    
    public static String getStats() {
        return String.format("Rays: %d, Blocks: %d, DistanceGroups: %d",
            PRECOMPUTED_RAYS.length,
            PRECOMPUTED_BLOCKS.length,
            BLOCKS_BY_DISTANCE.size());
    }
}
