package org.virgil.akiasync.mixin.brain.enderman;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class EndermanSnapshot {
    
    private final Vec3 position;
    private final double health;
    private final double maxHealth;
    private final boolean isInWater;
    private final boolean isInLava;
    private final boolean isOnFire;
    private final boolean isBrightOutside;
    private final boolean canSeeSky;
    private final float lightLevel;
    private final long gameTime;
    private final int tickCount;
    private final int targetChangeTime;
    private final boolean hasTarget;
    private final List<PlayerInfo> nearbyPlayers;
    
    private EndermanSnapshot(Vec3 position, double health, double maxHealth,
                            boolean isInWater, boolean isInLava, boolean isOnFire,
                            boolean isBrightOutside, boolean canSeeSky, float lightLevel,
                            long gameTime, int tickCount, int targetChangeTime,
                            boolean hasTarget, List<PlayerInfo> nearbyPlayers) {
        this.position = position;
        this.health = health;
        this.maxHealth = maxHealth;
        this.isInWater = isInWater;
        this.isInLava = isInLava;
        this.isOnFire = isOnFire;
        this.isBrightOutside = isBrightOutside;
        this.canSeeSky = canSeeSky;
        this.lightLevel = lightLevel;
        this.gameTime = gameTime;
        this.tickCount = tickCount;
        this.targetChangeTime = targetChangeTime;
        this.hasTarget = hasTarget;
        this.nearbyPlayers = nearbyPlayers;
    }
    
    public static EndermanSnapshot capture(EnderMan enderman, ServerLevel level, 
                                          int tickCount, int targetChangeTime) {
        Vec3 pos = enderman.position();
        double health = enderman.getHealth();
        double maxHealth = enderman.getMaxHealth();
        boolean isInWater = enderman.isInWater();
        boolean isInLava = enderman.isInLava();
        boolean isOnFire = enderman.isOnFire();
        boolean isBrightOutside = level.isBrightOutside();
        boolean canSeeSky = level.canSeeSky(enderman.blockPosition());
        float lightLevel = enderman.getLightLevelDependentMagicValue();
        long gameTime = level.getGameTime();
        boolean hasTarget = enderman.getTarget() != null;
        
        List<PlayerInfo> players = new ArrayList<>();
        List<net.minecraft.world.entity.player.Player> nearbyPlayers = 
            level.getEntitiesOfClass(
                net.minecraft.world.entity.player.Player.class,
                enderman.getBoundingBox().inflate(64.0),
                p -> !p.isSpectator() && p.isAlive() && !p.isCreative()
            );
        
        for (net.minecraft.world.entity.player.Player player : nearbyPlayers) {
            Vec3 viewVector = player.getViewVector(1.0F).normalize();
            double eyeY = player.getEyeY();
            players.add(new PlayerInfo(
                player.getUUID(),
                player.position(),
                player.distanceToSqr(enderman),
                viewVector,
                eyeY
            ));
        }
        
        return new EndermanSnapshot(pos, health, maxHealth, isInWater, isInLava, isOnFire,
                                   isBrightOutside, canSeeSky, lightLevel, gameTime,
                                   tickCount, targetChangeTime, hasTarget, players);
    }
    
    public Vec3 position() { return position; }
    public double health() { return health; }
    public double maxHealth() { return maxHealth; }
    public boolean isInWater() { return isInWater; }
    public boolean isInLava() { return isInLava; }
    public boolean isOnFire() { return isOnFire; }
    public boolean isBrightOutside() { return isBrightOutside; }
    public boolean canSeeSky() { return canSeeSky; }
    public float lightLevel() { return lightLevel; }
    public long gameTime() { return gameTime; }
    public int tickCount() { return tickCount; }
    public int targetChangeTime() { return targetChangeTime; }
    public boolean hasTarget() { return hasTarget; }
    public List<PlayerInfo> nearbyPlayers() { return nearbyPlayers; }
    
    public record PlayerInfo(java.util.UUID id, Vec3 pos, double distanceSq, Vec3 viewVector, double eyeY) {}
}
