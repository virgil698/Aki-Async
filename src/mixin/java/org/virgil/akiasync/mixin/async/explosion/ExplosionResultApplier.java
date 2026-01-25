package org.virgil.akiasync.mixin.async.explosion;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.virgil.akiasync.mixin.bridge.Bridge;
import org.virgil.akiasync.mixin.bridge.BridgeManager;
import org.virgil.akiasync.mixin.util.BridgeConfigCache;

import java.util.*;

public class ExplosionResultApplier {

    public static void applyExplosionResults(ServerLevel level,
                                            net.minecraft.world.level.ServerExplosion explosion,
                                            ExplosionResult result,
                                            PrimedTnt tnt,
                                            Vec3 center,
                                            boolean inWater) {
        try {
            Bridge bridge = BridgeManager.getBridge();

            playEffects(level, center, bridge);

            List<BlockPos> blocksToDestroy = processBlockDestruction(level, explosion, result, tnt, center, bridge);

            processEntityDamage(level, explosion, result, center, bridge);

            if (result.isFire()) {
                generateFire(level, result, bridge);
            }

        } catch (Exception ex) {
            BridgeConfigCache.errorLog("[AkiAsync-TNT] Error in applyExplosionResults: " + ex.getMessage());
        }
    }

    private static void playEffects(ServerLevel level, Vec3 center, Bridge bridge) {

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
    }

    private static List<BlockPos> processBlockDestruction(ServerLevel level,
                                                          net.minecraft.world.level.ServerExplosion explosion,
                                                          ExplosionResult result,
                                                          PrimedTnt tnt,
                                                          Vec3 center,
                                                          Bridge bridge) {
        Set<BlockPos> blocksToUpdate = new HashSet<>();

        List<BlockPos> blocksToDestroy = new ArrayList<>(result.getToDestroy());
        Collections.shuffle(blocksToDestroy, new Random(level.getRandom().nextLong()));

        if (bridge != null) {
            blocksToDestroy = bridge.fireEntityExplodeEvent(level, tnt, center, blocksToDestroy, 1.0f);
        }

        Map<BlockPos, List<net.minecraft.world.item.ItemStack>> positionDrops = new HashMap<>();

        for (BlockPos pos : blocksToDestroy) {
            BlockState state = level.getBlockState(pos);
            if (!state.isAir()) {
                if (bridge != null && bridge.isTNTDebugEnabled()) {
                    BridgeConfigCache.debugLog("[AkiAsync-TNT] Destroying block at " + pos + ": " + state.getBlock().getDescriptionId());
                }

                state.onExplosionHit(level, pos, explosion, (itemStack, blockPos1) -> {
                    List<net.minecraft.world.item.ItemStack> drops = positionDrops.computeIfAbsent(blockPos1, k -> new ArrayList<>());

                    boolean merged = false;
                    for (net.minecraft.world.item.ItemStack existing : drops) {
                        if (net.minecraft.world.entity.item.ItemEntity.areMergable(existing, itemStack)) {
                            existing.grow(itemStack.getCount());
                            merged = true;
                            break;
                        }
                    }
                    if (!merged) {
                        drops.add(itemStack.copy());
                    }
                });

                blocksToUpdate.add(pos);
            }
        }

        for (Map.Entry<BlockPos, List<net.minecraft.world.item.ItemStack>> entry : positionDrops.entrySet()) {
            for (net.minecraft.world.item.ItemStack stack : entry.getValue()) {
                if (!stack.isEmpty()) {
                    net.minecraft.world.level.block.Block.popResource(level, entry.getKey(), stack);
                }
            }
        }

        if (!blocksToUpdate.isEmpty()) {
            Set<BlockPos> boundaryBlocks = new HashSet<>();
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

        return blocksToDestroy;
    }

    private static void processEntityDamage(ServerLevel level,
                                           net.minecraft.world.level.ServerExplosion explosion,
                                           ExplosionResult result,
                                           Vec3 center,
                                           Bridge bridge) {
        if (bridge != null && bridge.isTNTDebugEnabled()) {
            bridge.debugLog("[AkiAsync-TNT] Processing %d entities for damage", result.getToHurt().size());
        }

        int entitiesFound = 0;
        int entitiesNotFound = 0;
        int entitiesRecovered = 0;

        for (Map.Entry<UUID, Vec3> entry : result.getToHurt().entrySet()) {
            net.minecraft.world.entity.Entity entity = level.getEntity(entry.getKey());

            if (entity == null) {
                entity = recoverEntityByAABB(level, entry.getKey(), center, bridge);
                if (entity != null) {
                    entitiesRecovered++;
                } else {
                    entitiesNotFound++;
                    continue;
                }
            }

            entitiesFound++;

            applyDamageAndKnockback(level, explosion, entity, entry.getValue(), center, bridge);
        }

        if (bridge != null && bridge.isTNTDebugEnabled()) {
            BridgeConfigCache.debugLog("[AkiAsync-TNT] Entity damage summary: " +
                entitiesFound + " found directly, " +
                entitiesRecovered + " recovered via AABB, " +
                entitiesNotFound + " not found at all");
        }
    }

    private static net.minecraft.world.entity.Entity recoverEntityByAABB(ServerLevel level,
                                                                        UUID entityUUID,
                                                                        Vec3 center,
                                                                        Bridge bridge) {
        double searchRadius = 8.0;
        List<net.minecraft.world.entity.Entity> nearbyEntities = level.getEntities(
            null,
            new net.minecraft.world.phys.AABB(
                center.x - searchRadius, center.y - searchRadius, center.z - searchRadius,
                center.x + searchRadius, center.y + searchRadius, center.z + searchRadius
            )
        );

        for (net.minecraft.world.entity.Entity e : nearbyEntities) {
            if (e.getUUID().equals(entityUUID)) {
                if (bridge != null && bridge.isTNTDebugEnabled()) {
                    BridgeConfigCache.debugLog("[AkiAsync-TNT] Recovered entity via AABB search: " +
                        e.getType().getDescriptionId() + " at " + e.position());
                }
                return e;
            }
        }

        return null;
    }

    private static void applyDamageAndKnockback(ServerLevel level,
                                               net.minecraft.world.level.ServerExplosion explosion,
                                               net.minecraft.world.entity.Entity entity,
                                               Vec3 knockback,
                                               Vec3 center,
                                               Bridge bridge) {
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
        float finalDamage = entityInWater ? baseDamage * 0.6f : baseDamage;

        if (bridge != null && bridge.isTNTDebugEnabled()) {
            bridge.debugLog("[AkiAsync-TNT] Applying damage: %.2f to %s (distance: %.2f, inWater: %s)",
                finalDamage, entity.getType().getDescriptionId(), distance, entityInWater);
        }

        if (finalDamage > 0) {
            entity.hurt(level.damageSources().explosion(explosion), finalDamage);
        }

        Vec3 finalKnockback = entityInWater ? knockback.scale(0.7) : knockback;
        entity.setDeltaMovement(entity.getDeltaMovement().add(finalKnockback));

        if (entity instanceof net.minecraft.world.entity.LivingEntity livingEntity) {
            livingEntity.invulnerableTime = Math.max(livingEntity.invulnerableTime, 10);
        }
    }

    private static void generateFire(ServerLevel level, ExplosionResult result, Bridge bridge) {
        boolean useVanillaFireLogic = bridge == null || bridge.isTNTUseVanillaFireLogic();
        List<BlockPos> firePositions = new ArrayList<>();

        for (BlockPos pos : result.getToDestroy()) {

            if (level.getRandom().nextInt(3) == 0 && level.getBlockState(pos).isAir()) {
                BlockState belowState = level.getBlockState(pos.below());
                if (useVanillaFireLogic) {
                    if (belowState.isSolidRender()) {
                        firePositions.add(pos);
                    }
                } else {
                    if (!belowState.isAir()) {
                        firePositions.add(pos);
                    }
                }
            }
        }

        for (BlockPos pos : firePositions) {
            if (useVanillaFireLogic) {
                level.setBlockAndUpdate(pos, net.minecraft.world.level.block.BaseFireBlock.getState(level, pos));
            } else {
                level.setBlock(pos, net.minecraft.world.level.block.Blocks.FIRE.defaultBlockState(), 2);
            }
        }
    }
}
