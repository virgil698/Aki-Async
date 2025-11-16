package org.virgil.akiasync.mixin.mixins.explosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import java.util.Map;
import java.util.UUID;
@SuppressWarnings("unused")
@Mixin(value = PrimedTnt.class, priority = 1200)
public class TNTExplosionMixin {
    @Inject(method = "explode", at = @At("HEAD"), cancellable = true)
    private void aki$asyncExplosion(CallbackInfo ci) {
        PrimedTnt tnt = (PrimedTnt) (Object) this;
        Level level = tnt.level();
        if (!(level instanceof ServerLevel sl)) return;
        
        org.virgil.akiasync.mixin.bridge.Bridge bridge = 
            org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        if (bridge == null || !bridge.isTNTOptimizationEnabled()) return;
        
        String entityId = tnt.getEncodeId();
        if (entityId == null || !bridge.getTNTExplosionEntities().contains(entityId)) {
            return;
        }
        
        ci.cancel();
        
        Vec3 center = tnt.position();
        boolean inWater = tnt.isInWater() || !sl.getFluidState(BlockPos.containing(center)).isEmpty();
        
        // 如果在水中，使用原版爆炸逻辑确保粒子效果正确
        if (inWater) {
            sl.getServer().execute(() -> {
                try {
                    // 在水中使用原版爆炸逻辑，MC会自动处理水中不破坏方块的逻辑
                    sl.explode(
                        tnt,
                        net.minecraft.world.level.Explosion.getDefaultDamageSource(sl, tnt),
                        null,
                        center.x,
                        center.y,
                        center.z,
                        4.0F,
                        false,
                        net.minecraft.world.level.Level.ExplosionInteraction.TNT
                    );
                    
                    if (bridge.isTNTDebugEnabled()) {
                        System.out.println("[AkiAsync-TNT] Water explosion completed at " + center);
                    }
                } catch (Exception ex) {
                    System.err.println("[AkiAsync-TNT] Error in water explosion: " + ex.getMessage());
                }
            });
            tnt.discard();
            return;
        }
        
        // 非水中环境使用异步优化
        org.virgil.akiasync.mixin.async.explosion.ExplosionSnapshot snapshot = 
            new org.virgil.akiasync.mixin.async.explosion.ExplosionSnapshot(sl, center, 4.0F, false);
        
        org.virgil.akiasync.mixin.async.TNTThreadPool.getExecutor().execute(() -> {
            try {
                org.virgil.akiasync.mixin.async.explosion.ExplosionCalculator calculator = 
                    new org.virgil.akiasync.mixin.async.explosion.ExplosionCalculator(snapshot);
                org.virgil.akiasync.mixin.async.explosion.ExplosionResult result = calculator.calculate();
                
                sl.getServer().execute(() -> {
                    try {
                        net.minecraft.world.level.ServerExplosion explosion = new net.minecraft.world.level.ServerExplosion(
                            sl, tnt, null, null, center, 4.0F, false, 
                            net.minecraft.world.level.Explosion.BlockInteraction.DESTROY_WITH_DECAY
                    );
                        
                        applyExplosionResults(sl, explosion, result, tnt, center, false);
                        
                        if (bridge.isTNTDebugEnabled()) {
                            System.out.println("[AkiAsync-TNT] Async explosion completed at " + center);
                        }
                    } catch (Exception ex) {
                        System.err.println("[AkiAsync-TNT] Error applying explosion results: " + ex.getMessage());
                    }
                });
            } catch (Exception ex) {
                System.err.println("[AkiAsync-TNT] Error in async explosion calculation: " + ex.getMessage());
                sl.getServer().execute(() -> {
                    try {
                        net.minecraft.world.level.ServerExplosion explosion = new net.minecraft.world.level.ServerExplosion(
                            sl, tnt, null, null, center, 4.0F, false, 
                            net.minecraft.world.level.Explosion.BlockInteraction.DESTROY_WITH_DECAY
                    );
                        explosion.explode();
                    } catch (Exception fallbackEx) {
                        System.err.println("[AkiAsync-TNT] Fallback explosion failed: " + fallbackEx.getMessage());
                    }
                });
            }
        });
        
        tnt.discard();
    }
    
    private static void applyExplosionResults(ServerLevel level, 
                                            net.minecraft.world.level.ServerExplosion explosion,
                                            org.virgil.akiasync.mixin.async.explosion.ExplosionResult result,
                                            PrimedTnt tnt, Vec3 center, boolean inWater) {
        try {
            // 播放爆炸声音
            level.playSound(null, center.x, center.y, center.z, 
                net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE, 
                net.minecraft.sounds.SoundSource.BLOCKS, 4.0F, 
                (1.0F + (level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 0.2F) * 0.7F);
            
            // 添加主爆炸粒子
            level.addParticle(net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER, 
                center.x, center.y, center.z, 1.0D, 0.0D, 0.0D);
            
            // 添加额外的爆炸粒子效果
            for (int i = 0; i < 16; i++) {
                double offsetX = (level.getRandom().nextDouble() - 0.5) * 4.0;
                double offsetY = (level.getRandom().nextDouble() - 0.5) * 4.0; 
                double offsetZ = (level.getRandom().nextDouble() - 0.5) * 4.0;
                level.addParticle(net.minecraft.core.particles.ParticleTypes.EXPLOSION, 
                    center.x + offsetX, center.y + offsetY, center.z + offsetZ, 
                    offsetX * 0.15, offsetY * 0.15, offsetZ * 0.15);
            }
            
            // 添加烟雾粒子效果
            for (int i = 0; i < 12; i++) {
                double offsetX = (level.getRandom().nextDouble() - 0.5) * 6.0;
                double offsetY = level.getRandom().nextDouble() * 3.0;
                double offsetZ = (level.getRandom().nextDouble() - 0.5) * 6.0;
                level.addParticle(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE, 
                    center.x + offsetX, center.y + offsetY, center.z + offsetZ, 
                    offsetX * 0.05, 0.1, offsetZ * 0.05);
            }
            
            // 添加火花粒子效果
            for (int i = 0; i < 20; i++) {
                double offsetX = (level.getRandom().nextDouble() - 0.5) * 3.0;
                double offsetY = (level.getRandom().nextDouble() - 0.5) * 3.0;
                double offsetZ = (level.getRandom().nextDouble() - 0.5) * 3.0;
                level.addParticle(net.minecraft.core.particles.ParticleTypes.LAVA, 
                    center.x + offsetX, center.y + offsetY, center.z + offsetZ, 
                    offsetX * 0.2, offsetY * 0.2, offsetZ * 0.2);
            }
            
            // 破坏方块 (此方法只在非水中环境调用)
            for (BlockPos pos : result.getToDestroy()) {
                net.minecraft.world.level.block.state.BlockState state = level.getBlockState(pos);
                if (!state.isAir()) {
                    // 特殊处理TNT方块：生成点燃的TNT实体而不是破坏
                    if (state.is(net.minecraft.world.level.block.Blocks.TNT)) {
                        // 移除TNT方块
                        level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 11);
                        
                        // 计算到爆炸中心的距离来确定引信时间
                        double distance = Math.sqrt(pos.distToCenterSqr(center.x, center.y, center.z));
                        int fuseTime = Math.max(10, (int)(distance * 2.0) + level.getRandom().nextInt(10)); // 10-30 ticks
                        
                        // 创建点燃的TNT实体
                        PrimedTnt primedTnt = new PrimedTnt(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, null);
                        primedTnt.setFuse(fuseTime);
                        
                        // 添加一些随机的初始速度，模拟被爆炸推动
                        double pushX = (pos.getX() + 0.5 - center.x) * 0.1 + (level.getRandom().nextDouble() - 0.5) * 0.1;
                        double pushY = Math.abs(pos.getY() + 0.5 - center.y) * 0.1 + level.getRandom().nextDouble() * 0.2;
                        double pushZ = (pos.getZ() + 0.5 - center.z) * 0.1 + (level.getRandom().nextDouble() - 0.5) * 0.1;
                        primedTnt.setDeltaMovement(pushX, pushY, pushZ);
                        
                        level.addFreshEntity(primedTnt);
                        
                        // 添加引燃粒子效果
                        for (int i = 0; i < 5; i++) {
                            double offsetX = (level.getRandom().nextDouble() - 0.5) * 0.8;
                            double offsetY = level.getRandom().nextDouble() * 0.8;
                            double offsetZ = (level.getRandom().nextDouble() - 0.5) * 0.8;
                            level.addParticle(net.minecraft.core.particles.ParticleTypes.FLAME,
                                pos.getX() + 0.5 + offsetX, pos.getY() + 0.5 + offsetY, pos.getZ() + 0.5 + offsetZ,
                                0.0, 0.1, 0.0);
                        }
                    } else {
                        // 普通方块正常破坏
                        boolean shouldDrop = level.getRandom().nextFloat() < 0.3f;
                        level.destroyBlock(pos, shouldDrop, tnt);
                    }
                }
            }
            
            // 生成火焰
            if (result.isFire()) {
                for (BlockPos pos : result.getToDestroy()) {
                    if (level.getRandom().nextInt(3) == 0 && level.getBlockState(pos).isAir()) {
                        net.minecraft.world.level.block.state.BlockState belowState = level.getBlockState(pos.below());
                        if (belowState.isSolidRender()) {
                            level.setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.FIRE.defaultBlockState());
                        }
                    }
                }
            }
            
            // 伤害和击退实体
            for (Map.Entry<UUID, Vec3> entry : result.getToHurt().entrySet()) {
                net.minecraft.world.entity.Entity entity = level.getEntity(entry.getKey());
                if (entity != null) {
                    Vec3 knockback = entry.getValue();
                    
                    double distance = entity.position().distanceTo(center);
                    double impact = (1.0 - distance / 8.0) * knockback.length();
                    float damage = (float) Math.max(0, (impact * (impact + 1.0) / 2.0 * 7.0 * 8.0 + 1.0));
                    
                    if (damage > 0) {
                        entity.hurt(level.damageSources().explosion(explosion), damage);
                    }
                    
                    entity.setDeltaMovement(entity.getDeltaMovement().add(knockback));
                }
            }
            
            
        } catch (Exception ex) {
            System.err.println("[AkiAsync-TNT] Error in applyExplosionResults: " + ex.getMessage());
        }
    }
}