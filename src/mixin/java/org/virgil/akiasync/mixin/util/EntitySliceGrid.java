package org.virgil.akiasync.mixin.util;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class EntitySliceGrid {

    private final Int2ObjectOpenHashMap<Set<Entity>> entitySlices = new Int2ObjectOpenHashMap<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private static final double SMALL_AABB_THRESHOLD = 4.0;
    private static final double MEDIUM_AABB_THRESHOLD = 32.0;

    public static int calculateIntXYZ(int x, int y, int z) {
        return (y << 16) | (z << 8) | x;
    }

    public static int calculateIntXYZ(Entity entity) {
        int x = Math.floorMod((int) Math.floor(entity.getX()), 16);
        int y = Math.floorMod((int) Math.floor(entity.getY()), 16);
        int z = Math.floorMod((int) Math.floor(entity.getZ()), 16);
        return calculateIntXYZ(x, y, z);
    }

    public void addEntity(Entity entity) {
        if (entity == null || entity.isRemoved()) {
            return;
        }

        int intXYZ = calculateIntXYZ(entity);

        lock.writeLock().lock();
        try {
            Set<Entity> entities = entitySlices.computeIfAbsent(intXYZ, k -> new HashSet<>());
            entities.add(entity);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void removeEntity(Entity entity, int intXYZ) {
        if (entity == null) {
            return;
        }

        lock.writeLock().lock();
        try {
            Set<Entity> entities = entitySlices.get(intXYZ);
            if (entities != null) {
                entities.remove(entity);

                if (entities.isEmpty()) {
                    entitySlices.remove(intXYZ);
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public int updateEntitySlice(Entity entity, int oldIntXYZ) {
        if (entity == null || entity.isRemoved()) {
            removeEntity(entity, oldIntXYZ);
            return -1;
        }

        int newIntXYZ = calculateIntXYZ(entity);

        if (oldIntXYZ != newIntXYZ) {
            lock.writeLock().lock();
            try {
                Set<Entity> oldEntities = entitySlices.get(oldIntXYZ);
                if (oldEntities != null) {
                    oldEntities.remove(entity);
                    if (oldEntities.isEmpty()) {
                        entitySlices.remove(oldIntXYZ);
                    }
                }

                Set<Entity> newEntities = entitySlices.computeIfAbsent(newIntXYZ, k -> new HashSet<>());
                newEntities.add(entity);
            } finally {
                lock.writeLock().unlock();
            }
        }

        return newIntXYZ;
    }

    public List<Entity> queryRange(AABB aabb) {
        double sizeX = aabb.maxX - aabb.minX;
        double sizeY = aabb.maxY - aabb.minY;
        double sizeZ = aabb.maxZ - aabb.minZ;
        double maxSize = Math.max(Math.max(sizeX, sizeY), sizeZ);

        if (maxSize <= SMALL_AABB_THRESHOLD) {
            return queryRangeSmall(aabb);
        } else if (maxSize <= MEDIUM_AABB_THRESHOLD) {
            return queryRangeMedium(aabb);
        } else {
            return queryRangeLarge(aabb);
        }
    }

    private List<Entity> queryRangeSmall(AABB aabb) {
        List<Entity> result = new ArrayList<>();

        int chunkMinX = ((int) Math.floor(aabb.minX)) >> 4;
        int chunkMaxX = ((int) Math.floor(aabb.maxX)) >> 4;
        int chunkMinZ = ((int) Math.floor(aabb.minZ)) >> 4;
        int chunkMaxZ = ((int) Math.floor(aabb.maxZ)) >> 4;

        boolean crossesChunks = (chunkMinX != chunkMaxX) || (chunkMinZ != chunkMaxZ);

        if (crossesChunks) {
            return queryRangeMedium(aabb);
        }

        int minX = ((int) Math.floor(aabb.minX)) & 15;
        int minY = ((int) Math.floor(aabb.minY)) & 15;
        int minZ = ((int) Math.floor(aabb.minZ)) & 15;
        int maxX = ((int) Math.floor(aabb.maxX)) & 15;
        int maxY = ((int) Math.floor(aabb.maxY)) & 15;
        int maxZ = ((int) Math.floor(aabb.maxZ)) & 15;

        lock.readLock().lock();
        try {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    for (int x = minX; x <= maxX; x++) {
                        int intXYZ = calculateIntXYZ(x, y, z);
                        Set<Entity> entities = entitySlices.get(intXYZ);

                        if (entities != null && !entities.isEmpty()) {
                            result.addAll(entities);
                        }
                    }
                }
            }
        } finally {
            lock.readLock().unlock();
        }

        return result;
    }

    private List<Entity> queryRangeMedium(AABB aabb) {
        List<Entity> result = new ArrayList<>();
        Set<Entity> deduplicatedEntities = new HashSet<>();

        int minX = (int) Math.floor(aabb.minX);
        int minY = (int) Math.floor(aabb.minY);
        int minZ = (int) Math.floor(aabb.minZ);
        int maxX = (int) Math.floor(aabb.maxX);
        int maxY = (int) Math.floor(aabb.maxY);
        int maxZ = (int) Math.floor(aabb.maxZ);

        lock.readLock().lock();
        try {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    for (int x = minX; x <= maxX; x++) {
                        int localX = x & 15;
                        int localY = y & 15;
                        int localZ = z & 15;
                        int intXYZ = calculateIntXYZ(localX, localY, localZ);

                        Set<Entity> entities = entitySlices.get(intXYZ);
                        if (entities != null && !entities.isEmpty()) {
                            for (Entity entity : entities) {
                                if (entity != null && !entity.isRemoved() &&
                                    deduplicatedEntities.add(entity)) {
                                    result.add(entity);
                                }
                            }
                        }
                    }
                }
            }
        } finally {
            lock.readLock().unlock();
        }

        return result;
    }

    private List<Entity> queryRangeLarge(AABB aabb) {
        List<Entity> result = new ArrayList<>();

        lock.readLock().lock();
        try {
            for (Set<Entity> entities : entitySlices.values()) {
                if (entities != null && !entities.isEmpty()) {
                    for (Entity entity : entities) {
                        if (entity != null && !entity.isRemoved()) {
                            AABB entityBox = entity.getBoundingBox();
                            if (entityBox != null && entityBox.intersects(aabb)) {
                                result.add(entity);
                            }
                        }
                    }
                }
            }
        } finally {
            lock.readLock().unlock();
        }

        return result;
    }

    public Set<Entity> getEntities(int intXYZ) {
        lock.readLock().lock();
        try {
            return entitySlices.get(intXYZ);
        } finally {
            lock.readLock().unlock();
        }
    }

    public void clear() {
        lock.writeLock().lock();
        try {
            entitySlices.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public int size() {
        lock.readLock().lock();
        try {
            return entitySlices.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    public int getTotalEntityCount() {
        lock.readLock().lock();
        try {
            int count = 0;
            for (Set<Entity> entities : entitySlices.values()) {
                count += entities.size();
            }
            return count;
        } finally {
            lock.readLock().unlock();
        }
    }

    public boolean isEmpty() {
        lock.readLock().lock();
        try {
            return entitySlices.isEmpty();
        } finally {
            lock.readLock().unlock();
        }
    }

    public String getStats() {
        return String.format("Slices: %d, Total Entities: %d, Thresholds: Small=%.1f, Medium=%.1f",
            size(), getTotalEntityCount(), SMALL_AABB_THRESHOLD, MEDIUM_AABB_THRESHOLD);
    }

    public static double getSmallThreshold() {
        return SMALL_AABB_THRESHOLD;
    }

    public static double getMediumThreshold() {
        return MEDIUM_AABB_THRESHOLD;
    }
}
