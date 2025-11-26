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

        if (aki$isFoliaEnvironment()) {
            net.minecraft.core.BlockPos centerPos = net.minecraft.core.BlockPos.containing(center);

            if (!bridge.isOwnedByCurrentRegion(sl, centerPos)) {
                aki$scheduleFoliaExplosion(tnt, sl, center, bridge);
                tnt.discard();
                return;
            }

            aki$executeExplosionInRegion(tnt, sl, center, bridge);
            tnt.discard();
            return;
        }

        boolean inWater = tnt.isInWater() || !sl.getFluidState(BlockPos.containing(center)).isEmpty();

        if (inWater) {
            Runnable waterExplosionTask = () -> {
                try {
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
                        bridge.debugLog("[AkiAsync-TNT] Water explosion completed at " + center);
                    }
                } catch (Exception ex) {
                    System.err.println("[AkiAsync-TNT] Error in water explosion: " + ex.getMessage());
                }
            };

            if (aki$isFoliaEnvironment()) {
                net.minecraft.core.BlockPos centerPos = net.minecraft.core.BlockPos.containing(center);
                bridge.scheduleRegionTask(sl, centerPos, waterExplosionTask);
            } else {
                sl.getServer().execute(waterExplosionTask);
            }
            tnt.discard();
            return;
        }

        aki$executeSafeExplosion(tnt, sl, center, false, bridge);

        tnt.discard();
    }

    private static void applyExplosionResults(ServerLevel level,
                                            net.minecraft.world.level.ServerExplosion explosion,
                                            org.virgil.akiasync.mixin.async.explosion.ExplosionResult result,
                                            PrimedTnt tnt, Vec3 center, boolean inWater) {
        try {
            org.virgil.akiasync.mixin.bridge.Bridge bridge =
                org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
            level.playSound(null, center.x, center.y, center.z,
                net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE,
                net.minecraft.sounds.SoundSource.BLOCKS, 4.0F,
                (1.0F + (level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 0.2F) * 0.7F);

            level.addParticle(net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER,
                center.x, center.y, center.z, 1.0D, 0.0D, 0.0D);

            for (int i = 0; i < 16; i++) {
                double offsetX = (level.getRandom().nextDouble() - 0.5) * 4.0;
                double offsetY = (level.getRandom().nextDouble() - 0.5) * 4.0;
                double offsetZ = (level.getRandom().nextDouble() - 0.5) * 4.0;
                level.addParticle(net.minecraft.core.particles.ParticleTypes.EXPLOSION,
                    center.x + offsetX, center.y + offsetY, center.z + offsetZ,
                    offsetX * 0.15, offsetY * 0.15, offsetZ * 0.15);
            }

            for (int i = 0; i < 12; i++) {
                double offsetX = (level.getRandom().nextDouble() - 0.5) * 6.0;
                double offsetY = level.getRandom().nextDouble() * 3.0;
                double offsetZ = (level.getRandom().nextDouble() - 0.5) * 6.0;
                level.addParticle(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,
                    center.x + offsetX, center.y + offsetY, center.z + offsetZ,
                    offsetX * 0.05, 0.1, offsetZ * 0.05);
            }

            for (int i = 0; i < 20; i++) {
                double offsetX = (level.getRandom().nextDouble() - 0.5) * 3.0;
                double offsetY = (level.getRandom().nextDouble() - 0.5) * 3.0;
                double offsetZ = (level.getRandom().nextDouble() - 0.5) * 3.0;
                level.addParticle(net.minecraft.core.particles.ParticleTypes.LAVA,
                    center.x + offsetX, center.y + offsetY, center.z + offsetZ,
                    offsetX * 0.2, offsetY * 0.2, offsetZ * 0.2);
            }

            for (BlockPos pos : result.getToDestroy()) {
                net.minecraft.world.level.block.state.BlockState state = level.getBlockState(pos);
                if (!state.isAir()) {
                    if (bridge != null && bridge.isTNTDebugEnabled()) {
                        bridge.debugLog("[AkiAsync-TNT] Destroying block at " + pos + ": " + state.getBlock().getDescriptionId());
                    }

                    if (state.is(net.minecraft.world.level.block.Blocks.TNT)) {
                        level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 11);

                        double distance = Math.sqrt(pos.distToCenterSqr(center.x, center.y, center.z));
                        int fuseTime = Math.max(10, (int)(distance * 2.0) + level.getRandom().nextInt(10));

                        PrimedTnt primedTnt = new PrimedTnt(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, null);
                        primedTnt.setFuse(fuseTime);

                        double pushX = (pos.getX() + 0.5 - center.x) * 0.1 + (level.getRandom().nextDouble() - 0.5) * 0.1;
                        double pushY = Math.abs(pos.getY() + 0.5 - center.y) * 0.1 + level.getRandom().nextDouble() * 0.2;
                        double pushZ = (pos.getZ() + 0.5 - center.z) * 0.1 + (level.getRandom().nextDouble() - 0.5) * 0.1;
                        primedTnt.setDeltaMovement(pushX, pushY, pushZ);

                        level.addFreshEntity(primedTnt);

                        for (int i = 0; i < 5; i++) {
                            double offsetX = (level.getRandom().nextDouble() - 0.5) * 0.8;
                            double offsetY = level.getRandom().nextDouble() * 0.8;
                            double offsetZ = (level.getRandom().nextDouble() - 0.5) * 0.8;
                            level.addParticle(net.minecraft.core.particles.ParticleTypes.FLAME,
                                pos.getX() + 0.5 + offsetX, pos.getY() + 0.5 + offsetY, pos.getZ() + 0.5 + offsetZ,
                                0.0, 0.1, 0.0);
                        }
                    } else {
                        if (bridge != null && bridge.isTNTUseVanillaBlockDestruction()) {
                            boolean shouldDrop = level.getRandom().nextFloat() < 0.3f;
                            level.destroyBlock(pos, shouldDrop, tnt);
                        } else {
                            boolean shouldDrop = level.getRandom().nextFloat() < 0.3f;
                            level.destroyBlock(pos, shouldDrop, tnt);
                        }
                    }
                }
            }

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

            if (bridge != null && bridge.isTNTDebugEnabled()) {
                bridge.debugLog("[AkiAsync-TNT] Processing " + result.getToHurt().size() + " entities for damage");
            }

            for (Map.Entry<UUID, Vec3> entry : result.getToHurt().entrySet()) {
                net.minecraft.world.entity.Entity entity = level.getEntity(entry.getKey());
                if (entity != null) {
                    if (bridge != null && bridge.isTNTDebugEnabled()) {
                        bridge.debugLog("[AkiAsync-TNT] Processing entity: " + entity.getType().getDescriptionId() +
                            " at " + entity.position());
                    }
                    Vec3 knockback = entry.getValue();

                    double distance = entity.position().distanceTo(center);

                    float baseDamage;
                    if (bridge != null && bridge.isTNTUseVanillaDamageCalculation()) {
                        double impact = Math.max(0.0, (8.0 - distance) / 8.0);
                        baseDamage = (float) (impact * impact + impact) * 7.0F * 4.0F + 1.0F;
                    } else {
                        double impact = (1.0 - distance / 8.0) * knockback.length();
                        baseDamage = (float) Math.max(0, (impact * (impact + 1.0) / 2.0 * 7.0 * 8.0 + 1.0));
                    }

                    boolean entityInWater = entity.isInWater() || !level.getFluidState(BlockPos.containing(entity.position())).isEmpty();
                    float finalDamage = baseDamage;

                    if (entityInWater) {
                        finalDamage = baseDamage * 0.6f;
                        if (bridge != null && bridge.isTNTDebugEnabled()) {
                            bridge.debugLog("[AkiAsync-TNT] Water damage reduction: " + baseDamage + " -> " + finalDamage);
                        }
                    }

                    if (finalDamage > 0) {
                        entity.hurt(level.damageSources().explosion(explosion), finalDamage);
                    }

                    Vec3 finalKnockback = entityInWater ? knockback.scale(0.7) : knockback;
                    entity.setDeltaMovement(entity.getDeltaMovement().add(finalKnockback));

                    if (entity instanceof net.minecraft.world.entity.LivingEntity livingEntity) {
                        livingEntity.invulnerableTime = Math.max(livingEntity.invulnerableTime, 10);

                        if (bridge != null && bridge.isTNTDebugEnabled()) {
                            String entityName = entity instanceof net.minecraft.server.level.ServerPlayer player ?
                                "Player " + player.getScoreboardName() :
                                entity.getType().getDescriptionId();
                            bridge.debugLog("[AkiAsync-TNT] " + entityName +
                                " damaged: " + finalDamage + " (distance: " + String.format("%.2f", distance) +
                                ", inWater: " + entityInWater + ")");
                        }
                    }
                }
            }

        } catch (Exception ex) {
            System.err.println("[AkiAsync-TNT] Error in applyExplosionResults: " + ex.getMessage());
        }
    }

    private static boolean aki$isFoliaEnvironment() {
        org.virgil.akiasync.mixin.bridge.Bridge bridge = org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        return bridge != null && bridge.isFoliaEnvironment();
    }

    private static void aki$scheduleFoliaExplosion(PrimedTnt tnt, ServerLevel sl, Vec3 center,
                                                   org.virgil.akiasync.mixin.bridge.Bridge bridge) {
        Runnable explosionTask = () -> bridge.safeExecute(
            () -> aki$executeExplosionInRegion(tnt, sl, center, bridge),
            "Folia scheduled explosion"
        );

        net.minecraft.core.BlockPos centerPos = net.minecraft.core.BlockPos.containing(center);
        bridge.scheduleRegionTask(sl, centerPos, explosionTask);

        tnt.discard();
    }

    private static void aki$executeExplosionInRegion(PrimedTnt tnt, ServerLevel sl, Vec3 center,
                                                     org.virgil.akiasync.mixin.bridge.Bridge bridge) {
        boolean inWater = tnt.isInWater() || !sl.getFluidState(BlockPos.containing(center)).isEmpty();

        if (inWater) {
            sl.explode(
                tnt,
                net.minecraft.world.level.Explosion.getDefaultDamageSource(sl, tnt),
                null,
                center.x, center.y, center.z,
                4.0F, false,
                net.minecraft.world.level.Level.ExplosionInteraction.TNT
            );
            return;
        }

        try {
            org.virgil.akiasync.mixin.async.explosion.ExplosionSnapshot snapshot =
                new org.virgil.akiasync.mixin.async.explosion.ExplosionSnapshot(sl, center, 4.0F, false);

            aki$executeSyncExplosion(tnt, sl, center, snapshot, bridge);

            if (bridge != null && bridge.isTNTDebugEnabled()) {
                bridge.debugLog("[AkiAsync-TNT] Folia region explosion completed at " + center);
            }

        } catch (Exception ex) {
            if (bridge != null) {
                bridge.errorLog("[AkiAsync-TNT] Error in Folia region explosion: " + ex.getMessage());
            }
            aki$fallbackSyncExplosion(tnt, sl, center);
        }
    }

    private static void aki$fallbackSyncExplosion(PrimedTnt tnt, ServerLevel sl, Vec3 center) {
        try {
            net.minecraft.world.level.ServerExplosion explosion = new net.minecraft.world.level.ServerExplosion(
                sl, tnt, null, null, center, 4.0F, false,
                net.minecraft.world.level.Explosion.BlockInteraction.DESTROY_WITH_DECAY
            );
            explosion.explode();
        } catch (Exception fallbackEx) {
            System.err.println("[AkiAsync-TNT] Fallback explosion also failed: " + fallbackEx.getMessage());
        }
    }

    private static void aki$executeSafeExplosion(PrimedTnt tnt, ServerLevel sl, Vec3 center,
                                                boolean inWater, org.virgil.akiasync.mixin.bridge.Bridge bridge) {
        try {
            org.virgil.akiasync.mixin.async.explosion.ExplosionSnapshot snapshot =
                new org.virgil.akiasync.mixin.async.explosion.ExplosionSnapshot(sl, center, 4.0F, false);

            java.util.concurrent.ExecutorService executor = org.virgil.akiasync.mixin.async.TNTThreadPool.getExecutor();
            String healthStatus = bridge.checkExecutorHealth(executor, "TNT");

            if (healthStatus != null && healthStatus.contains("unhealthy")) {
                if (bridge != null && bridge.isTNTDebugEnabled()) {
                    bridge.debugLog("[AkiAsync-TNT] Executor unhealthy, using sync calculation: " + healthStatus);
                }
                aki$executeSyncExplosion(tnt, sl, center, snapshot, bridge);
                return;
            }

            executor.execute(() -> {
                try {
                    org.virgil.akiasync.mixin.async.explosion.ExplosionCalculator calculator =
                        new org.virgil.akiasync.mixin.async.explosion.ExplosionCalculator(snapshot);
                    org.virgil.akiasync.mixin.async.explosion.ExplosionResult result = calculator.calculate();

                    Runnable applyResultsTask = () -> {
                        try {
                            net.minecraft.world.level.ServerExplosion explosion = new net.minecraft.world.level.ServerExplosion(
                                sl, tnt, null, null, center, 4.0F, false,
                                net.minecraft.world.level.Explosion.BlockInteraction.DESTROY_WITH_DECAY
                            );

                            applyExplosionResults(sl, explosion, result, tnt, center, inWater);

                            if (bridge != null && bridge.isTNTDebugEnabled()) {
                                bridge.debugLog("[AkiAsync-TNT] Safe async explosion completed at " + center);
                            }
                        } catch (Exception ex) {
                            bridge.errorLog("[AkiAsync-TNT] Error applying safe explosion results: " + ex.getMessage());
                            aki$fallbackSyncExplosion(tnt, sl, center);
                        }
                    };

                    if (aki$isFoliaEnvironment()) {
                        net.minecraft.core.BlockPos centerPos = net.minecraft.core.BlockPos.containing(center);
                        bridge.scheduleRegionTask(sl, centerPos, applyResultsTask);
                    } else {
                        sl.getServer().execute(applyResultsTask);
                    }
                } catch (Exception ex) {
                    bridge.errorLog("[AkiAsync-TNT] Error in safe async explosion calculation: " + ex.getMessage());
                    Runnable fallbackTask = () -> aki$fallbackSyncExplosion(tnt, sl, center);
                    if (aki$isFoliaEnvironment()) {
                        net.minecraft.core.BlockPos centerPos = net.minecraft.core.BlockPos.containing(center);
                        bridge.scheduleRegionTask(sl, centerPos, fallbackTask);
                    } else {
                        sl.getServer().execute(fallbackTask);
                    }
                }
            });

        } catch (Exception e) {
            if (bridge != null) {
                bridge.errorLog("[AkiAsync-TNT] Safe explosion setup failed: " + e.getMessage());
            }
            aki$fallbackSyncExplosion(tnt, sl, center);
        }
    }

    private static void aki$executeSyncExplosion(PrimedTnt tnt, ServerLevel sl, Vec3 center,
                                               org.virgil.akiasync.mixin.async.explosion.ExplosionSnapshot snapshot,
                                               org.virgil.akiasync.mixin.bridge.Bridge bridge) {
        try {
            org.virgil.akiasync.mixin.async.explosion.ExplosionCalculator calculator =
                new org.virgil.akiasync.mixin.async.explosion.ExplosionCalculator(snapshot);
            org.virgil.akiasync.mixin.async.explosion.ExplosionResult result = calculator.calculate();

            net.minecraft.world.level.ServerExplosion explosion = new net.minecraft.world.level.ServerExplosion(
                sl, tnt, null, null, center, 4.0F, false,
                net.minecraft.world.level.Explosion.BlockInteraction.DESTROY_WITH_DECAY
            );

            applyExplosionResults(sl, explosion, result, tnt, center, false);

            if (bridge != null && bridge.isTNTDebugEnabled()) {
                bridge.debugLog("[AkiAsync-TNT] Sync explosion completed at " + center);
            }
        } catch (Exception ex) {
            if (bridge != null) {
                bridge.errorLog("[AkiAsync-TNT] Sync explosion calculation failed: " + ex.getMessage());
            }
            aki$fallbackSyncExplosion(tnt, sl, center);
        }
    }
}