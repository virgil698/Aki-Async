package org.virgil.akiasync.mixin.async.explosion;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
public class ExplosionSnapshot {
    private final Map<BlockPos, BlockState> blocks;
    private final Set<BlockPos> protectedBlocks;
    private final List<EntitySnapshot> entities;
    private final Vec3 center;
    private final float power;
    private final boolean fire;
    private final ServerLevel level;
    public ExplosionSnapshot(ServerLevel level, Vec3 center, float power, boolean fire) {
        this.level = level;
        this.center = center;
        this.power = power;
        this.fire = fire;
        this.blocks = new HashMap<>();
        this.protectedBlocks = new HashSet<>();

        org.virgil.akiasync.mixin.bridge.Bridge bridge =
            org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        boolean landProtectionEnabled = bridge != null && bridge.isTNTLandProtectionEnabled();

        int minX = (int) Math.floor(center.x - power - 1);
        int minY = (int) Math.floor(center.y - power - 1);
        int minZ = (int) Math.floor(center.z - power - 1);
        int maxX = (int) Math.ceil(center.x + power + 1);
        int maxY = (int) Math.ceil(center.y + power + 1);
        int maxZ = (int) Math.ceil(center.z + power + 1);
        minY = Math.max(level.getMinY(), minY);
        maxY = Math.min(level.getMaxY(), maxY);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    blocks.put(pos, level.getBlockState(pos));

                    if (landProtectionEnabled && !bridge.canTNTExplodeAt(level, pos)) {
                        protectedBlocks.add(pos);
                        if (bridge.isTNTDebugEnabled()) {
                            bridge.debugLog("[AkiAsync-TNT] Snapshot: Block at " + pos + " is protected by land protection");
                        }
                    }
                }
            }
        }
        double radius = 8.0;
        this.entities = level.getEntities(null,
            new net.minecraft.world.phys.AABB(
                center.x - radius, center.y - radius, center.z - radius,
                center.x + radius, center.y + radius, center.z + radius
            ))
            .stream()
            .map(e -> new EntitySnapshot(e.getUUID(), e.position(), e.getBoundingBox()))
            .collect(Collectors.toList());
    }
    public BlockState getBlockState(BlockPos pos) {
        return blocks.getOrDefault(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
    }
    public List<EntitySnapshot> getEntities() {
        return Collections.unmodifiableList(entities);
    }
    public Vec3 getCenter() {
        return center;
    }
    public float getPower() {
        return power;
    }
    public boolean isFire() {
        return fire;
    }
    public ServerLevel getLevel() {
        return level;
    }

    public boolean isProtected(BlockPos pos) {
        return protectedBlocks.contains(pos);
    }
    public static class EntitySnapshot {
        private final java.util.UUID uuid;
        private final Vec3 position;
        private final net.minecraft.world.phys.AABB boundingBox;
        public EntitySnapshot(java.util.UUID uuid, Vec3 position, net.minecraft.world.phys.AABB boundingBox) {
            this.uuid = uuid;
            this.position = position;
            this.boundingBox = boundingBox;
        }
        public java.util.UUID getUuid() {
            return uuid;
        }
        public Vec3 getPosition() {
            return position;
        }
        public net.minecraft.world.phys.AABB getBoundingBox() {
            return boundingBox;
        }
    }
}
