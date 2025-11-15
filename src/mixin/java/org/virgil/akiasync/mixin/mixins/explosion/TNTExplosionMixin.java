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
        boolean inWater = !sl.getFluidState(BlockPos.containing(center)).isEmpty();
        
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
                            net.minecraft.world.level.Explosion.BlockInteraction.DESTROY
                        );
                        
                        applyExplosionResults(sl, explosion, result, tnt, center, inWater);
                        
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
                            net.minecraft.world.level.Explosion.BlockInteraction.DESTROY
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
            level.playSound(null, center.x, center.y, center.z, 
                net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE, 
                net.minecraft.sounds.SoundSource.BLOCKS, 4.0F, 
                (1.0F + (level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 0.2F) * 0.7F);
            
            level.addParticle(net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER, 
                center.x, center.y, center.z, 1.0D, 0.0D, 0.0D);
            
            for (int i = 0; i < 8; i++) {
                double offsetX = (level.getRandom().nextDouble() - 0.5) * 2.0;
                double offsetY = (level.getRandom().nextDouble() - 0.5) * 2.0; 
                double offsetZ = (level.getRandom().nextDouble() - 0.5) * 2.0;
                level.addParticle(net.minecraft.core.particles.ParticleTypes.EXPLOSION, 
                    center.x + offsetX, center.y + offsetY, center.z + offsetZ, 
                    offsetX * 0.1, offsetY * 0.1, offsetZ * 0.1);
            }
            
            if (!inWater) {
                for (BlockPos pos : result.getToDestroy()) {
                    net.minecraft.world.level.block.state.BlockState state = level.getBlockState(pos);
                    if (!state.isAir()) {
                        boolean shouldDrop = level.getRandom().nextFloat() < 0.3f;
                        level.destroyBlock(pos, shouldDrop, tnt);
                    }
                }
            }
            
            if (!inWater && result.isFire()) {
                for (BlockPos pos : result.getToDestroy()) {
                    if (level.getRandom().nextInt(3) == 0 && level.getBlockState(pos).isAir()) {
                        net.minecraft.world.level.block.state.BlockState belowState = level.getBlockState(pos.below());
                        if (belowState.isSolidRender()) {
                            level.setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.FIRE.defaultBlockState());
                        }
                    }
                }
            }
            
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