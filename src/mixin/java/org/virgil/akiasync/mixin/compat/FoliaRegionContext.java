package org.virgil.akiasync.mixin.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.lang.reflect.Method;

public final class FoliaRegionContext {

    private static volatile Boolean isFolia = null;
    private static volatile Class<?> regionizedServerClass = null;
    private static volatile Method isOwnedByCurrentRegionEntityMethod = null;
    private static volatile Method isOwnedByCurrentRegionLocationMethod = null;

    private FoliaRegionContext() {
        throw new UnsupportedOperationException("Utility class");
    }

    private static boolean isFoliaEnvironment() {
        if (isFolia == null) {
            synchronized (FoliaRegionContext.class) {
                if (isFolia == null) {
                    try {
                        Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
                        isFolia = true;
                    } catch (ClassNotFoundException e) {
                        isFolia = false;
                    }
                }
            }
        }
        return isFolia;
    }

    private static void initializeReflection() {
        if (regionizedServerClass != null) {
            return;
        }

        synchronized (FoliaRegionContext.class) {
            if (regionizedServerClass != null) {
                return;
            }

            try {
                regionizedServerClass = Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
                
                try {
                    isOwnedByCurrentRegionEntityMethod = regionizedServerClass.getMethod(
                        "isOwnedByCurrentRegion", Entity.class);
                } catch (NoSuchMethodException e) {
                    
                }

                try {
                    isOwnedByCurrentRegionLocationMethod = regionizedServerClass.getMethod(
                        "isOwnedByCurrentRegion", Level.class, BlockPos.class);
                } catch (NoSuchMethodException e) {
                    
                }

            } catch (Exception e) {
                org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                    "FoliaRegionContext", "initializeReflection", e);
            }
        }
    }

    public static boolean canAccessEntityDirectly(Entity entity) {
        if (entity == null) {
            return true;
        }

        if (!isFoliaEnvironment()) {
            return true;
        }

        initializeReflection();

        if (isOwnedByCurrentRegionEntityMethod == null) {
            return false;
        }

        try {
            Object result = isOwnedByCurrentRegionEntityMethod.invoke(null, entity);
            return result instanceof Boolean && (Boolean) result;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean canAccessBlockPosDirectly(Level level, BlockPos pos) {
        if (level == null || pos == null) {
            return true;
        }

        if (!isFoliaEnvironment()) {
            return true;
        }

        initializeReflection();

        if (isOwnedByCurrentRegionLocationMethod == null) {
            return false;
        }

        try {
            Object result = isOwnedByCurrentRegionLocationMethod.invoke(null, level, pos);
            return result instanceof Boolean && (Boolean) result;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isInCorrectThread(Entity entity) {
        return canAccessEntityDirectly(entity);
    }

    public static boolean isInCorrectThread(Level level, BlockPos pos) {
        return canAccessBlockPosDirectly(level, pos);
    }
}
