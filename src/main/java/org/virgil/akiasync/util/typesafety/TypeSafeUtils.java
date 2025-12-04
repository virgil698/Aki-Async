package org.virgil.akiasync.util.typesafety;

import org.bukkit.entity.Entity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;

public final class TypeSafeUtils {

    private TypeSafeUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    @Nonnull
    public static <T> Optional<T> safeCast(@Nullable Object obj, @Nonnull Class<T> targetClass) {
        if (obj == null) {
            return Optional.empty();
        }
        
        try {
            if (targetClass.isInstance(obj)) {
                return Optional.of(targetClass.cast(obj));
            }
        } catch (ClassCastException e) {
            
        }
        
        return Optional.empty();
    }

    @Nonnull
    public static <T> Optional<T> safeCastGeneric(@Nullable Object obj, @Nonnull Class<T> targetClass) {
        return safeCast(obj, targetClass);
    }

    @Nonnull
    public static Optional<net.minecraft.world.entity.Entity> toNMSEntity(@Nullable Entity bukkitEntity) {
        if (bukkitEntity == null) {
            return Optional.empty();
        }
        
        try {

            Object handle = bukkitEntity.getClass().getMethod("getHandle").invoke(bukkitEntity);
            return safeCast(handle, net.minecraft.world.entity.Entity.class);
        } catch (ReflectiveOperationException e) {

            return Optional.empty();
        }
    }

    @Nonnull
    public static Optional<Entity> toBukkitEntity(@Nullable net.minecraft.world.entity.Entity nmsEntity) {
        if (nmsEntity == null) {
            return Optional.empty();
        }
        
        try {
            Entity bukkitEntity = nmsEntity.getBukkitEntity();
            return Optional.ofNullable(bukkitEntity);
        } catch (Exception e) {
            
            return Optional.empty();
        }
    }

    @Nonnull
    @SuppressWarnings("unchecked")
    public static <K, V> Optional<Map<K, V>> safeCastMap(
            @Nullable Object obj, 
            @Nonnull Class<K> keyClass, 
            @Nonnull Class<V> valueClass) {
        
        if (obj == null) {
            return Optional.empty();
        }
        
        if (!(obj instanceof Map)) {
            return Optional.empty();
        }
        
        try {
            Map<?, ?> rawMap = (Map<?, ?>) obj;
            
            for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                Object key = entry.getKey();
                Object value = entry.getValue();
                
                if (key != null && !keyClass.isInstance(key)) {
                    return Optional.empty();
                }
                if (value != null && !valueClass.isInstance(value)) {
                    return Optional.empty();
                }
            }
            
            return Optional.of((Map<K, V>) rawMap);
        } catch (ClassCastException e) {
            
            return Optional.empty();
        }
    }
}
