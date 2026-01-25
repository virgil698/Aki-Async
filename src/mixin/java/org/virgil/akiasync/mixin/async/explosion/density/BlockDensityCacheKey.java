package org.virgil.akiasync.mixin.async.explosion.density;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import java.util.Objects;

public final class BlockDensityCacheKey {
    private final Vec3 explosionPos;
    private final BlockPos entityPos;
    private final int hashCode;

    public BlockDensityCacheKey(Vec3 explosionPos, BlockPos entityPos) {
        this.explosionPos = explosionPos;
        this.entityPos = entityPos;
        this.hashCode = Objects.hash(explosionPos, entityPos);
    }

    public static int getLenientKey(Vec3 explosionPos, BlockPos entityPos) {
        int expX = (int) Math.floor(explosionPos.x);
        int expY = (int) Math.floor(explosionPos.y);
        int expZ = (int) Math.floor(explosionPos.z);

        return Objects.hash(expX, expY, expZ, entityPos.getX(), entityPos.getY(), entityPos.getZ());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof BlockDensityCacheKey other)) return false;
        return explosionPos.equals(other.explosionPos) && entityPos.equals(other.entityPos);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    public Vec3 getExplosionPos() {
        return explosionPos;
    }

    public BlockPos getEntityPos() {
        return entityPos;
    }
}
