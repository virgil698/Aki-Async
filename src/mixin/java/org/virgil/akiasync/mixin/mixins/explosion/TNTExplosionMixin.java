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
        
        // 取消原版爆炸，我们将异步处理
        ci.cancel();
        
        // 使用原有的异步计算器，但基于ServerExplosion的逻辑
        Vec3 center = tnt.position();
        
        // 创建爆炸快照用于异步计算
        org.virgil.akiasync.mixin.async.explosion.ExplosionSnapshot snapshot = 
            new org.virgil.akiasync.mixin.async.explosion.ExplosionSnapshot(sl, center, 4.0F, false);
        
        // 异步计算爆炸影响
        org.virgil.akiasync.mixin.async.TNTThreadPool.getExecutor().execute(() -> {
            try {
                // 在异步线程中进行爆炸计算
                org.virgil.akiasync.mixin.async.explosion.ExplosionCalculator calculator = 
                    new org.virgil.akiasync.mixin.async.explosion.ExplosionCalculator(snapshot);
                org.virgil.akiasync.mixin.async.explosion.ExplosionResult result = calculator.calculate();
                
                // 回到主线程应用结果，确保Bukkit事件在主线程触发
                sl.getServer().execute(() -> {
                    try {
                        // 创建ServerExplosion用于正确的事件处理
                        net.minecraft.world.level.ServerExplosion explosion = new net.minecraft.world.level.ServerExplosion(
                            sl, tnt, null, null, center, 4.0F, false, 
                            net.minecraft.world.level.Explosion.BlockInteraction.DESTROY
                        );
                        
                        // 使用计算结果应用爆炸效果，但通过原版方法确保事件正确触发
                        applyExplosionResults(sl, explosion, result, tnt, center);
                        
                        if (bridge.isTNTDebugEnabled()) {
                            System.out.println("[AkiAsync-TNT] Async explosion completed at " + center);
                        }
                    } catch (Exception ex) {
                        System.err.println("[AkiAsync-TNT] Error applying explosion results: " + ex.getMessage());
                    }
                });
            } catch (Exception ex) {
                System.err.println("[AkiAsync-TNT] Error in async explosion calculation: " + ex.getMessage());
                // 如果异步失败，回退到原版同步爆炸
                sl.getServer().execute(() -> {
                    try {
                        net.minecraft.world.level.ServerExplosion explosion = new net.minecraft.world.level.ServerExplosion(
                            sl, tnt, null, null, center, 4.0F, false, 
                            net.minecraft.world.level.Explosion.BlockInteraction.DESTROY
                        );
                        explosion.explode();
                    } catch (Exception fallbackEx) {
                        System.err.println("[AkiAsync-TNT] Fallback explosion failed: " + fallbackEx.getMessage());
                    }
                });
            }
        });
        
        // 立即移除TNT实体
        tnt.discard();
    }
    
    /**
     * 应用异步计算的爆炸结果，确保在主线程中正确触发Bukkit事件
     */
    private static void applyExplosionResults(ServerLevel level, 
                                            net.minecraft.world.level.ServerExplosion explosion,
                                            org.virgil.akiasync.mixin.async.explosion.ExplosionResult result,
                                            PrimedTnt tnt, Vec3 center) {
        try {
            // 播放爆炸音效和粒子特效 - 完全符合原版
            level.playSound(null, center.x, center.y, center.z, 
                net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE, 
                net.minecraft.sounds.SoundSource.BLOCKS, 4.0F, 
                (1.0F + (level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 0.2F) * 0.7F);
            
            // 大爆炸粒子效果 - 主要特效
            level.addParticle(net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER, 
                center.x, center.y, center.z, 1.0D, 0.0D, 0.0D);
            
            // 额外的爆炸粒子 - 增强视觉效果
            for (int i = 0; i < 8; i++) {
                double offsetX = (level.getRandom().nextDouble() - 0.5) * 2.0;
                double offsetY = (level.getRandom().nextDouble() - 0.5) * 2.0; 
                double offsetZ = (level.getRandom().nextDouble() - 0.5) * 2.0;
                level.addParticle(net.minecraft.core.particles.ParticleTypes.EXPLOSION, 
                    center.x + offsetX, center.y + offsetY, center.z + offsetZ, 
                    offsetX * 0.1, offsetY * 0.1, offsetZ * 0.1);
            }
            
            // 方块破坏 - 使用原版方法确保事件正确触发
            for (BlockPos pos : result.getToDestroy()) {
                net.minecraft.world.level.block.state.BlockState state = level.getBlockState(pos);
                if (!state.isAir()) {
                    // 使用原版的destroyBlock方法，确保触发正确的事件
                    boolean shouldDrop = level.getRandom().nextFloat() < 0.3f; // 30%掉落率
                    level.destroyBlock(pos, shouldDrop, tnt);
                }
            }
            
            // 火焰生成
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
            
            // 实体伤害 - 使用原版方法确保事件正确触发
            for (Map.Entry<UUID, Vec3> entry : result.getToHurt().entrySet()) {
                net.minecraft.world.entity.Entity entity = level.getEntity(entry.getKey());
                if (entity != null) {
                    Vec3 knockback = entry.getValue();
                    
                    // 计算伤害
                    double distance = entity.position().distanceTo(center);
                    double impact = (1.0 - distance / 8.0) * knockback.length();
                    float damage = (float) Math.max(0, (impact * (impact + 1.0) / 2.0 * 7.0 * 8.0 + 1.0));
                    
                    // 使用原版伤害方法，确保触发正确的事件
                    if (damage > 0) {
                        entity.hurt(level.damageSources().explosion(explosion), damage);
                    }
                    
                    // 应用击退
                    entity.setDeltaMovement(entity.getDeltaMovement().add(knockback));
                }
            }
            
        } catch (Exception ex) {
            System.err.println("[AkiAsync-TNT] Error in applyExplosionResults: " + ex.getMessage());
        }
    }
}