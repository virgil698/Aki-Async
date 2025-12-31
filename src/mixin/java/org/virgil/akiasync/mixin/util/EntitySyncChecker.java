package org.virgil.akiasync.mixin.util;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.ExperienceOrb;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


public final class EntitySyncChecker {

    private EntitySyncChecker() {}

    
    public static final Set<Class<?>> BLOCKED_ENTITY_CLASSES = Set.of(
            FallingBlockEntity.class,
            Shulker.class,
            Boat.class
    );

    
    private static volatile Set<String> synchronizedEntityTypes = Set.of(
            "minecraft:tnt",
            "minecraft:item",
            "minecraft:experience_orb"
    );

    
    private static final Set<UUID> blacklistedEntities = ConcurrentHashMap.newKeySet();

    
    private static final Map<UUID, Integer> portalTickSyncMap = new ConcurrentHashMap<>();

    
    private static final int PORTAL_SYNC_TICKS = 39;

    
    public static boolean shouldTickSynchronously(Entity entity) {
        if (entity == null) {
            return true;
        }

        
        if (entity.level().isClientSide()) {
            return true;
        }

        UUID entityId = entity.getUUID();

        
        if (entity instanceof ServerPlayer ||
            entity instanceof Projectile ||
            entity instanceof AbstractMinecart ||
            BLOCKED_ENTITY_CLASSES.contains(entity.getClass()) ||
            blacklistedEntities.contains(entityId)) {
            return true;
        }

        
        String entityTypeKey = EntityType.getKey(entity.getType()).toString();
        if (synchronizedEntityTypes.contains(entityTypeKey)) {
            return true;
        }

        
        if (portalTickSyncMap.containsKey(entityId)) {
            int ticksLeft = portalTickSyncMap.get(entityId);
            if (ticksLeft > 0) {
                portalTickSyncMap.put(entityId, ticksLeft - 1);
                return true;
            } else {
                portalTickSyncMap.remove(entityId);
            }
        }

        
        if (isPortalTickRequired(entity)) {
            portalTickSyncMap.put(entityId, PORTAL_SYNC_TICKS);
            return true;
        }

        return false;
    }

    
    private static boolean isPortalTickRequired(Entity entity) {
        try {
            return entity.portalProcess != null && entity.portalProcess.isInsidePortalThisTick();
        } catch (Throwable t) {
            
            return false;
        }
    }

    
    public static void blacklistEntity(UUID entityId) {
        blacklistedEntities.add(entityId);
    }

    
    public static void unblacklistEntity(UUID entityId) {
        blacklistedEntities.remove(entityId);
    }

    
    public static void clearPortalSync(UUID entityId) {
        portalTickSyncMap.remove(entityId);
    }

    
    public static void setSynchronizedEntityTypes(Set<String> types) {
        
        Set<String> copy = ConcurrentHashMap.newKeySet();
        if (types != null) {
            copy.addAll(types);
        }
        synchronizedEntityTypes = copy;
    }

    
    public static Set<String> getSynchronizedEntityTypes() {
        return java.util.Collections.unmodifiableSet(synchronizedEntityTypes);
    }

    
    public static boolean isEntityTypeSynchronized(String entityType) {
        return synchronizedEntityTypes.contains(entityType);
    }

    
    public static void addSynchronizedEntityType(String entityType) {
        Set<String> newSet = ConcurrentHashMap.newKeySet();
        newSet.addAll(synchronizedEntityTypes);
        newSet.add(entityType);
        synchronizedEntityTypes = newSet;
    }

    
    public static void removeSynchronizedEntityType(String entityType) {
        Set<String> newSet = ConcurrentHashMap.newKeySet();
        newSet.addAll(synchronizedEntityTypes);
        newSet.remove(entityType);
        synchronizedEntityTypes = newSet;
    }

    
    public static int getBlacklistSize() {
        return blacklistedEntities.size();
    }

    
    public static int getPortalSyncCount() {
        return portalTickSyncMap.size();
    }
}
