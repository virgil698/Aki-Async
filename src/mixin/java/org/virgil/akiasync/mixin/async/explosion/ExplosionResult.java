package org.virgil.akiasync.mixin.async.explosion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public class ExplosionResult {
    private final List<BlockPos> toDestroy;
    private final Map<UUID, Vec3> toHurt;
    private boolean fire;

    public ExplosionResult() {
        this.toDestroy = new ArrayList<>(1000);
        this.toHurt = new HashMap<>(100);
        this.fire = false;
    }

    public ExplosionResult(List<BlockPos> toDestroy, Map<UUID, Vec3> toHurt, boolean fire) {
        this.toDestroy = new ArrayList<>(toDestroy);
        this.toHurt = new HashMap<>(toHurt);
        this.fire = fire;
    }

    public void set(List<BlockPos> toDestroy, Map<UUID, Vec3> toHurt, boolean fire) {
        this.toDestroy.clear();
        this.toDestroy.addAll(toDestroy);
        this.toHurt.clear();
        this.toHurt.putAll(toHurt);
        this.fire = fire;
    }

    public void clear() {
        this.toDestroy.clear();
        this.toHurt.clear();
        this.fire = false;
    }

    public List<BlockPos> getToDestroy() {
        return Collections.unmodifiableList(toDestroy);
    }

    public Map<UUID, Vec3> getToHurt() {
        return Collections.unmodifiableMap(toHurt);
    }

    public boolean isFire() {
        return fire;
    }
}
