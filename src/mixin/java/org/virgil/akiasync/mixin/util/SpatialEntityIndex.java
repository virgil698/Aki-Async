package org.virgil.akiasync.mixin.util;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import org.virgil.akiasync.mixin.async.explosion.density.IdBlockPos;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class SpatialEntityIndex {

    private final Object2ObjectOpenHashMap<IdBlockPos, Set<Entity>> spatialIndex = new Object2ObjectOpenHashMap<>();

    public void put(int x, int y, int z, Entity entity) {
        if (entity == null) return;

        IdBlockPos idPos = new IdBlockPos(x, y, z, entity.getUUID(), 0.0f);

        Set<Entity> entities = spatialIndex.computeIfAbsent(idPos, k -> new HashSet<>());
        entities.add(entity);
    }

    public boolean remove(Entity entity) {
        if (entity == null) return false;

        double x = entity.getX();
        double y = entity.getY();
        double z = entity.getZ();

        int chunkX = ((int) Math.floor(x)) & 15;
        int chunkY = ((int) Math.floor(y)) & 15;
        int chunkZ = ((int) Math.floor(z)) & 15;

        IdBlockPos searchKey = new IdBlockPos(chunkX, chunkY, chunkZ, entity.getUUID(), 0.0f);

        for (Map.Entry<IdBlockPos, Set<Entity>> entry : spatialIndex.entrySet()) {
            if (searchKey.strictEquals(entry.getKey())) {
                boolean removed = entry.getValue().remove(entity);

                if (entry.getValue().isEmpty()) {
                    spatialIndex.remove(entry.getKey());
                }
                return removed;
            }
        }

        return false;
    }

    public List<Entity> queryRange(AABB aabb) {
        List<Entity> result = new ArrayList<>();

        int minX = Math.max(0, ((int) Math.floor(aabb.minX)) & 15);
        int minY = Math.max(0, ((int) Math.floor(aabb.minY)) & 15);
        int minZ = Math.max(0, ((int) Math.floor(aabb.minZ)) & 15);
        int maxX = Math.min(15, ((int) Math.ceil(aabb.maxX)) & 15);
        int maxY = Math.min(15, ((int) Math.ceil(aabb.maxY)) & 15);
        int maxZ = Math.min(15, ((int) Math.ceil(aabb.maxZ)) & 15);

        int startKey = (minY << 16) | (minZ << 8) | minX;
        int endKey = (maxY << 16) | (maxZ << 8) | maxX;

        for (Map.Entry<IdBlockPos, Set<Entity>> entry : spatialIndex.entrySet()) {
            IdBlockPos idPos = entry.getKey();
            int linearKey = idPos.getLinearKey();

            if (linearKey >= startKey && linearKey <= endKey) {

                if (idPos.getX() >= minX && idPos.getX() <= maxX &&
                    idPos.getY() >= minY && idPos.getY() <= maxY &&
                    idPos.getZ() >= minZ && idPos.getZ() <= maxZ) {

                    result.addAll(entry.getValue());
                }
            }
        }

        return result;
    }

    public boolean contains(Entity entity) {
        if (entity == null) return false;

        for (Set<Entity> entities : spatialIndex.values()) {
            if (entities.contains(entity)) {
                return true;
            }
        }

        return false;
    }

    public void clear() {
        spatialIndex.clear();
    }

    public int size() {
        return spatialIndex.size();
    }

    public boolean isEmpty() {
        return spatialIndex.isEmpty();
    }

    public List<Entity> getAll() {
        List<Entity> result = new ArrayList<>();
        for (Set<Entity> entities : spatialIndex.values()) {
            result.addAll(entities);
        }
        return result;
    }
}
