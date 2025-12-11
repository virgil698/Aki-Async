package org.virgil.akiasync.mixin.brain.warden;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import org.virgil.akiasync.mixin.brain.core.AiQueryHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class WardenSnapshot {
    
    private final double health;
    private final BlockPos position;
    private final int angerLevel;
    private final List<EntityInfo> nearbyEntities;
    private final List<PlayerInfo> nearbyPlayers;
    private final boolean isDigging;
    private final boolean isEmerging;
    private final int attackCooldown;
    private final long gameTime;
    
    private WardenSnapshot(
        double health,
        BlockPos position,
        int angerLevel,
        List<EntityInfo> entities,
        List<PlayerInfo> players,
        boolean isDigging,
        boolean isEmerging,
        int attackCooldown,
        long gameTime
    ) {
        this.health = health;
        this.position = position;
        this.angerLevel = angerLevel;
        this.nearbyEntities = entities;
        this.nearbyPlayers = players;
        this.isDigging = isDigging;
        this.isEmerging = isEmerging;
        this.attackCooldown = attackCooldown;
        this.gameTime = gameTime;
    }
    
    public static WardenSnapshot capture(Warden warden, ServerLevel level) {
        double health = warden.getHealth();
        BlockPos position = warden.blockPosition();
        long gameTime = level.getGameTime();
        
        int angerLevel = 0; 
        
        List<EntityInfo> entities = new ArrayList<>();
        for (LivingEntity entity : AiQueryHelper.getNearbyEntities(warden, LivingEntity.class, 24.0)) {
            if (entity instanceof Warden) continue; 
            
            entities.add(new EntityInfo(
                entity.getUUID(),
                entity.blockPosition(),
                entity.distanceToSqr(warden),
                entity instanceof Player
            ));
        }
        
        List<PlayerInfo> players = new ArrayList<>();
        for (Player player : AiQueryHelper.getNearbyPlayers(warden, 32.0)) {
            players.add(new PlayerInfo(
                player.getUUID(),
                player.blockPosition(),
                player.distanceToSqr(warden),
                player.isShiftKeyDown()
            ));
        }
        
        boolean isDigging = false; 
        boolean isEmerging = false; 
        
        int attackCooldown = 0; 
        
        return new WardenSnapshot(
            health, position, angerLevel, entities, players,
            isDigging, isEmerging, attackCooldown, gameTime
        );
    }
    
    public double getHealth() { return health; }
    public BlockPos getPosition() { return position; }
    public int getAngerLevel() { return angerLevel; }
    public List<EntityInfo> getNearbyEntities() { return nearbyEntities; }
    public List<PlayerInfo> getNearbyPlayers() { return nearbyPlayers; }
    public boolean isDigging() { return isDigging; }
    public boolean isEmerging() { return isEmerging; }
    public int getAttackCooldown() { return attackCooldown; }
    public long getGameTime() { return gameTime; }
    
    public static class EntityInfo {
        private final UUID entityId;
        private final BlockPos position;
        private final double distanceSq;
        private final boolean isPlayer;
        
        public EntityInfo(UUID entityId, BlockPos position, double distanceSq, boolean isPlayer) {
            this.entityId = entityId;
            this.position = position;
            this.distanceSq = distanceSq;
            this.isPlayer = isPlayer;
        }
        
        public UUID getEntityId() { return entityId; }
        public BlockPos getPosition() { return position; }
        public double getDistanceSq() { return distanceSq; }
        public boolean isPlayer() { return isPlayer; }
    }
    
    public static class PlayerInfo {
        private final UUID playerId;
        private final BlockPos position;
        private final double distanceSq;
        private final boolean isSneaking;
        
        public PlayerInfo(UUID playerId, BlockPos position, double distanceSq, boolean isSneaking) {
            this.playerId = playerId;
            this.position = position;
            this.distanceSq = distanceSq;
            this.isSneaking = isSneaking;
        }
        
        public UUID getPlayerId() { return playerId; }
        public BlockPos getPosition() { return position; }
        public double getDistanceSq() { return distanceSq; }
        public boolean isSneaking() { return isSneaking; }
    }
}
