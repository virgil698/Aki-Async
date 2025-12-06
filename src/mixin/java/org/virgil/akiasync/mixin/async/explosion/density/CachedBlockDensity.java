package org.virgil.akiasync.mixin.async.explosion.density;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class CachedBlockDensity {
    private AABB source;
    private AABB entity;
    private final float blockDensity;
    private final boolean complete;

    public CachedBlockDensity(Vec3 explosionPos, Entity entity, float blockDensity) {
        this.source = new AABB(explosionPos, explosionPos);
        this.entity = entity.getBoundingBox();
        this.blockDensity = blockDensity;
        this.complete = blockDensity == 0.0f || blockDensity == 1.0f;
    }

    public float blockDensity() {
        return this.blockDensity;
    }

    public boolean complete() {
        return this.complete;
    }

    public boolean hasPosition(Vec3 explosionPos, AABB entityBoundingBox) {
        return this.isExplosionPosition(explosionPos) && this.entity.contains(entityBoundingBox.getCenter());
    }

    public boolean isKnownPosition(Vec3 pos) {
        return this.entity.contains(pos);
    }

    public boolean isExplosionPosition(Vec3 explosionPos) {
        return this.source.contains(explosionPos);
    }

    public void expand(Vec3 explosionPos, Entity entity) {
        this.source = this.source.minmax(new AABB(explosionPos, explosionPos));
        this.entity = this.entity.minmax(entity.getBoundingBox());
    }
}
