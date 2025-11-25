package org.virgil.akiasync.mixin.async.explosion;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.virgil.akiasync.mixin.optimization.OptimizationManager;
import org.virgil.akiasync.mixin.optimization.scheduler.WorkStealingTaskScheduler;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class ExplosionCalculator {
    private static final int RAYCAST_SAMPLES = 16;
    private final ExplosionSnapshot snapshot;
    private final LinkedBlockingQueue<BlockPos> toDestroy = new LinkedBlockingQueue<>(10000);
    private final ConcurrentHashMap<BlockPos, Boolean> destroyedBlocks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Vec3> toHurt = new ConcurrentHashMap<>();
    private final boolean useFullRaycast;
    private final WorkStealingTaskScheduler scheduler;

    public ExplosionCalculator(ExplosionSnapshot snapshot) {
        this.snapshot = snapshot;
        org.virgil.akiasync.mixin.bridge.Bridge bridge = 
            org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        this.useFullRaycast = bridge != null && 
            bridge.isTNTVanillaCompatibilityEnabled() && 
            bridge.isTNTUseFullRaycast();
        
        WorkStealingTaskScheduler tempScheduler = null;
        if (!isFoliaEnvironment()) {
            try {
                tempScheduler = OptimizationManager.getInstance().getTaskScheduler();
            } catch (Exception e) {
                tempScheduler = null;
            }
        }
        this.scheduler = tempScheduler;
    }

    public ExplosionResult calculate() {
        calculateAffectedBlocks();
        calculateEntityDamage();
        return new ExplosionResult(new ArrayList<>(toDestroy), new HashMap<>(toHurt), snapshot.isFire());
    }

    private void calculateAffectedBlocks() {
        Vec3 center = snapshot.getCenter();
        float power = snapshot.getPower();
        for (int rayX = 0; rayX < RAYCAST_SAMPLES; rayX++) {
            for (int rayY = 0; rayY < RAYCAST_SAMPLES; rayY++) {
                for (int rayZ = 0; rayZ < RAYCAST_SAMPLES; rayZ++) {
                    if (!useFullRaycast && 
                        rayX != 0 && rayX != RAYCAST_SAMPLES - 1 &&
                        rayY != 0 && rayY != RAYCAST_SAMPLES - 1 &&
                        rayZ != 0 && rayZ != RAYCAST_SAMPLES - 1) {
                        continue;
                    }
                    double dirX = (double) rayX / (RAYCAST_SAMPLES - 1) * 2.0 - 1.0;
                    double dirY = (double) rayY / (RAYCAST_SAMPLES - 1) * 2.0 - 1.0;
                    double dirZ = (double) rayZ / (RAYCAST_SAMPLES - 1) * 2.0 - 1.0;
                    double length = Math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
                    dirX /= length;
                    dirY /= length;
                    dirZ /= length;
                    float rayPower = power * (0.7f + snapshot.getLevel().getRandom().nextFloat() * 0.6f);
                    double x = center.x;
                    double y = center.y;
                    double z = center.z;
                    while (rayPower > 0.0f) {
                        BlockPos pos = new BlockPos((int)x, (int)y, (int)z);
                        BlockState state = snapshot.getBlockState(pos);
                        if (!state.isAir()) {
                            float resistance = Math.max(0.0f, state.getBlock().getExplosionResistance());
                            rayPower -= (resistance + 0.3f) * 0.3f;
                            if (rayPower > 0.0f && !destroyedBlocks.containsKey(pos)) {
                                if (!state.getFluidState().isEmpty()) {
                                    continue;
                                }
                                if (state.canBeReplaced() && (state.isAir() || 
                                    state.is(net.minecraft.world.level.block.Blocks.WATER) ||
                                    state.is(net.minecraft.world.level.block.Blocks.LAVA) ||
                                    state.is(net.minecraft.world.level.block.Blocks.FIRE) ||
                                    state.is(net.minecraft.world.level.block.Blocks.SOUL_FIRE))) {
                                    
                                    org.virgil.akiasync.mixin.bridge.Bridge bridge = 
                                        org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
                                    if (bridge != null && bridge.isTNTDebugEnabled()) {
                                        bridge.debugLog("[AkiAsync-TNT] Skipping replaceable block at " + pos + ": " + 
                                            state.getBlock().getDescriptionId() + " (canBeReplaced: " + state.canBeReplaced() + ")");
                                    }
                                    continue;
                                }
                                
                                org.virgil.akiasync.mixin.bridge.Bridge landBridge = 
                                    org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
                                if (landBridge != null && !landBridge.canTNTExplodeAt(snapshot.getLevel(), pos)) {
                                    if (landBridge.isTNTDebugEnabled()) {
                                        landBridge.debugLog("[AkiAsync-TNT] Block at " + pos + " is protected by land protection plugin, skipping");
                                    }
                                    continue;
                                }
                                
                                if (destroyedBlocks.putIfAbsent(pos, true) == null) {
                                    toDestroy.add(pos);
                                    
                                    org.virgil.akiasync.mixin.bridge.Bridge bridge = 
                                        org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
                                    if (bridge != null && bridge.isTNTDebugEnabled()) {
                                        bridge.debugLog("[AkiAsync-TNT] Adding block to destroy at " + pos + ": " + 
                                            state.getBlock().getDescriptionId() + " (resistance: " + resistance + 
                                            ", rayPower: " + rayPower + ", canBeReplaced: " + state.canBeReplaced() + ")");
                                    }
                                }
                            }
                        }
                        x += dirX * 0.3;
                        y += dirY * 0.3;
                        z += dirZ * 0.3;
                        rayPower -= 0.22500001f;
                    }
                }
            }
        }
    }
    private void calculateEntityDamage() {
        Vec3 center = snapshot.getCenter();
        double radius = 8.0;
        for (ExplosionSnapshot.EntitySnapshot entity : snapshot.getEntities()) {
            double dx = entity.getPosition().x - center.x;
            double dy = entity.getPosition().y - center.y;
            double dz = entity.getPosition().z - center.z;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dist >= radius) continue;
            
            double exposure = calculateExposure(center, entity);
            if (exposure <= 0) continue;
            
            double impact = (1.0 - dist / radius) * exposure;
            
            org.virgil.akiasync.mixin.bridge.Bridge bridge = 
                org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
            if (bridge != null && bridge.isTNTDebugEnabled()) {
                bridge.debugLog("[AkiAsync-TNT] Entity damage calculation: " + 
                    entity.getUuid() + " dist=" + String.format("%.2f", dist) + 
                    " exposure=" + String.format("%.3f", exposure) + 
                    " impact=" + String.format("%.3f", impact));
            }
            
            if (impact <= 0.0) continue;
            
            double knockbackX = dx / dist * impact;
            double knockbackY = Math.max(dy / dist * impact, impact * 0.3);
            double knockbackZ = dz / dist * impact;
            
            double maxKnockback = 2.0;
            double knockbackLength = Math.sqrt(knockbackX * knockbackX + knockbackY * knockbackY + knockbackZ * knockbackZ);
            if (knockbackLength > maxKnockback) {
                double scale = maxKnockback / knockbackLength;
                knockbackX *= scale;
                knockbackY *= scale;
                knockbackZ *= scale;
            }
            
            toHurt.put(entity.getUuid(), new Vec3(knockbackX, knockbackY, knockbackZ));
        }
    }
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
                    
                    if (!hasBlockCollisionImproved(explosionCenter, target)) {
                        visibleRays++;
                    }
                    totalRays++;
                }
            }
        }
        
        double exposure = (double) visibleRays / totalRays;
        
        org.virgil.akiasync.mixin.bridge.Bridge bridge = 
            org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        if (bridge != null && bridge.isTNTDebugEnabled()) {
            bridge.debugLog("[AkiAsync-TNT] Entity exposure calculation: " + 
                entity.getUuid() + " exposure=" + String.format("%.3f", exposure) + 
                " (" + visibleRays + "/" + totalRays + " rays)");
        }
        
        return exposure;
    }
    
    private boolean hasBlockCollisionImproved(Vec3 start, Vec3 end) {
        Vec3 dir = end.subtract(start);
        double dist = dir.length();
        if (dist < 0.01) return false;
        
        dir = dir.normalize();
        
        double stepSize = 0.2;
        for (double step = 0; step < dist; step += stepSize) {
            Vec3 pos = start.add(dir.scale(step));
            BlockPos blockPos = new BlockPos((int)Math.floor(pos.x), (int)Math.floor(pos.y), (int)Math.floor(pos.z));
            BlockState state = snapshot.getBlockState(blockPos);
            
            if (!state.isAir() && state.getFluidState().isEmpty()) {
                float resistance = state.getBlock().getExplosionResistance();
                if (resistance > 0.5f) {
                    return true;
                }
            }
        }
        return false;
    }
    
    private static volatile Boolean isFolia = null;
    
    private static boolean isFoliaEnvironment() {
        if (isFolia == null) {
            try {
                Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
                isFolia = true;
            } catch (ClassNotFoundException e) {
                isFolia = false;
            }
        }
        return isFolia;
    }
}