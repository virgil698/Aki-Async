package org.virgil.akiasync.mixin.async.explosion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Async explosion calculator (raycast + resistance + knockback)
 * 
 * Pure computation, thread-safe, no MC dependencies
 * Based on vanilla Explosion logic
 * 
 * @author Virgil
 */
public class ExplosionCalculator {
    private static final int RAYCAST_SAMPLES = 16; // 16 rays per block (vanilla)
    
    private final ExplosionSnapshot snapshot;
    private final List<BlockPos> toDestroy = new ArrayList<>();
    private final Map<UUID, Vec3> toHurt = new HashMap<>();
    
    public ExplosionCalculator(ExplosionSnapshot snapshot) {
        this.snapshot = snapshot;
    }
    
    /**
     * Calculate explosion (main entry point)
     */
    public ExplosionResult calculate() {
        // Step 1: Raycast to find affected blocks
        calculateAffectedBlocks();
        
        // Step 2: Calculate entity damage & knockback
        calculateEntityDamage();
        
        return new ExplosionResult(toDestroy, toHurt, snapshot.isFire());
    }
    
    /**
     * Step 1: Raycast explosion (vanilla algorithm)
     */
    private void calculateAffectedBlocks() {
        Vec3 center = snapshot.getCenter();
        float power = snapshot.getPower();
        
        // Raycast in 16x16x16 directions (vanilla)
        for (int rayX = 0; rayX < RAYCAST_SAMPLES; rayX++) {
            for (int rayY = 0; rayY < RAYCAST_SAMPLES; rayY++) {
                for (int rayZ = 0; rayZ < RAYCAST_SAMPLES; rayZ++) {
                    if (rayX != 0 && rayX != RAYCAST_SAMPLES - 1 &&
                        rayY != 0 && rayY != RAYCAST_SAMPLES - 1 &&
                        rayZ != 0 && rayZ != RAYCAST_SAMPLES - 1) {
                        continue; // Skip internal rays
                    }
                    
                    // Ray direction
                    double dirX = (double) rayX / (RAYCAST_SAMPLES - 1) * 2.0 - 1.0;
                    double dirY = (double) rayY / (RAYCAST_SAMPLES - 1) * 2.0 - 1.0;
                    double dirZ = (double) rayZ / (RAYCAST_SAMPLES - 1) * 2.0 - 1.0;
                    double length = Math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
                    dirX /= length;
                    dirY /= length;
                    dirZ /= length;
                    
                    // Ray power
                    float rayPower = power * (0.7f + snapshot.getLevel().getRandom().nextFloat() * 0.6f);
                    
                    // Cast ray
                    double x = center.x;
                    double y = center.y;
                    double z = center.z;
                    
                    while (rayPower > 0.0f) {
                        BlockPos pos = new BlockPos((int)x, (int)y, (int)z);
                        BlockState state = snapshot.getBlockState(pos);
                        
                        if (!state.isAir()) {
                            float resistance = Math.max(0.0f, state.getBlock().getExplosionResistance());
                            rayPower -= (resistance + 0.3f) * 0.3f;
                            
                            if (rayPower > 0.0f && !toDestroy.contains(pos)) {
                                // Water protection: skip waterlogged blocks
                                if (!state.getFluidState().isEmpty()) {
                                    continue; // Skip waterlogged blocks
                                }
                                
                                // Check if block can be destroyed by explosion
                                if (state.canBeReplaced()) {
                                    continue; // Skip replaceable blocks (water sources)
                                }
                                
                                toDestroy.add(pos);
                            }
                        }
                        
                        // Move ray forward
                        x += dirX * 0.3;
                        y += dirY * 0.3;
                        z += dirZ * 0.3;
                        rayPower -= 0.22500001f;
                    }
                }
            }
        }
    }
    
    /**
     * Step 2: Calculate entity damage & knockback
     */
    private void calculateEntityDamage() {
        Vec3 center = snapshot.getCenter();
        float power = snapshot.getPower();
        double radius = power * 2.0;
        
        for (ExplosionSnapshot.EntitySnapshot entity : snapshot.getEntities()) {
            double dx = entity.getPosition().x - center.x;
            double dy = entity.getPosition().y - center.y;
            double dz = entity.getPosition().z - center.z;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            
            if (dist >= radius) continue;
            
            // Calculate exposure (raycast from explosion to entity)
            double exposure = calculateExposure(center, entity);
            
            // Calculate knockback
            double impact = (1.0 - dist / radius) * exposure;
            double knockbackX = dx / dist * impact;
            double knockbackY = dy / dist * impact;
            double knockbackZ = dz / dist * impact;
            
            toHurt.put(entity.getUuid(), new Vec3(knockbackX, knockbackY, knockbackZ));
        }
    }
    
    /**
     * Calculate exposure (how visible entity is from explosion center)
     */
    private double calculateExposure(Vec3 explosionCenter, ExplosionSnapshot.EntitySnapshot entity) {
        net.minecraft.world.phys.AABB aabb = entity.getBoundingBox();
        double sizeX = aabb.maxX - aabb.minX;
        double sizeY = aabb.maxY - aabb.minY;
        double sizeZ = aabb.maxZ - aabb.minZ;
        
        double stepX = 1.0 / (sizeX * 2.0 + 1.0);
        double stepY = 1.0 / (sizeY * 2.0 + 1.0);
        double stepZ = 1.0 / (sizeZ * 2.0 + 1.0);
        
        if (stepX < 0 || stepY < 0 || stepZ < 0) {
            return 0;
        }
        
        int visibleRays = 0;
        int totalRays = 0;
        
        for (double x = 0.0; x <= 1.0; x += stepX) {
            for (double y = 0.0; y <= 1.0; y += stepY) {
                for (double z = 0.0; z <= 1.0; z += stepZ) {
                    double targetX = Mth.lerp(x, aabb.minX, aabb.maxX);
                    double targetY = Mth.lerp(y, aabb.minY, aabb.maxY);
                    double targetZ = Mth.lerp(z, aabb.minZ, aabb.maxZ);
                    
                    Vec3 target = new Vec3(targetX, targetY, targetZ);
                    if (!hasBlockCollision(explosionCenter, target)) {
                        visibleRays++;
                    }
                    totalRays++;
                }
            }
        }
        
        return (double) visibleRays / totalRays;
    }
    
    /**
     * Check if ray has block collision
     */
    private boolean hasBlockCollision(Vec3 start, Vec3 end) {
        Vec3 dir = end.subtract(start).normalize();
        double dist = start.distanceTo(end);
        
        for (double step = 0; step < dist; step += 0.3) {
            Vec3 pos = start.add(dir.scale(step));
            BlockPos blockPos = new BlockPos((int)pos.x, (int)pos.y, (int)pos.z);
            BlockState state = snapshot.getBlockState(blockPos);
            
            if (!state.isAir()) {
                return true;
            }
        }
        
        return false;
    }
}

