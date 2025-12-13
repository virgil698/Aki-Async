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
    private final Set<BlockPos> blockLockerProtectedBlocks; 
    private final List<EntitySnapshot> entities;
    private final Vec3 center;
    private final float power;
    private final boolean fire;
    private final boolean inFluid;
    private final ServerLevel level;
    private final boolean isInProtectedLand; 
    public ExplosionSnapshot(ServerLevel level, Vec3 center, float power, boolean fire) {
        this.level = level;
        this.center = center;
        this.power = power;
        this.fire = fire;
        
        BlockPos centerPos = BlockPos.containing(center);
        BlockState centerState = level.getBlockState(centerPos);
        this.inFluid = !centerState.getFluidState().isEmpty();
        
        this.blocks = new HashMap<>();
        this.protectedBlocks = new HashSet<>();
        this.blockLockerProtectedBlocks = new HashSet<>();

        org.virgil.akiasync.mixin.bridge.Bridge bridge =
            org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        boolean landProtectionEnabled = bridge != null && bridge.isTNTLandProtectionEnabled();
        boolean blockLockerEnabled = bridge != null && bridge.isBlockLockerProtectionEnabled();
        
        
        boolean centerInProtectedLand = false;
        if (landProtectionEnabled) {
            centerInProtectedLand = !bridge.canTNTExplodeAt(level, centerPos);
            if (centerInProtectedLand && bridge.isTNTDebugEnabled()) {
                bridge.debugLog("[AkiAsync-TNT] Explosion center at %s is in protected land, explosion will be cancelled", centerPos);
            }
        }
        this.isInProtectedLand = centerInProtectedLand;

        int minX = (int) Math.floor(center.x - power - 1);
        int minY = (int) Math.floor(center.y - power - 1);
        int minZ = (int) Math.floor(center.z - power - 1);
        int maxX = (int) Math.ceil(center.x + power + 1);
        int maxY = (int) Math.ceil(center.y + power + 1);
        int maxZ = (int) Math.ceil(center.z + power + 1);
        minY = Math.max(level.getMinY(), minY);
        maxY = Math.min(level.getMaxY(), maxY);

        Map<String, Boolean> chunkProtectionCache = new HashMap<>();
        
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    blocks.put(pos, state);

                    boolean isLandProtected = false;

                    
                    if (landProtectionEnabled && !centerInProtectedLand) {
                        
                        int chunkX = x >> 4;
                        int chunkZ = z >> 4;
                        String chunkKey = chunkX + "," + chunkZ;
                        
                        Boolean chunkProtection = chunkProtectionCache.get(chunkKey);
                        if (chunkProtection == null) {
                            
                            chunkProtection = bridge.checkChunkProtection(level, chunkX, chunkZ);
                            chunkProtectionCache.put(chunkKey, chunkProtection);
                        }

                        if (chunkProtection != null) {
                            
                            if (!chunkProtection) {
                                isLandProtected = true;
                            }
                        } else {
                            
                            if (!bridge.canTNTExplodeAt(level, pos)) {
                                isLandProtected = true;
                            }
                        }

                        if (isLandProtected) {
                            protectedBlocks.add(pos);
                            if (bridge.isTNTDebugEnabled()) {
                                bridge.debugLog("[AkiAsync-TNT] Snapshot: Block at " + pos + " is protected by land protection");
                            }
                            continue;
                        }
                    }

                    
                    if (blockLockerEnabled && bridge.isBlockLockerProtected(level, pos, state)) {
                        blockLockerProtectedBlocks.add(pos);
                        protectedBlocks.add(pos);
                        
                        
                        for (int dx = -1; dx <= 1; dx++) {
                            for (int dy = -1; dy <= 1; dy++) {
                                for (int dz = -1; dz <= 1; dz++) {
                                    if (dx == 0 && dy == 0 && dz == 0) continue;
                                    BlockPos nearbyPos = pos.offset(dx, dy, dz);
                                    protectedBlocks.add(nearbyPos);
                                }
                            }
                        }
                        
                        if (bridge.isTNTDebugEnabled()) {
                            bridge.debugLog("[AkiAsync-TNT] Snapshot: Container at " + pos + " is protected by BlockLocker (including 26 nearby blocks)");
                        }
                    }
                }
            }
        }
        
        double radius = Math.min(power * 2.0, 8.0);
        
        if (bridge != null && bridge.isTNTDebugEnabled()) {
            bridge.debugLog("[AkiAsync-TNT] ExplosionSnapshot: Collecting entities");
            bridge.debugLog("[AkiAsync-TNT] Center: %s, Power: %.1f, Radius: %.1f", center, power, radius);
        }
        
        net.minecraft.world.phys.AABB searchBox = new net.minecraft.world.phys.AABB(
            center.x - radius, center.y - radius, center.z - radius,
            center.x + radius, center.y + radius, center.z + radius
        );
        
        if (bridge != null && bridge.isTNTDebugEnabled()) {
            bridge.debugLog("[AkiAsync-TNT] Search box: %s", searchBox);
        }
        
        java.util.List<net.minecraft.world.entity.Entity> allEntities;
        
        if (org.virgil.akiasync.mixin.util.DirectEntityQuery.isAvailable()) {
            if (bridge != null && bridge.isTNTDebugEnabled()) {
                bridge.debugLog("[AkiAsync-TNT] Using DirectEntityQuery to bypass Mixin interception");
            }
            allEntities = org.virgil.akiasync.mixin.util.DirectEntityQuery.getEntitiesInRange(level, searchBox);
        } else {
            if (bridge != null && bridge.isTNTDebugEnabled()) {
                bridge.debugLog("[AkiAsync-TNT] DirectEntityQuery not available, using vanilla method");
            }
            allEntities = level.getEntities(null, searchBox);
        }
        
        if (bridge != null && bridge.isTNTDebugEnabled()) {
            bridge.debugLog("[AkiAsync-TNT] Found %d entities in search box", allEntities.size());
            
            for (net.minecraft.world.entity.Entity e : allEntities) {
                bridge.debugLog("[AkiAsync-TNT]   - Entity: %s at %s (UUID: %s)", 
                    e.getType().getDescriptionId(), e.position(), e.getUUID());
            }
        }
        
        java.util.List<net.minecraft.world.entity.Entity> filteredEntities = new java.util.ArrayList<>();
        for (net.minecraft.world.entity.Entity entity : allEntities) {
            
            if (!isEntityProtectedByBlocks(level, center, entity.position(), bridge)) {
                filteredEntities.add(entity);
            } else if (bridge != null && bridge.isTNTDebugEnabled()) {
                bridge.debugLog("[AkiAsync-TNT] Entity %s at %s is protected by explosion-proof blocks, skipping",
                    entity.getType().getDescriptionId(), entity.position());
            }
        }
        
        if (bridge != null && bridge.isTNTDebugEnabled()) {
            bridge.debugLog("[AkiAsync-TNT] After filtering: %d entities (removed %d protected entities)",
                filteredEntities.size(), allEntities.size() - filteredEntities.size());
        }
        
        this.entities = filteredEntities.stream()
            .map(e -> new EntitySnapshot(e.getUUID(), e.position(), e.getBoundingBox()))
            .collect(Collectors.toList());
        
        if (bridge != null && bridge.isTNTDebugEnabled()) {
            bridge.debugLog("[AkiAsync-TNT] Created %d entity snapshots", this.entities.size());
        }
        
        if (bridge != null && bridge.isTNTDebugEnabled()) {
            bridge.debugLog("[AkiAsync-TNT] Snapshot collected " + this.entities.size() + " entities at " + center);
            for (EntitySnapshot e : this.entities) {
                bridge.debugLog("[AkiAsync-TNT]   - Entity UUID: " + e.getUuid() + " at " + e.getPosition());
            }
        }
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
    public boolean isInFluid() {
        return inFluid;
    }
    public ServerLevel getLevel() {
        return level;
    }

    public boolean isProtected(BlockPos pos) {
        return protectedBlocks.contains(pos);
    }
    
    public boolean isBlockLockerProtected(BlockPos pos) {
        return blockLockerProtectedBlocks.contains(pos);
    }
    
    public boolean isInProtectedLand() {
        return isInProtectedLand;
    }
    
    private static boolean isEntityProtectedByBlocks(
        ServerLevel level,
        Vec3 explosionCenter,
        Vec3 entityPos,
        org.virgil.akiasync.mixin.bridge.Bridge bridge
    ) {
        
        double dx = entityPos.x - explosionCenter.x;
        double dy = entityPos.y - explosionCenter.y;
        double dz = entityPos.z - explosionCenter.z;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        
        if (distance < 0.1) {
            return false; 
        }
        
        dx /= distance;
        dy /= distance;
        dz /= distance;
        
        double step = 0.5;
        int steps = (int) Math.ceil(distance / step);
        
        for (int i = 1; i < steps; i++) {
            double checkDist = i * step;
            double checkX = explosionCenter.x + dx * checkDist;
            double checkY = explosionCenter.y + dy * checkDist;
            double checkZ = explosionCenter.z + dz * checkDist;
            
            BlockPos checkPos = new BlockPos((int) Math.floor(checkX), (int) Math.floor(checkY), (int) Math.floor(checkZ));
            BlockState state = level.getBlockState(checkPos);
            
            if (!state.isAir()) {
                
                float resistance = state.getBlock().getExplosionResistance();
                
                if (resistance > 20.0f) {
                    if (bridge != null && bridge.isTNTDebugEnabled()) {
                        bridge.debugLog("[AkiAsync-TNT] Found explosion-proof block at %s: %s (resistance: %.1f)",
                            checkPos, state.getBlock().getDescriptionId(), resistance);
                    }
                    return true; 
                }
                
                if (resistance > 5.0f) {
                    
                    int nearbyResistantBlocks = 0;
                    for (int j = i + 1; j < Math.min(i + 3, steps); j++) {
                        double nextDist = j * step;
                        double nextX = explosionCenter.x + dx * nextDist;
                        double nextY = explosionCenter.y + dy * nextDist;
                        double nextZ = explosionCenter.z + dz * nextDist;
                        
                        BlockPos nextPos = new BlockPos((int) Math.floor(nextX), (int) Math.floor(nextY), (int) Math.floor(nextZ));
                        if (!nextPos.equals(checkPos)) {
                            BlockState nextState = level.getBlockState(nextPos);
                            if (nextState.getBlock().getExplosionResistance() > 5.0f) {
                                nearbyResistantBlocks++;
                            }
                        }
                    }
                    
                    if (nearbyResistantBlocks >= 2) {
                        if (bridge != null && bridge.isTNTDebugEnabled()) {
                            bridge.debugLog("[AkiAsync-TNT] Found multiple resistant blocks starting at %s, entity is protected",
                                checkPos);
                        }
                        return true; 
                    }
                }
            }
        }
        
        return false; 
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
