package org.virgil.akiasync.mixin.brain.frog;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;


public final class FrogSnapshot {
    
    private final Vec3 position;
    private final double health;
    private final double maxHealth;
    private final boolean isInWater;
    private final boolean isInLava;
    private final boolean onGround;
    private final long gameTime;
    private final int tickCount;
    private final boolean isBaby;
    private final List<SlimeInfo> nearbySlimes;
    private final List<PlayerInfo> nearbyPlayers;
    private final boolean canJump;
    
    private FrogSnapshot(Vec3 position, double health, double maxHealth,
                        boolean isInWater, boolean isInLava, boolean onGround,
                        long gameTime, int tickCount, boolean isBaby,
                        List<SlimeInfo> nearbySlimes, List<PlayerInfo> nearbyPlayers,
                        boolean canJump) {
        this.position = position;
        this.health = health;
        this.maxHealth = maxHealth;
        this.isInWater = isInWater;
        this.isInLava = isInLava;
        this.onGround = onGround;
        this.gameTime = gameTime;
        this.tickCount = tickCount;
        this.isBaby = isBaby;
        this.nearbySlimes = nearbySlimes;
        this.nearbyPlayers = nearbyPlayers;
        this.canJump = canJump;
    }
    
    public static FrogSnapshot capture(net.minecraft.world.entity.animal.Animal frog, 
                                      ServerLevel level, int tickCount) {
        Vec3 pos = frog.position();
        double health = frog.getHealth();
        double maxHealth = frog.getMaxHealth();
        boolean isInWater = frog.isInWater();
        boolean isInLava = frog.isInLava();
        boolean onGround = frog.onGround();
        long gameTime = level.getGameTime();
        boolean isBaby = frog.isBaby();
        boolean canJump = onGround && !isInWater;
        
        
        List<SlimeInfo> slimes = new ArrayList<>();
        List<LivingEntity> nearbyEntities = level.getEntitiesOfClass(
            LivingEntity.class,
            frog.getBoundingBox().inflate(10.0),
            e -> e != frog && e.isAlive() && 
                 (e instanceof net.minecraft.world.entity.monster.Slime ||
                  e instanceof net.minecraft.world.entity.monster.MagmaCube)
        );
        
        for (LivingEntity entity : nearbyEntities) {
            slimes.add(new SlimeInfo(
                entity.getUUID(),
                entity.position(),
                frog.distanceToSqr(entity),
                entity.getBbWidth() 
            ));
        }
        
        
        List<PlayerInfo> players = new ArrayList<>();
        List<net.minecraft.world.entity.player.Player> nearbyPlayers = 
            level.getEntitiesOfClass(
                net.minecraft.world.entity.player.Player.class,
                frog.getBoundingBox().inflate(16.0),
                p -> !p.isSpectator() && p.isAlive()
            );
        
        for (net.minecraft.world.entity.player.Player player : nearbyPlayers) {
            players.add(new PlayerInfo(
                player.getUUID(),
                player.position(),
                frog.distanceToSqr(player)
            ));
        }
        
        return new FrogSnapshot(pos, health, maxHealth, isInWater, isInLava, onGround,
                               gameTime, tickCount, isBaby, slimes, players, canJump);
    }
    
    public Vec3 position() { return position; }
    public double health() { return health; }
    public double maxHealth() { return maxHealth; }
    public boolean isInWater() { return isInWater; }
    public boolean isInLava() { return isInLava; }
    public boolean onGround() { return onGround; }
    public long gameTime() { return gameTime; }
    public int tickCount() { return tickCount; }
    public boolean isBaby() { return isBaby; }
    public List<SlimeInfo> nearbySlimes() { return nearbySlimes; }
    public List<PlayerInfo> nearbyPlayers() { return nearbyPlayers; }
    public boolean canJump() { return canJump; }
    
    public record SlimeInfo(UUID id, Vec3 pos, double distanceSq, float size) {}
    public record PlayerInfo(UUID id, Vec3 pos, double distanceSq) {}
}

