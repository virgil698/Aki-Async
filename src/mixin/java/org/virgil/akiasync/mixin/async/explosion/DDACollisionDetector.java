package org.virgil.akiasync.mixin.async.explosion;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class DDACollisionDetector {

    private final BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

    public boolean hasCollision(
            OptimizedExplosionCache cache,
            double startX, double startY, double startZ,
            double endX, double endY, double endZ) {

        double dx = endX - startX;
        double dy = endY - startY;
        double dz = endZ - startZ;

        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (distance < 0.01) {
            return false;
        }

        dx /= distance;
        dy /= distance;
        dz /= distance;

        double currentX = startX;
        double currentY = startY;
        double currentZ = startZ;

        int blockX = (int) Math.floor(currentX);
        int blockY = (int) Math.floor(currentY);
        int blockZ = (int) Math.floor(currentZ);

        int endBlockX = (int) Math.floor(endX);
        int endBlockY = (int) Math.floor(endY);
        int endBlockZ = (int) Math.floor(endZ);

        int stepX = dx > 0 ? 1 : (dx < 0 ? -1 : 0);
        int stepY = dy > 0 ? 1 : (dy < 0 ? -1 : 0);
        int stepZ = dz > 0 ? 1 : (dz < 0 ? -1 : 0);

        double tMaxX = computeTMax(currentX, dx, stepX);
        double tMaxY = computeTMax(currentY, dy, stepY);
        double tMaxZ = computeTMax(currentZ, dz, stepZ);

        double tDeltaX = stepX != 0 ? Math.abs(1.0 / dx) : Double.MAX_VALUE;
        double tDeltaY = stepY != 0 ? Math.abs(1.0 / dy) : Double.MAX_VALUE;
        double tDeltaZ = stepZ != 0 ? Math.abs(1.0 / dz) : Double.MAX_VALUE;

        int maxSteps = 200;
        for (int step = 0; step < maxSteps; step++) {

            mutablePos.set(blockX, blockY, blockZ);

            if (checkBlockCollision(cache, mutablePos)) {
                return true;
            }

            if (blockX == endBlockX && blockY == endBlockY && blockZ == endBlockZ) {
                break;
            }

            if (tMaxX < tMaxY) {
                if (tMaxX < tMaxZ) {
                    blockX += stepX;
                    tMaxX += tDeltaX;
                } else {
                    blockZ += stepZ;
                    tMaxZ += tDeltaZ;
                }
            } else {
                if (tMaxY < tMaxZ) {
                    blockY += stepY;
                    tMaxY += tDeltaY;
                } else {
                    blockZ += stepZ;
                    tMaxZ += tDeltaZ;
                }
            }
        }

        return false;
    }

    private double computeTMax(double current, double direction, int step) {
        if (step == 0) {
            return Double.MAX_VALUE;
        }

        double blockCoord = Math.floor(current);
        double boundary = step > 0 ? blockCoord + 1.0 : blockCoord;
        return (boundary - current) / direction;
    }

    private boolean checkBlockCollision(OptimizedExplosionCache cache, BlockPos pos) {
        BlockState state = cache.getBlockState(pos);

        if (state.isAir()) {
            return false;
        }

        float resistance = cache.getResistance(pos);

        if (!cache.getFluidState(pos).isEmpty()) {

            return resistance > 0.5f;
        }

        return resistance > 0.5f;
    }

    public boolean hasCollision(OptimizedExplosionCache cache, Vec3 start, Vec3 end) {
        return hasCollision(cache,
            start.x, start.y, start.z,
            end.x, end.y, end.z);
    }
}
