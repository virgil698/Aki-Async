package org.virgil.akiasync.mixin.util;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class SectionEntityGrid {

    private final Int2ObjectOpenHashMap<Entity[]> grid;

    private final Int2ObjectOpenHashMap<Integer> entityToBlockHash;

    private static final int INITIAL_BLOCK_CAPACITY = 4;

    public SectionEntityGrid() {
        this.grid = new Int2ObjectOpenHashMap<>(256);
        this.entityToBlockHash = new Int2ObjectOpenHashMap<>(512);
    }

    private int hashBlock(int dotX, int dotY, int dotZ) {
        return (dotX & 0xF) << 8 | (dotY & 0xF) << 4 | (dotZ & 0xF);
    }

    private int hashBlockFromEntity(Entity entity) {
        Vec3 pos = entity.position();
        int dotX = ((int) Math.floor(pos.x)) & 0xF;
        int dotY = ((int) Math.floor(pos.y)) & 0xF;
        int dotZ = ((int) Math.floor(pos.z)) & 0xF;
        return hashBlock(dotX, dotY, dotZ);
    }

    public void addEntity(Entity entity) {
        if (entity == null || !entity.isAlive()) {
            return;
        }

        int blockHash = hashBlockFromEntity(entity);
        int entityId = entity.getId();

        entityToBlockHash.put(entityId, Integer.valueOf(blockHash));

        Entity[] entities = grid.get(blockHash);
        if (entities == null) {
            entities = new Entity[INITIAL_BLOCK_CAPACITY];
            entities[0] = entity;
            grid.put(blockHash, entities);
        } else {

            int emptySlot = -1;
            for (int i = 0; i < entities.length; i++) {
                if (entities[i] == null) {
                    emptySlot = i;
                    break;
                } else if (entities[i].getId() == entityId) {

                    return;
                }
            }

            if (emptySlot != -1) {
                entities[emptySlot] = entity;
            } else {

                Entity[] newEntities = new Entity[entities.length * 2];
                System.arraycopy(entities, 0, newEntities, 0, entities.length);
                newEntities[entities.length] = entity;
                grid.put(blockHash, newEntities);
            }
        }
    }

    public void removeEntity(Entity entity) {
        if (entity == null) {
            return;
        }

        int entityId = entity.getId();
        Integer blockHash = entityToBlockHash.remove(entityId);
        if (blockHash == null) {
            return;
        }

        Entity[] entities = grid.get(blockHash);
        if (entities == null) {
            return;
        }

        for (int i = 0; i < entities.length; i++) {
            if (entities[i] != null && entities[i].getId() == entityId) {
                entities[i] = null;
                break;
            }
        }
    }

    public void updateEntity(Entity entity) {
        if (entity == null || !entity.isAlive()) {
            return;
        }

        int entityId = entity.getId();
        int newBlockHash = hashBlockFromEntity(entity);
        Integer oldBlockHash = entityToBlockHash.get(entityId);

        if (oldBlockHash != null && oldBlockHash == newBlockHash) {
            return;
        }

        if (oldBlockHash != null) {

            Entity[] oldEntities = grid.get(oldBlockHash);
            if (oldEntities != null) {
                for (int i = 0; i < oldEntities.length; i++) {
                    if (oldEntities[i] != null && oldEntities[i].getId() == entityId) {
                        oldEntities[i] = null;
                        break;
                    }
                }
            }
        }

        entityToBlockHash.put(entityId, Integer.valueOf(newBlockHash));
        Entity[] newEntities = grid.get(newBlockHash);
        if (newEntities == null) {
            newEntities = new Entity[INITIAL_BLOCK_CAPACITY];
            newEntities[0] = entity;
            grid.put(newBlockHash, newEntities);
        } else {

            int emptySlot = -1;
            for (int i = 0; i < newEntities.length; i++) {
                if (newEntities[i] == null) {
                    emptySlot = i;
                    break;
                }
            }

            if (emptySlot != -1) {
                newEntities[emptySlot] = entity;
            } else {

                Entity[] expandedEntities = new Entity[newEntities.length * 2];
                System.arraycopy(newEntities, 0, expandedEntities, 0, newEntities.length);
                expandedEntities[newEntities.length] = entity;
                grid.put(newBlockHash, expandedEntities);
            }
        }
    }

    public List<Entity> queryAABB(AABB aabb) {
        List<Entity> result = new ArrayList<>();

        int minDotX = ((int) Math.floor(aabb.minX)) & 0xF;
        int minDotY = ((int) Math.floor(aabb.minY)) & 0xF;
        int minDotZ = ((int) Math.floor(aabb.minZ)) & 0xF;
        int maxDotX = ((int) Math.floor(aabb.maxX)) & 0xF;
        int maxDotY = ((int) Math.floor(aabb.maxY)) & 0xF;
        int maxDotZ = ((int) Math.floor(aabb.maxZ)) & 0xF;

        for (int dotX = minDotX; dotX <= maxDotX; dotX++) {
            for (int dotY = minDotY; dotY <= maxDotY; dotY++) {
                for (int dotZ = minDotZ; dotZ <= maxDotZ; dotZ++) {
                    int blockHash = hashBlock(dotX, dotY, dotZ);
                    Entity[] entities = grid.get(blockHash);

                    if (entities == null) {
                        continue;
                    }

                    for (Entity entity : entities) {
                        if (entity == null) {
                            continue;
                        }

                        if (!entity.isAlive()) {
                            continue;
                        }

                        if (entity.getBoundingBox().intersects(aabb)) {
                            result.add(entity);
                        }
                    }
                }
            }
        }

        return result;
    }

    public int countCollisions(Entity entity, double radius) {
        Vec3 pos = entity.position();
        AABB searchBox = new AABB(
            pos.x - radius, pos.y - radius, pos.z - radius,
            pos.x + radius, pos.y + radius, pos.z + radius
        );

        List<Entity> nearby = queryAABB(searchBox);

        int count = 0;
        int entityId = entity.getId();
        for (Entity e : nearby) {
            if (e.getId() != entityId) {
                count++;
            }
        }

        return count;
    }

    public void clear() {
        grid.clear();
        entityToBlockHash.clear();
    }

    public String getStats() {
        int totalBlocks = grid.size();
        int totalEntities = entityToBlockHash.size();
        int totalCapacity = 0;
        int usedSlots = 0;

        for (Entity[] entities : grid.values()) {
            if (entities != null) {
                totalCapacity += entities.length;
                for (Entity entity : entities) {
                    if (entity != null) {
                        usedSlots++;
                    }
                }
            }
        }

        double fillRate = totalCapacity > 0 ? (usedSlots * 100.0 / totalCapacity) : 0;

        return String.format("Blocks: %d, Entities: %d, Capacity: %d, Fill: %.1f%%",
            totalBlocks, totalEntities, totalCapacity, fillRate);
    }
}
