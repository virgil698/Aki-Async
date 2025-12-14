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
import org.virgil.akiasync.mixin.util.BridgeConfigCache;
import org.virgil.akiasync.mixin.bridge.Bridge;
import org.virgil.akiasync.mixin.bridge.BridgeManager;
import java.util.Map;
import java.util.UUID;
@SuppressWarnings("unused")
@Mixin(value = PrimedTnt.class, priority = 1200)
public class TNTExplosionMixin {
    @Inject(method = "explode", at = @At("HEAD"), cancellable = true)
    private void aki$asyncExplosion(CallbackInfo ci) {
        PrimedTnt tnt = (PrimedTnt) (Object) this;
        Level level = tnt.level();
        
        if (!(level instanceof ServerLevel sl)) {
            return;
        }

        Bridge bridge = BridgeManager.getBridge();
        if (bridge == null) {
            BridgeConfigCache.errorLog("[AkiAsync-TNT] Bridge is null!");
            return;
        }
        
        boolean tntEnabled = bridge.isTNTOptimizationEnabled();
        
        if (!tntEnabled) {
            if (bridge.isTNTDebugEnabled()) {
                bridge.debugLog("[AkiAsync-TNT] TNT optimization is disabled, using vanilla logic");
            }
            return;
        }

        String entityId = tnt.getEncodeId();
        
        if (entityId == null) {
            if (bridge.isTNTDebugEnabled()) {
                bridge.debugLog("[AkiAsync-TNT] Entity ID is null, using vanilla logic");
            }
            return;
        }
        
        java.util.Set<String> whitelist = bridge.getTNTExplosionEntities();
        
        if (!whitelist.contains(entityId)) {
            if (bridge.isTNTDebugEnabled()) {
                bridge.debugLog("[AkiAsync-TNT] Entity %s not in whitelist, using vanilla logic", entityId);
            }
            return;
        }
        
        if (bridge.isTNTDebugEnabled()) {
            BridgeConfigCache.debugLog("[AkiAsync-TNT] ===== TNT EXPLOSION START =====");
            BridgeConfigCache.debugLog("[AkiAsync-TNT] Entity: " + entityId + " at " + tnt.position());
        }

        Vec3 velocity = tnt.getDeltaMovement();
        double speed = velocity.lengthSqr();
        int fuse = tnt.getFuse();
        
        double horizontalSpeed = velocity.x * velocity.x + velocity.z * velocity.z;
        boolean isPistonPushed = speed > 0.1 || horizontalSpeed > 0.05;
        
        if (isPistonPushed) {
            if (bridge.isTNTDebugEnabled()) {
                BridgeConfigCache.debugLog("[AkiAsync-TNT] Skipping optimization for piston-pushed TNT: speed=" + 
                    String.format("%.3f", Math.sqrt(speed)) + ", horizontal=" + String.format("%.3f", Math.sqrt(horizontalSpeed)) + 
                    ", fuse=" + fuse);
            }
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

        boolean inWater = tnt.isInWater() || 
                          tnt.isUnderWater() || 
                          !sl.getFluidState(BlockPos.containing(center)).isEmpty();

        if (inWater) {
            if (bridge.isTNTDebugEnabled()) {
                BridgeConfigCache.debugLog("[AkiAsync-TNT] TNT in water at " + center + ", using vanilla explosion (no block damage)");
            }
            
            ci.cancel();
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
            Bridge bridge = BridgeManager.getBridge();
            
            level.playSound(null, center.x, center.y, center.z,
                net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE,
                net.minecraft.sounds.SoundSource.BLOCKS, 4.0F,
                (1.0F + (level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 0.2F) * 0.7F);

            level.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER,
                center.x, center.y, center.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);

            for (int i = 0; i < 16; i++) {
                double offsetX = (level.getRandom().nextDouble() - 0.5) * 4.0;
                double offsetY = (level.getRandom().nextDouble() - 0.5) * 4.0;
                double offsetZ = (level.getRandom().nextDouble() - 0.5) * 4.0;
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION,
                    center.x + offsetX, center.y + offsetY, center.z + offsetZ,
                    1, offsetX * 0.15, offsetY * 0.15, offsetZ * 0.15, 0.0D);
            }

            for (int i = 0; i < 12; i++) {
                double offsetX = (level.getRandom().nextDouble() - 0.5) * 6.0;
                double offsetY = level.getRandom().nextDouble() * 3.0;
                double offsetZ = (level.getRandom().nextDouble() - 0.5) * 6.0;
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,
                    center.x + offsetX, center.y + offsetY, center.z + offsetZ,
                    1, offsetX * 0.05, 0.1, offsetZ * 0.05, 0.0D);
            }

            for (int i = 0; i < 20; i++) {
                double offsetX = (level.getRandom().nextDouble() - 0.5) * 3.0;
                double offsetY = (level.getRandom().nextDouble() - 0.5) * 3.0;
                double offsetZ = (level.getRandom().nextDouble() - 0.5) * 3.0;
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.LAVA,
                    center.x + offsetX, center.y + offsetY, center.z + offsetZ,
                    1, offsetX * 0.2, offsetY * 0.2, offsetZ * 0.2, 0.0D);
            }

            java.util.List<PrimedTnt> tntToSpawn = new java.util.ArrayList<>();
            java.util.Set<BlockPos> blocksToUpdate = new java.util.HashSet<>();
            
            for (BlockPos pos : result.getToDestroy()) {
                net.minecraft.world.level.block.state.BlockState state = level.getBlockState(pos);
                if (!state.isAir()) {
                    if (bridge != null && bridge.isTNTDebugEnabled()) {
                        BridgeConfigCache.debugLog("[AkiAsync-TNT] Destroying block at " + pos + ": " + state.getBlock().getDescriptionId());
                    }

                    if (state.is(net.minecraft.world.level.block.Blocks.TNT)) {
                        
                        level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);

                        double distance = Math.sqrt(pos.distToCenterSqr(center.x, center.y, center.z));
                        int fuseTime = Math.max(10, (int)(distance * 2.0) + level.getRandom().nextInt(10));

                        PrimedTnt primedTnt = new PrimedTnt(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, null);
                        primedTnt.setFuse(fuseTime);

                        double pushX = (pos.getX() + 0.5 - center.x) * 0.1 + (level.getRandom().nextDouble() - 0.5) * 0.1;
                        double pushY = Math.abs(pos.getY() + 0.5 - center.y) * 0.1 + level.getRandom().nextDouble() * 0.2;
                        double pushZ = (pos.getZ() + 0.5 - center.z) * 0.1 + (level.getRandom().nextDouble() - 0.5) * 0.1;
                        primedTnt.setDeltaMovement(pushX, pushY, pushZ);

                        tntToSpawn.add(primedTnt);

                        if (level.getRandom().nextInt(3) == 0) {
                            for (int i = 0; i < 2; i++) {
                                double offsetX = (level.getRandom().nextDouble() - 0.5) * 0.8;
                                double offsetY = level.getRandom().nextDouble() * 0.8;
                                double offsetZ = (level.getRandom().nextDouble() - 0.5) * 0.8;
                                level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME,
                                    pos.getX() + 0.5 + offsetX, pos.getY() + 0.5 + offsetY, pos.getZ() + 0.5 + offsetZ,
                                    1, 0.0, 0.1, 0.0, 0.0D);
                            }
                        }
                    } else {
                        boolean shouldDrop = level.getRandom().nextFloat() < 0.3f;
                        
                        if (shouldDrop) {
                            net.minecraft.world.level.block.Block.dropResources(state, level, pos, level.getBlockEntity(pos), tnt, net.minecraft.world.item.ItemStack.EMPTY);
                        }
                        
                        level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                        blocksToUpdate.add(pos);
                    }
                }
            }
            
            for (PrimedTnt primedTnt : tntToSpawn) {
                level.addFreshEntity(primedTnt);
            }
            
            if (!blocksToUpdate.isEmpty()) {
                java.util.Set<BlockPos> boundaryBlocks = new java.util.HashSet<>();
                for (BlockPos pos : blocksToUpdate) {
                    
                    for (net.minecraft.core.Direction direction : net.minecraft.core.Direction.values()) {
                        BlockPos neighbor = pos.relative(direction);
                        if (!blocksToUpdate.contains(neighbor)) {
                            boundaryBlocks.add(neighbor);
                        }
                    }
                }
                
                for (BlockPos pos : boundaryBlocks) {
                    level.updateNeighborsAt(pos, net.minecraft.world.level.block.Blocks.AIR);
                }
            }

            if (result.isFire()) {
                java.util.List<BlockPos> firePositions = new java.util.ArrayList<>();
                for (BlockPos pos : result.getToDestroy()) {
                    if (level.getRandom().nextInt(3) == 0 && level.getBlockState(pos).isAir()) {
                        net.minecraft.world.level.block.state.BlockState belowState = level.getBlockState(pos.below());
                        if (belowState.isSolidRender()) {
                            firePositions.add(pos);
                        }
                    }
                }
                
                for (BlockPos pos : firePositions) {
                    level.setBlock(pos, net.minecraft.world.level.block.Blocks.FIRE.defaultBlockState(), 2);
                }
            }

            if (bridge != null && bridge.isTNTDebugEnabled()) {
                bridge.debugLog("[AkiAsync-TNT] Processing %d entities for damage", result.getToHurt().size());
                bridge.debugLog("[AkiAsync-TNT] Explosion center: %s", center);
            }

            int entitiesFound = 0;
            int entitiesNotFound = 0;
            int entitiesRecovered = 0;
            
            for (Map.Entry<UUID, Vec3> entry : result.getToHurt().entrySet()) {
                net.minecraft.world.entity.Entity entity = level.getEntity(entry.getKey());
                
                if (entity == null) {
                    if (bridge != null && bridge.isTNTDebugEnabled()) {
                        BridgeConfigCache.debugLog("[AkiAsync-TNT] Entity not found by UUID: " + entry.getKey() + ", trying AABB search");
                    }
                    
                    double searchRadius = 8.0;
                    java.util.List<net.minecraft.world.entity.Entity> nearbyEntities = level.getEntities(
                        null,
                        new net.minecraft.world.phys.AABB(
                            center.x - searchRadius, center.y - searchRadius, center.z - searchRadius,
                            center.x + searchRadius, center.y + searchRadius, center.z + searchRadius
                        )
                    );
                    
                    if (bridge != null && bridge.isTNTDebugEnabled()) {
                        BridgeConfigCache.debugLog("[AkiAsync-TNT] AABB search found " + nearbyEntities.size() + " nearby entities");
                    }
                    
                    for (net.minecraft.world.entity.Entity e : nearbyEntities) {
                        if (e.getUUID().equals(entry.getKey())) {
                            entity = e;
                            entitiesRecovered++;
                            if (bridge != null && bridge.isTNTDebugEnabled()) {
                                BridgeConfigCache.debugLog("[AkiAsync-TNT] Recovered entity via AABB search: " + 
                                    entity.getType().getDescriptionId() + " at " + entity.position());
                            }
                            break;
                        }
                    }
                    
                    if (entity == null) {
                        entitiesNotFound++;
                        if (bridge != null && bridge.isTNTDebugEnabled()) {
                            BridgeConfigCache.debugLog("[AkiAsync-TNT] Entity still not found after AABB search: " + entry.getKey());
                        }
                        continue;
                    }
                }
                
                entitiesFound++;
                
                if (entity != null) {
                    if (bridge != null && bridge.isTNTDebugEnabled()) {
                        BridgeConfigCache.debugLog("[AkiAsync-TNT] Processing entity: " + entity.getType().getDescriptionId() +
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
                            BridgeConfigCache.debugLog("[AkiAsync-TNT] Water damage reduction: " + baseDamage + " -> " + finalDamage);
                        }
                    }

                    if (bridge != null && bridge.isTNTDebugEnabled()) {
                        bridge.debugLog("[AkiAsync-TNT] Applying damage: %.2f to %s (distance: %.2f, inWater: %s)", 
                            finalDamage, entity.getType().getDescriptionId(), distance, entityInWater);
                    }
                    
                    if (finalDamage > 0) {
                        entity.hurt(level.damageSources().explosion(explosion), finalDamage);
                    }

                    Vec3 finalKnockback = entityInWater ? knockback.scale(0.7) : knockback;
                    entity.setDeltaMovement(entity.getDeltaMovement().add(finalKnockback));
                    
                    if (bridge != null && bridge.isTNTDebugEnabled()) {
                        bridge.debugLog("[AkiAsync-TNT] Applied knockback: %s, new velocity: %s", 
                            finalKnockback, entity.getDeltaMovement());
                    }

                    if (entity instanceof net.minecraft.world.entity.LivingEntity livingEntity) {
                        livingEntity.invulnerableTime = Math.max(livingEntity.invulnerableTime, 10);

                        if (bridge != null && bridge.isTNTDebugEnabled()) {
                            String entityName = entity instanceof net.minecraft.server.level.ServerPlayer player ?
                                "Player " + player.getScoreboardName() :
                                entity.getType().getDescriptionId();
                            BridgeConfigCache.debugLog("[AkiAsync-TNT] " + entityName +
                                " damaged: " + finalDamage + " (distance: " + String.format("%.2f", distance) +
                                ", inWater: " + entityInWater + ")");
                        }
                    }
                }
            }
            
            if (bridge != null && bridge.isTNTDebugEnabled()) {
                BridgeConfigCache.debugLog("[AkiAsync-TNT] Entity damage summary: " + 
                    entitiesFound + " found directly, " + 
                    entitiesRecovered + " recovered via AABB, " + 
                    entitiesNotFound + " not found at all");
            }

        } catch (Exception ex) {
            BridgeConfigCache.errorLog("[AkiAsync-TNT] Error in applyExplosionResults: " + ex.getMessage());
        }
    }

    private static boolean aki$isFoliaEnvironment() {
        Bridge bridge = BridgeManager.getBridge();
        return bridge != null && bridge.isFoliaEnvironment();
    }

    private static void aki$scheduleFoliaExplosion(PrimedTnt tnt, ServerLevel sl, Vec3 center,
                                                   Bridge bridge) {
        Runnable explosionTask = () -> bridge.safeExecute(
            () -> aki$executeExplosionInRegion(tnt, sl, center, bridge),
            "Folia scheduled explosion"
        );

        net.minecraft.core.BlockPos centerPos = net.minecraft.core.BlockPos.containing(center);
        bridge.scheduleRegionTask(sl, centerPos, explosionTask);

        tnt.discard();
    }

    private static void aki$executeExplosionInRegion(PrimedTnt tnt, ServerLevel sl, Vec3 center,
                                                     Bridge bridge) {
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
                BridgeConfigCache.debugLog("[AkiAsync-TNT] Folia region explosion completed at " + center);
            }

        } catch (Exception ex) {
            BridgeConfigCache.errorLog("[AkiAsync-TNT] Error in Folia region explosion: " + ex.getMessage());
            aki$fallbackSyncExplosion(tnt, sl, center);
        }
    }

    private static void aki$fallbackSyncExplosion(PrimedTnt tnt, ServerLevel sl, Vec3 center) {
        try {
            
            sl.explode(
                tnt,
                net.minecraft.world.level.Explosion.getDefaultDamageSource(sl, tnt),
                null,
                center.x, center.y, center.z,
                4.0F,
                false,
                net.minecraft.world.level.Level.ExplosionInteraction.TNT
            );
            
            Bridge bridge = BridgeManager.getBridge();
            if (bridge != null && bridge.isTNTDebugEnabled()) {
                bridge.debugLog("[AkiAsync-TNT] Fallback explosion completed at " + center);
            }
        } catch (Exception fallbackEx) {
            BridgeConfigCache.errorLog("[AkiAsync-TNT] Fallback explosion failed: " + fallbackEx.getMessage());
        }
    }

    private static void aki$executeSafeExplosion(PrimedTnt tnt, ServerLevel sl, Vec3 center,
                                                boolean inWater, Bridge bridge) {
        try {
            
            if (tnt == null) {
                BridgeConfigCache.errorLog("[AkiAsync-TNT] TNT entity is null, aborting");
                return;
            }
            if (sl == null) {
                BridgeConfigCache.errorLog("[AkiAsync-TNT] ServerLevel is null, aborting");
                return;
            }
            if (center == null) {
                BridgeConfigCache.errorLog("[AkiAsync-TNT] Explosion center is null, aborting");
                return;
            }
            if (tnt.isRemoved()) {
                if (bridge != null && bridge.isTNTDebugEnabled()) {
                    bridge.debugLog("[AkiAsync-TNT] TNT entity already removed, skipping");
                }
                return;
            }
            
            if (bridge != null && bridge.isTNTDebugEnabled()) {
                bridge.debugLog("[AkiAsync-TNT] Collecting entities in main thread before async calculation");
            }
            
            org.virgil.akiasync.mixin.async.explosion.ExplosionSnapshot snapshot =
                new org.virgil.akiasync.mixin.async.explosion.ExplosionSnapshot(sl, center, 4.0F, false);

            java.util.concurrent.ExecutorService executor = org.virgil.akiasync.mixin.async.TNTThreadPool.getExecutor();
            String healthStatus = bridge.checkExecutorHealth(executor, "TNT");

            if (healthStatus != null && healthStatus.contains("unhealthy")) {
                if (bridge != null && bridge.isTNTDebugEnabled()) {
                    BridgeConfigCache.debugLog("[AkiAsync-TNT] Executor unhealthy, using sync calculation: " + healthStatus);
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
                            
                            org.virgil.akiasync.mixin.async.explosion.ExplosionCalculator.releaseResult(result);

                            if (bridge != null && bridge.isTNTDebugEnabled()) {
                                BridgeConfigCache.debugLog("[AkiAsync-TNT] Safe async explosion completed at " + center);
                            }
                        } catch (Exception ex) {
                            BridgeConfigCache.errorLog("[AkiAsync-TNT] Error applying safe explosion results: " + ex.getMessage());
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
                    
                    BridgeConfigCache.errorLog("[AkiAsync-TNT] Error in safe async explosion calculation:");
                    BridgeConfigCache.errorLog("[AkiAsync-TNT] Exception type: " + ex.getClass().getName());
                    BridgeConfigCache.errorLog("[AkiAsync-TNT] Message: " + (ex.getMessage() != null ? ex.getMessage() : "null"));
                    
                    StackTraceElement[] stackTrace = ex.getStackTrace();
                    if (stackTrace != null && stackTrace.length > 0) {
                        BridgeConfigCache.errorLog("[AkiAsync-TNT] Stack trace:");
                        for (int i = 0; i < Math.min(5, stackTrace.length); i++) {
                            BridgeConfigCache.errorLog("[AkiAsync-TNT]   at " + stackTrace[i].toString());
                        }
                    }
                    
                    Throwable cause = ex.getCause();
                    if (cause != null) {
                        BridgeConfigCache.errorLog("[AkiAsync-TNT] Caused by: " + cause.getClass().getName() + 
                            ": " + (cause.getMessage() != null ? cause.getMessage() : "null"));
                    }
                    
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
            
            BridgeConfigCache.errorLog("[AkiAsync-TNT] Safe explosion setup failed:");
            BridgeConfigCache.errorLog("[AkiAsync-TNT] Exception type: " + e.getClass().getName());
            BridgeConfigCache.errorLog("[AkiAsync-TNT] Message: " + (e.getMessage() != null ? e.getMessage() : "null"));
            
            StackTraceElement[] stackTrace = e.getStackTrace();
            if (stackTrace != null && stackTrace.length > 0) {
                BridgeConfigCache.errorLog("[AkiAsync-TNT] Stack trace:");
                for (int i = 0; i < Math.min(5, stackTrace.length); i++) {
                    BridgeConfigCache.errorLog("[AkiAsync-TNT]   at " + stackTrace[i].toString());
                }
            }
            
            aki$fallbackSyncExplosion(tnt, sl, center);
        }
    }

    private static void aki$executeSyncExplosion(PrimedTnt tnt, ServerLevel sl, Vec3 center,
                                               org.virgil.akiasync.mixin.async.explosion.ExplosionSnapshot snapshot,
                                               Bridge bridge) {
        try {
            org.virgil.akiasync.mixin.async.explosion.ExplosionCalculator calculator =
                new org.virgil.akiasync.mixin.async.explosion.ExplosionCalculator(snapshot);
            org.virgil.akiasync.mixin.async.explosion.ExplosionResult result = calculator.calculate();

            net.minecraft.world.level.ServerExplosion explosion = new net.minecraft.world.level.ServerExplosion(
                sl, tnt, null, null, center, 4.0F, false,
                net.minecraft.world.level.Explosion.BlockInteraction.DESTROY_WITH_DECAY
            );

            applyExplosionResults(sl, explosion, result, tnt, center, false);
            
            org.virgil.akiasync.mixin.async.explosion.ExplosionCalculator.releaseResult(result);

            if (bridge != null && bridge.isTNTDebugEnabled()) {
                BridgeConfigCache.debugLog("[AkiAsync-TNT] Sync explosion completed at " + center);
            }
        } catch (Exception ex) {
            BridgeConfigCache.errorLog("[AkiAsync-TNT] Sync explosion calculation failed: " + ex.getMessage());
            aki$fallbackSyncExplosion(tnt, sl, center);
        }
    }
}
