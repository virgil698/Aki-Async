package org.virgil.akiasync.mixin.brain.panda;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public final class PandaSnapshot {
    
    private final Vec3 position;
    private final double health;
    private final double maxHealth;
    private final boolean isInWater;
    private final boolean onGround;
    private final long gameTime;
    private final int tickCount;
    private final boolean isBaby;
    private final String personality; 
    private final boolean isEating;
    private final boolean hasFood;
    private final List<EntityInfo> nearbyEntities;
    private final List<PlayerInfo> nearbyPlayers;
    private final List<ItemInfo> nearbyItems;
    
    private PandaSnapshot(Vec3 position, double health, double maxHealth,
                         boolean isInWater, boolean onGround, long gameTime, int tickCount,
                         boolean isBaby, String personality, boolean isEating, boolean hasFood,
                         List<EntityInfo> nearbyEntities, List<PlayerInfo> nearbyPlayers,
                         List<ItemInfo> nearbyItems) {
        this.position = position;
        this.health = health;
        this.maxHealth = maxHealth;
        this.isInWater = isInWater;
        this.onGround = onGround;
        this.gameTime = gameTime;
        this.tickCount = tickCount;
        this.isBaby = isBaby;
        this.personality = personality;
        this.isEating = isEating;
        this.hasFood = hasFood;
        this.nearbyEntities = nearbyEntities;
        this.nearbyPlayers = nearbyPlayers;
        this.nearbyItems = nearbyItems;
    }
    
    public static PandaSnapshot capture(net.minecraft.world.entity.animal.Animal panda, 
                                       ServerLevel level, int tickCount) {
        Vec3 pos = panda.position();
        double health = panda.getHealth();
        double maxHealth = panda.getMaxHealth();
        boolean isInWater = panda.isInWater();
        boolean onGround = panda.onGround();
        long gameTime = level.getGameTime();
        boolean isBaby = panda.isBaby();
        
        String personality = "NORMAL";
        boolean isEating = false;
        try {
            if (panda instanceof net.minecraft.world.entity.animal.Panda pandaEntity) {
                
                personality = pandaEntity.getMainGene().toString();
                isEating = pandaEntity.isEating();
            }
        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "PandaSnapshot", "capturePersonality", e);
        }
        
        boolean hasFood = false;
        try {
            
            ItemStack mainHand = panda.getMainHandItem();
            ItemStack offHand = panda.getOffhandItem();
            if ((mainHand != null && mainHand.is(net.minecraft.world.item.Items.BAMBOO)) ||
                (offHand != null && offHand.is(net.minecraft.world.item.Items.BAMBOO))) {
                hasFood = true;
            }
        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "PandaSnapshot", "captureFood", e);
        }
        
        List<EntityInfo> entities = new ArrayList<>();
        List<LivingEntity> nearbyEntities = level.getEntitiesOfClass(
            LivingEntity.class,
            panda.getBoundingBox().inflate(16.0),
            e -> e != panda && e.isAlive() && !e.isSpectator()
        );
        
        for (LivingEntity entity : nearbyEntities) {
            entities.add(new EntityInfo(
                entity.getUUID(),
                entity.position(),
                panda.distanceToSqr(entity),
                entity instanceof net.minecraft.world.entity.animal.Panda
            ));
        }
        
        List<PlayerInfo> players = new ArrayList<>();
        List<net.minecraft.world.entity.player.Player> nearbyPlayers = 
            level.getEntitiesOfClass(
                net.minecraft.world.entity.player.Player.class,
                panda.getBoundingBox().inflate(16.0),
                p -> !p.isSpectator() && p.isAlive()
            );
        
        for (net.minecraft.world.entity.player.Player player : nearbyPlayers) {
            players.add(new PlayerInfo(
                player.getUUID(),
                player.position(),
                panda.distanceToSqr(player)
            ));
        }
        
        List<ItemInfo> items = new ArrayList<>();
        List<net.minecraft.world.entity.item.ItemEntity> nearbyItems = 
            level.getEntitiesOfClass(
                net.minecraft.world.entity.item.ItemEntity.class,
                panda.getBoundingBox().inflate(8.0),
                item -> item.getItem().is(net.minecraft.world.item.Items.BAMBOO)
            );
        
        for (net.minecraft.world.entity.item.ItemEntity item : nearbyItems) {
            items.add(new ItemInfo(
                item.getUUID(),
                item.position(),
                panda.distanceToSqr(item),
                "BAMBOO"
            ));
        }
        
        return new PandaSnapshot(pos, health, maxHealth, isInWater, onGround,
                                gameTime, tickCount, isBaby, personality, isEating, hasFood,
                                entities, players, items);
    }
    
    public Vec3 position() { return position; }
    public double health() { return health; }
    public double maxHealth() { return maxHealth; }
    public boolean isInWater() { return isInWater; }
    public boolean onGround() { return onGround; }
    public long gameTime() { return gameTime; }
    public int tickCount() { return tickCount; }
    public boolean isBaby() { return isBaby; }
    public String personality() { return personality; }
    public boolean isEating() { return isEating; }
    public boolean hasFood() { return hasFood; }
    public List<EntityInfo> nearbyEntities() { return nearbyEntities; }
    public List<PlayerInfo> nearbyPlayers() { return nearbyPlayers; }
    public List<ItemInfo> nearbyItems() { return nearbyItems; }
    
    public record EntityInfo(UUID id, Vec3 pos, double distanceSq, boolean isPanda) {}
    public record PlayerInfo(UUID id, Vec3 pos, double distanceSq) {}
    public record ItemInfo(UUID id, Vec3 pos, double distanceSq, String itemType) {}
}
