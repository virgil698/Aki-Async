package org.virgil.akiasync.mixin.async.explosion;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import org.virgil.akiasync.mixin.async.explosion.density.SakuraBlockDensityCache;
import org.virgil.akiasync.mixin.optimization.OptimizationManager;
import org.virgil.akiasync.mixin.optimization.scheduler.WorkStealingTaskScheduler;
import org.virgil.akiasync.mixin.util.ObjectPool;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class ExplosionCalculator {
    private static final int RAYCAST_SAMPLES = 16;
    
    private static final ObjectPool<ExplosionResult> RESULT_POOL = new ObjectPool<>(
        ExplosionResult::new,
        16 
    );
    
    private final ExplosionSnapshot snapshot;
    private final LinkedBlockingQueue<BlockPos> toDestroy = new LinkedBlockingQueue<>(10000);
    private final ConcurrentHashMap<BlockPos, Boolean> destroyedBlocks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Vec3> toHurt = new ConcurrentHashMap<>();
    private final boolean useFullRaycast;
    private final SakuraBlockDensityCache densityCache;
    private final OptimizedExplosionCache optimizedCache;
    private final DDACollisionDetector ddaDetector;
    
    @SuppressWarnings("unused")
    private final WorkStealingTaskScheduler scheduler;

    public ExplosionCalculator(ExplosionSnapshot snapshot) {
        this.snapshot = snapshot;
        org.virgil.akiasync.mixin.bridge.Bridge bridge =
            org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        this.useFullRaycast = bridge != null &&
            bridge.isTNTVanillaCompatibilityEnabled() &&
            bridge.isTNTUseFullRaycast();

        this.densityCache = SakuraBlockDensityCache.getOrCreate(snapshot.getLevel());
        
        this.densityCache.expire(snapshot.getLevel().getGameTime());
        
        this.optimizedCache = new OptimizedExplosionCache(snapshot.getLevel());
        
        this.ddaDetector = new DDACollisionDetector();

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
        org.virgil.akiasync.mixin.bridge.Bridge bridge =
            org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        
        if (bridge != null && bridge.isTNTDebugEnabled()) {
            bridge.debugLog("[AkiAsync-TNT] ExplosionCalculator.calculate() started");
        }
        
        calculateAffectedBlocks();
        
        if (bridge != null && bridge.isTNTDebugEnabled()) {
            bridge.debugLog("[AkiAsync-TNT] Blocks to destroy: %d", toDestroy.size());
        }
        
        calculateEntityDamage();
        
        if (bridge != null && bridge.isTNTDebugEnabled()) {
            bridge.debugLog("[AkiAsync-TNT] Entities to hurt: %d", toHurt.size());
        }
        
        ExplosionResult result = RESULT_POOL.acquire();
        result.set(new ArrayList<>(toDestroy), new HashMap<>(toHurt), snapshot.isFire());
        
        if (bridge != null && bridge.isTNTDebugEnabled()) {
            bridge.debugLog("[AkiAsync-TNT] ExplosionCalculator.calculate() completed");
        }
        
        return result;
    }
    
    public static void releaseResult(ExplosionResult result) {
        if (result != null) {
            result.clear();
            RESULT_POOL.release(result);
        }
    }
    
    public static String getPoolStats() {
        return RESULT_POOL.getStats();
    }

    private void calculateAffectedBlocks() {
        Vec3 center = snapshot.getCenter();
        if (center == null) {
            return;
        }
        
        if (snapshot.isInFluid()) {
            return;
        }
        
        float power = snapshot.getPower();
        
        Vec3[] precomputedRays = PrecomputedExplosionShape.getPrecomputedRays();
        int totalRays = precomputedRays.length;
        
        for (int rayIndex = 0; rayIndex < totalRays; rayIndex++) {
            
            Vec3 rayDir = precomputedRays[rayIndex];
            double dirX = rayDir.x * 0.3;
            double dirY = rayDir.y * 0.3;
            double dirZ = rayDir.z * 0.3;
            
            float rayPower = power * (0.7f + snapshot.getLevel().getRandom().nextFloat() * 0.6f);
            double x = center.x;
            double y = center.y;
            double z = center.z;
                    while (rayPower > 0.0f) {
                        BlockPos pos = new BlockPos((int)x, (int)y, (int)z);
                        
                        BlockState state = optimizedCache.getBlockState(pos);
                        if (!state.isAir()) {
                            float resistance = Math.max(0.0f, optimizedCache.getResistance(pos));
                            
                            if (!optimizedCache.getFluidState(pos).isEmpty()) {

                                rayPower -= (resistance + 0.3f) * 0.3f;
                                continue;
                            }
                            
                            rayPower -= (resistance + 0.3f) * 0.3f;
                            if (rayPower > 0.0f && !destroyedBlocks.containsKey(pos)) {
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

                                if (snapshot.isProtected(pos)) {
                                    org.virgil.akiasync.mixin.bridge.Bridge bridge =
                                        org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
                                    if (bridge != null && bridge.isTNTDebugEnabled()) {
                                        bridge.debugLog("[AkiAsync-TNT] Block at " + pos + " is protected (from snapshot), skipping");
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
                        
                        x += dirX;
                        y += dirY;
                        z += dirZ;
                        rayPower -= 0.22500001f;
                    }
        }
    }
    private void calculateEntityDamage() {
        Vec3 center = snapshot.getCenter();
        if (center == null) {
            return;
        }
        double radius = 8.0;
        
        org.virgil.akiasync.mixin.bridge.Bridge bridge =
            org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        
        java.util.List<ExplosionSnapshot.EntitySnapshot> entitiesToProcess = snapshot.getEntities();
        
        if (bridge != null && bridge.isTNTDebugEnabled()) {
            bridge.debugLog("[AkiAsync-TNT] calculateEntityDamage: Processing %d entities", entitiesToProcess.size());
            bridge.debugLog("[AkiAsync-TNT] Explosion center: %s", center);
            bridge.debugLog("[AkiAsync-TNT] Explosion radius: %.1f", radius);
        }
        
        if (bridge != null && bridge.isTNTUseSakuraDensityCache()) {
            
            net.minecraft.world.phys.AABB explosionAABB = new net.minecraft.world.phys.AABB(
                center.x - radius, center.y - radius, center.z - radius,
                center.x + radius, center.y + radius, center.z + radius
            );
            
            java.util.List<org.virgil.akiasync.mixin.async.explosion.density.IdBlockPos> filteredEntities = 
                densityCache.querySpatialIndex(explosionAABB);
            
            if (bridge.isTNTDebugEnabled()) {
                bridge.debugLog("[AkiAsync-TNT] Spatial index filtered: " + 
                    filteredEntities.size() + " / " + entitiesToProcess.size() + " entities");
            }
        }
        
        for (ExplosionSnapshot.EntitySnapshot entity : entitiesToProcess) {
            double dx = entity.getPosition().x - center.x;
            double dy = entity.getPosition().y - center.y;
            double dz = entity.getPosition().z - center.z;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dist >= radius) continue;

            double exposure = calculateExposure(center, entity);
            if (exposure <= 0) continue;

            double impact = (1.0 - dist / radius) * exposure;

            if (bridge != null && bridge.isTNTDebugEnabled()) {
                bridge.debugLog("[AkiAsync-TNT] Entity damage calculation: " +
                    entity.getUuid() + " dist=" + String.format("%.2f", dist) +
                    " exposure=" + String.format("%.3f", exposure) +
                    " impact=" + String.format("%.3f", impact));
            }

            if (impact <= 0.0) {
                if (bridge != null && bridge.isTNTDebugEnabled()) {
                    bridge.debugLog("[AkiAsync-TNT] Entity %s impact <= 0, skipping", entity.getUuid());
                }
                continue;
            }

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

            Vec3 knockbackVec = new Vec3(knockbackX, knockbackY, knockbackZ);
            
            if (bridge != null && bridge.isTNTDebugEnabled()) {
                bridge.debugLog("[AkiAsync-TNT] Adding entity %s to hurt list", entity.getUuid());
                bridge.debugLog("[AkiAsync-TNT]   Knockback: %s", knockbackVec);
            }
            
            toHurt.put(entity.getUuid(), knockbackVec);
        }
        
        if (bridge != null && bridge.isTNTDebugEnabled()) {
            bridge.debugLog("[AkiAsync-TNT] calculateEntityDamage completed, total entities to hurt: %d", toHurt.size());
        }
    }
    private double calculateExposure(Vec3 explosionCenter, ExplosionSnapshot.EntitySnapshot entity) {
        org.virgil.akiasync.mixin.bridge.Bridge bridge =
            org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        
        if (bridge != null && bridge.isTNTUseSakuraDensityCache()) {
            float cachedDensity = densityCache.getDensityByUUID(entity.getUuid());
            if (cachedDensity != SakuraBlockDensityCache.UNKNOWN_DENSITY) {
                if (bridge.isTNTDebugEnabled()) {
                    bridge.debugLog("[AkiAsync-TNT] Using cached density from spatial index: " + 
                        entity.getUuid() + " density=" + String.format("%.3f", cachedDensity));
                }
                return cachedDensity;
            }
        }
        
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

        if (bridge != null && bridge.isTNTUseSakuraDensityCache()) {
            densityCache.putSpatialIndex(explosionCenter, entity.getPosition(), entity.getUuid(), (float) exposure);
        }

        if (bridge != null && bridge.isTNTDebugEnabled()) {
            bridge.debugLog("[AkiAsync-TNT] Entity exposure calculation: " +
                entity.getUuid() + " exposure=" + String.format("%.3f", exposure) +
                " (" + visibleRays + "/" + totalRays + " rays)");
        }

        return exposure;
    }

    private boolean hasBlockCollisionImproved(Vec3 start, Vec3 end) {
        if (start == null || end == null) {
            return false;
        }
        
        return ddaDetector.hasCollision(optimizedCache, start, end);
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
