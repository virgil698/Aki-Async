package org.virgil.akiasync.mixin.async.explosion;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.virgil.akiasync.mixin.optimization.OptimizationManager;
import org.virgil.akiasync.mixin.optimization.scheduler.WorkStealingTaskScheduler;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class ExplosionCalculator {
    private static final int RAYCAST_SAMPLES = 16;
    private final ExplosionSnapshot snapshot;
    private final ConcurrentLinkedQueue<BlockPos> toDestroy = new ConcurrentLinkedQueue<>();
    private final ConcurrentHashMap<UUID, Vec3> toHurt = new ConcurrentHashMap<>();
    private final boolean useFullRaycast;
    private final WorkStealingTaskScheduler scheduler;

    public ExplosionCalculator(ExplosionSnapshot snapshot) {
        this.snapshot = snapshot;
        // 从Bridge获取是否使用完整射线投射的配置
        org.virgil.akiasync.mixin.bridge.Bridge bridge = 
            org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        this.useFullRaycast = bridge != null && 
            bridge.isTNTVanillaCompatibilityEnabled() && 
            bridge.isTNTUseFullRaycast();
        
        // 获取Nitori风格的工作窃取调度器
        this.scheduler = OptimizationManager.getInstance().getTaskScheduler();
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
                            if (rayPower > 0.0f && !toDestroy.contains(pos)) {
                                if (!state.getFluidState().isEmpty()) {
                                    continue;
                                }
                                if (state.canBeReplaced()) {
                                    continue;
                                }
                                toDestroy.add(pos);
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
        float power = snapshot.getPower();
        double radius = power * 2.0;
        for (ExplosionSnapshot.EntitySnapshot entity : snapshot.getEntities()) {
            double dx = entity.getPosition().x - center.x;
            double dy = entity.getPosition().y - center.y;
            double dz = entity.getPosition().z - center.z;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dist >= radius) continue;
            
            // 计算暴露度（考虑方块遮挡）
            double exposure = calculateExposure(center, entity);
            if (exposure <= 0) continue; // 完全被遮挡，不受伤害
            
            double impact = (1.0 - dist / radius) * exposure;
            
            // 确保最小冲击力，避免过小的冲击
            if (impact < 0.1) continue;
            
            // 计算冲击方向，添加一些随机性使效果更自然
            double knockbackX = dx / dist * impact;
            double knockbackY = Math.max(dy / dist * impact, impact * 0.3); // 确保有向上的冲击
            double knockbackZ = dz / dist * impact;
            
            // 限制最大冲击力，避免玩家被炸飞太远
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
                    if (!hasBlockCollision(explosionCenter, target)) {
                        visibleRays++;
                    }
                    totalRays++;
                }
            }
        }
        return (double) visibleRays / totalRays;
    }
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