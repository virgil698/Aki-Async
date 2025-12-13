package org.virgil.akiasync.mixin.brain.camel;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;


public final class CamelSnapshot {
    
    private final Vec3 position;
    private final double health;
    private final double maxHealth;
    private final boolean isInWater;
    private final boolean onGround;
    private final long gameTime;
    private final int tickCount;
    private final boolean isBaby;
    private final boolean hasPassengers;
    private final int passengerCount;
    private final List<PlayerInfo> nearbyPlayers;
    private final boolean isSitting;
    
    private CamelSnapshot(Vec3 position, double health, double maxHealth,
                         boolean isInWater, boolean onGround, long gameTime, int tickCount,
                         boolean isBaby, boolean hasPassengers, int passengerCount,
                         List<PlayerInfo> nearbyPlayers, boolean isSitting) {
        this.position = position;
        this.health = health;
        this.maxHealth = maxHealth;
        this.isInWater = isInWater;
        this.onGround = onGround;
        this.gameTime = gameTime;
        this.tickCount = tickCount;
        this.isBaby = isBaby;
        this.hasPassengers = hasPassengers;
        this.passengerCount = passengerCount;
        this.nearbyPlayers = nearbyPlayers;
        this.isSitting = isSitting;
    }
    
    public static CamelSnapshot capture(net.minecraft.world.entity.animal.Animal camel, 
                                       ServerLevel level, int tickCount) {
        Vec3 pos = camel.position();
        double health = camel.getHealth();
        double maxHealth = camel.getMaxHealth();
        boolean isInWater = camel.isInWater();
        boolean onGround = camel.onGround();
        long gameTime = level.getGameTime();
        boolean isBaby = camel.isBaby();
        
        
        List<Entity> passengers = camel.getPassengers();
        boolean hasPassengers = !passengers.isEmpty();
        int passengerCount = passengers.size();
        
        
        boolean isSitting = false;
        try {
            
            
            if (camel.getDeltaMovement().lengthSqr() < 0.001 && !hasPassengers) {
                isSitting = true;
            }
        } catch (Exception ignored) {
            
        }
        
        
        List<PlayerInfo> players = new ArrayList<>();
        List<net.minecraft.world.entity.player.Player> nearbyPlayers = 
            level.getEntitiesOfClass(
                net.minecraft.world.entity.player.Player.class,
                camel.getBoundingBox().inflate(16.0),
                p -> !p.isSpectator() && p.isAlive()
            );
        
        for (net.minecraft.world.entity.player.Player player : nearbyPlayers) {
            players.add(new PlayerInfo(
                player.getUUID(),
                player.position(),
                camel.distanceToSqr(player)
            ));
        }
        
        return new CamelSnapshot(pos, health, maxHealth, isInWater, onGround,
                                gameTime, tickCount, isBaby, hasPassengers, passengerCount,
                                players, isSitting);
    }
    
    public Vec3 position() { return position; }
    public double health() { return health; }
    public double maxHealth() { return maxHealth; }
    public boolean isInWater() { return isInWater; }
    public boolean onGround() { return onGround; }
    public long gameTime() { return gameTime; }
    public int tickCount() { return tickCount; }
    public boolean isBaby() { return isBaby; }
    public boolean hasPassengers() { return hasPassengers; }
    public int passengerCount() { return passengerCount; }
    public List<PlayerInfo> nearbyPlayers() { return nearbyPlayers; }
    public boolean isSitting() { return isSitting; }
    
    public record PlayerInfo(UUID id, Vec3 pos, double distanceSq) {}
}

