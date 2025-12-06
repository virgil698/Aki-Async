package org.virgil.akiasync.mixin.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.function.Predicate;

public class EntitySelectorCache {
    
    public static final Predicate<Entity> PUSHABLE = entity -> 
        entity != null && 
        !entity.isRemoved() && 
        entity.isPushable();
    
    public static final Predicate<Entity> PUSHABLE_LIVING = entity -> 
        entity instanceof LivingEntity living &&
        !living.isRemoved() && 
        living.isPushable() &&
        living.isAlive();
    
    public static final Predicate<Entity> COLLISIONABLE = entity -> 
        entity != null &&
        !entity.isRemoved() && 
        entity.isPushable();
    
    public static final Predicate<Entity> COLLISIONABLE_LIVING = entity -> 
        entity instanceof LivingEntity living &&
        !living.isRemoved() && 
        living.isPushable() &&
        living.isAlive();
    
    public static final Predicate<Entity> PUSHABLE_AND_COLLISIONABLE = entity -> 
        entity != null &&
        !entity.isRemoved() && 
        entity.isPushable();
    
    public static final Predicate<Entity> ALIVE_LIVING = entity -> 
        entity instanceof LivingEntity living &&
        !living.isRemoved() && 
        living.isAlive();
    
    public static Predicate<Entity> combine(Predicate<Entity> base, Predicate<Entity> additional) {
        return entity -> base.test(entity) && additional.test(entity);
    }
    
    public static Predicate<Entity> getPushableExcept(Entity except) {
        if (except == null) {
            return PUSHABLE;
        }
        return entity -> entity != except && PUSHABLE.test(entity);
    }
    
    public static Predicate<Entity> getCollisionableExcept(Entity except) {
        if (except == null) {
            return COLLISIONABLE;
        }
        return entity -> entity != except && COLLISIONABLE.test(entity);
    }
}
