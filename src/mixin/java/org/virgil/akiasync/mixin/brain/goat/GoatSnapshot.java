package org.virgil.akiasync.mixin.brain.goat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;


public final class GoatSnapshot {
    
    private final Vec3 position;
    private final double health;
    private final double maxHealth;
    private final boolean isInWater;
    private final boolean onGround;
    private final long gameTime;
    private final int tickCount;
    private final boolean isBaby;
    private final boolean isScreaming; 
    private final List<EntityInfo> nearbyEntities;
    private final List<PlayerInfo> nearbyPlayers;
    private final boolean canRam; 
    
    private GoatSnapshot(Vec3 position, double health, double maxHealth,
                        boolean isInWater, boolean onGround, long gameTime, int tickCount,
                        boolean isBaby, boolean isScreaming, List<EntityInfo> nearbyEntities,
                        List<PlayerInfo> nearbyPlayers, boolean canRam) {
        this.position = position;
        this.health = health;
        this.maxHealth = maxHealth;
        this.isInWater = isInWater;
        this.onGround = onGround;
        this.gameTime = gameTime;
        this.tickCount = tickCount;
        this.isBaby = isBaby;
        this.isScreaming = isScreaming;
        this.nearbyEntities = nearbyEntities;
        this.nearbyPlayers = nearbyPlayers;
        this.canRam = canRam;
    }
    
    public static GoatSnapshot capture(net.minecraft.world.entity.animal.Animal goat, 
                                      ServerLevel level, int tickCount) {
        Vec3 pos = goat.position();
        double health = goat.getHealth();
        double maxHealth = goat.getMaxHealth();
        boolean isInWater = goat.isInWater();
        boolean onGround = goat.onGround();
        long gameTime = level.getGameTime();
        boolean isBaby = goat.isBaby();
        
        
        boolean isScreaming = false;
        try {
            if (goat instanceof net.minecraft.world.entity.animal.goat.Goat goatEntity) {
                isScreaming = goatEntity.isScreamingGoat();
            }
        } catch (Exception ignored) {
            
        }
        
        boolean canRam = onGround && !isInWater;
        
        
        List<EntityInfo> entities = new ArrayList<>();
        List<LivingEntity> nearbyEntities = level.getEntitiesOfClass(
            LivingEntity.class,
            goat.getBoundingBox().inflate(16.0),
            e -> e != goat && e.isAlive() && !e.isSpectator()
        );
        
        for (LivingEntity entity : nearbyEntities) {
            entities.add(new EntityInfo(
                entity.getUUID(),
                entity.position(),
                goat.distanceToSqr(entity),
                entity instanceof net.minecraft.world.entity.player.Player
            ));
        }
        
        
        List<PlayerInfo> players = new ArrayList<>();
        List<net.minecraft.world.entity.player.Player> nearbyPlayers = 
            level.getEntitiesOfClass(
                net.minecraft.world.entity.player.Player.class,
                goat.getBoundingBox().inflate(16.0),
                p -> !p.isSpectator() && p.isAlive()
            );
        
        for (net.minecraft.world.entity.player.Player player : nearbyPlayers) {
            players.add(new PlayerInfo(
                player.getUUID(),
                player.position(),
                goat.distanceToSqr(player)
            ));
        }
        
        return new GoatSnapshot(pos, health, maxHealth, isInWater, onGround,
                               gameTime, tickCount, isBaby, isScreaming, entities,
                               players, canRam);
    }
    
    public Vec3 position() { return position; }
    public double health() { return health; }
    public double maxHealth() { return maxHealth; }
    public boolean isInWater() { return isInWater; }
    public boolean onGround() { return onGround; }
    public long gameTime() { return gameTime; }
    public int tickCount() { return tickCount; }
    public boolean isBaby() { return isBaby; }
    public boolean isScreaming() { return isScreaming; }
    public List<EntityInfo> nearbyEntities() { return nearbyEntities; }
    public List<PlayerInfo> nearbyPlayers() { return nearbyPlayers; }
    public boolean canRam() { return canRam; }
    
    public record EntityInfo(UUID id, Vec3 pos, double distanceSq, boolean isPlayer) {}
    public record PlayerInfo(UUID id, Vec3 pos, double distanceSq) {}
}

