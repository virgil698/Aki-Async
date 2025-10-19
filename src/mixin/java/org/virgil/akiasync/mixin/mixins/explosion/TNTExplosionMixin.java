package org.virgil.akiasync.mixin.mixins.explosion;

import java.util.Map;
import java.util.UUID;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

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
        
        Vec3 center = tnt.position();
        float power = 4.0f;
        boolean createFire = false;
        
        org.virgil.akiasync.mixin.async.explosion.ExplosionSnapshot snapshot = 
            new org.virgil.akiasync.mixin.async.explosion.ExplosionSnapshot(sl, center, power, createFire);
        
        org.virgil.akiasync.mixin.async.TNTThreadPool.getExecutor().execute(() -> {
            try {
                org.virgil.akiasync.mixin.async.explosion.ExplosionCalculator calculator = 
                    new org.virgil.akiasync.mixin.async.explosion.ExplosionCalculator(snapshot);
                org.virgil.akiasync.mixin.async.explosion.ExplosionResult result = calculator.calculate();
                
                sl.getServer().execute(() -> {
                    try {
                        for (BlockPos pos : result.getToDestroy()) {
                            net.minecraft.world.level.block.state.BlockState state = sl.getBlockState(pos);
                            
                            if (state.getBlock() == net.minecraft.world.level.block.Blocks.FIRE) {
                                java.util.List<net.minecraft.world.entity.item.ItemEntity> items = 
                                    sl.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, 
                                        new net.minecraft.world.phys.AABB(pos));
                                for (net.minecraft.world.entity.item.ItemEntity item : items) {
                                    if (item.fireImmune()) continue;
                                    item.setRemainingFireTicks(100);
                                    item.hurt(sl.damageSources().onFire(), 1.0F);
                                }
                            }
                            
                            sl.destroyBlock(pos, true, tnt);
                            
                            if (result.isFire() && sl.getRandom().nextInt(3) == 0) {
                                if (state.is(net.minecraft.world.level.block.Blocks.FIRE)) {
                                    sl.setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.FIRE.defaultBlockState());
                                }
                            }
                        }
                        
                        for (Map.Entry<UUID, Vec3> entry : result.getToHurt().entrySet()) {
                            net.minecraft.world.entity.Entity entity = sl.getEntity(entry.getKey());
                            if (entity != null) {
                                Vec3 knockback = entry.getValue();
                                entity.setDeltaMovement(entity.getDeltaMovement().add(knockback));
                                
                                float damage = (float) knockback.length() * 7.0f;
                                entity.hurt(sl.damageSources().explosion(tnt, null), damage);
                            }
                        }
                    } catch (Exception ex) {
                        System.err.println("[AkiAsync] TNT writeback error: " + ex.getMessage());
                    }
                });
            } catch (Exception ex) {
                System.err.println("[AkiAsync] TNT async error: " + ex.getMessage());
            }
        });
        
        tnt.discard();
        ci.cancel();
    }
}

