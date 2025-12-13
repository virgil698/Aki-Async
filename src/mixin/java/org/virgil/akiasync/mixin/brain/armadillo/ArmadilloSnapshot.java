package org.virgil.akiasync.mixin.brain.armadillo;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;


public final class ArmadilloSnapshot {
    
    private final Vec3 position;
    private final double health;
    private final double maxHealth;
    private final boolean isInWater;
    private final boolean isInLava;
    private final boolean isOnFire;
    private final boolean onGround;
    private final long gameTime;
    private final int tickCount;
    private final boolean hasTarget;
    private final List<ThreatInfo> nearbyThreats;
    private final boolean isBaby;
    
    private ArmadilloSnapshot(Vec3 position, double health, double maxHealth,
                             boolean isInWater, boolean isInLava, boolean isOnFire,
                             boolean onGround, long gameTime, int tickCount,
                             boolean hasTarget, List<ThreatInfo> nearbyThreats,
                             boolean isBaby) {
        this.position = position;
        this.health = health;
        this.maxHealth = maxHealth;
        this.isInWater = isInWater;
        this.isInLava = isInLava;
        this.isOnFire = isOnFire;
        this.onGround = onGround;
        this.gameTime = gameTime;
        this.tickCount = tickCount;
        this.hasTarget = hasTarget;
        this.nearbyThreats = nearbyThreats;
        this.isBaby = isBaby;
    }
    
    public static ArmadilloSnapshot capture(net.minecraft.world.entity.animal.Animal armadillo, 
                                           ServerLevel level, int tickCount) {
        Vec3 pos = armadillo.position();
        double health = armadillo.getHealth();
        double maxHealth = armadillo.getMaxHealth();
        boolean isInWater = armadillo.isInWater();
        boolean isInLava = armadillo.isInLava();
        boolean isOnFire = armadillo.isOnFire();
        boolean onGround = armadillo.onGround();
        long gameTime = level.getGameTime();
        boolean hasTarget = armadillo.getLastHurtByMob() != null;
        boolean isBaby = armadillo.isBaby();
        
        
        List<ThreatInfo> threats = new ArrayList<>();
        List<LivingEntity> nearbyEntities = level.getEntitiesOfClass(
            LivingEntity.class,
            armadillo.getBoundingBox().inflate(10.0),
            e -> e != armadillo && e.isAlive() && !e.isSpectator()
        );
        
        for (LivingEntity entity : nearbyEntities) {
            
            if (entity instanceof net.minecraft.world.entity.player.Player ||
                entity instanceof net.minecraft.world.entity.monster.Monster) {
                threats.add(new ThreatInfo(
                    entity.getUUID(),
                    entity.position(),
                    armadillo.distanceToSqr(entity),
                    entity instanceof net.minecraft.world.entity.player.Player
                ));
            }
        }
        
        return new ArmadilloSnapshot(pos, health, maxHealth, isInWater, isInLava, isOnFire,
                                    onGround, gameTime, tickCount, hasTarget, threats, isBaby);
    }
    
    public Vec3 position() { return position; }
    public double health() { return health; }
    public double maxHealth() { return maxHealth; }
    public boolean isInWater() { return isInWater; }
    public boolean isInLava() { return isInLava; }
    public boolean isOnFire() { return isOnFire; }
    public boolean onGround() { return onGround; }
    public long gameTime() { return gameTime; }
    public int tickCount() { return tickCount; }
    public boolean hasTarget() { return hasTarget; }
    public List<ThreatInfo> nearbyThreats() { return nearbyThreats; }
    public boolean isBaby() { return isBaby; }
    
    public record ThreatInfo(UUID id, Vec3 pos, double distanceSq, boolean isPlayer) {}
}

