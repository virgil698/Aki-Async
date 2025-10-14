package org.virgil.akiasync.mixin.brain;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Witch snapshot (1.21.8 specific)
 * 
 * Snapshot contents (main thread < 0.05 ms):
 * 1. health - Current health value
 * 2. activeEffects - Active potion effects list  
 * 3. nearbyPlayers - Players within 16 blocks (UUID + position + health%)
 * 4. drinkCd - DRINK_POTION_RECENTLY cooldown flag
 * 
 * @author Virgil
 */
public final class WitchSnapshot {
    
    // Health snapshot
    private final double health;
    
    // Active effects snapshot
    private final List<String> activeEffects;
    
    // Nearby players snapshot (UUID + position + health%)
    private final List<PlayerHealthInfo> nearbyPlayers;
    
    // Drink cooldown flag (Boolean)
    private final boolean drinkCd;
    
    private WitchSnapshot(
            double health,
            List<String> effects,
            List<PlayerHealthInfo> players,
            boolean drinkCd
    ) {
        this.health = health;
        this.activeEffects = effects;
        this.nearbyPlayers = players;
        this.drinkCd = drinkCd;
    }
    
    /**
     * Capture witch snapshot (main thread invocation)
     * 
     * @param witch Witch entity
     * @param level ServerLevel
     * @return Snapshot
     */
    public static WitchSnapshot capture(Witch witch, ServerLevel level) {
        // 1. Health
        double health = witch.getHealth();
        
        // 2. Active effects (effect names only for comparison)
        List<String> effects = witch.getActiveEffects().stream()
            .map(effect -> effect.getEffect().value().getDescriptionId())
            .collect(Collectors.toList());
        
        // 3. Scan players within 16 blocks (UUID + position + health%)
        AABB scanBox = witch.getBoundingBox().inflate(16.0);
        List<PlayerHealthInfo> players = level.getEntitiesOfClass(
            net.minecraft.world.entity.player.Player.class,
            scanBox
        ).stream()
            .map(player -> new PlayerHealthInfo(
                player.getUUID(),
                player.blockPosition(),
                player.getHealth() / player.getMaxHealth()
            ))
            .collect(Collectors.toList());
        
        // 4. Drink cooldown (simplified: assume no CD initially)
        // Witches don't use Brain system in 1.21.8, use Goal system instead
        boolean drinkCd = false;
        
        return new WitchSnapshot(health, effects, players, drinkCd);
    }
    
    // Getters
    public double getHealth() { return health; }
    public List<String> getActiveEffects() { return activeEffects; }
    public List<PlayerHealthInfo> getNearbyPlayers() { return nearbyPlayers; }
    public boolean isDrinkCd() { return drinkCd; }
    
    /**
     * Player health info (virtual reference: UUID + BlockPos + health%)
     */
    public static class PlayerHealthInfo {
        final UUID playerId;
        final BlockPos pos;
        final float healthPercent;
        
        public PlayerHealthInfo(UUID id, BlockPos pos, float healthPercent) {
            this.playerId = id;
            this.pos = pos;
            this.healthPercent = healthPercent;
        }
        
        public UUID playerId() { return playerId; }
        public BlockPos pos() { return pos; }
        public float healthPercent() { return healthPercent; }
    }
}

