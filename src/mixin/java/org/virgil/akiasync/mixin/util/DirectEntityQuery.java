package org.virgil.akiasync.mixin.util;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.phys.AABB;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class DirectEntityQuery {

    private static Field entityGetterField = null;
    private static Method getAllMethod = null;
    private static boolean initialized = false;
    private static boolean reflectionFailed = false;

    private static synchronized void init() {
        if (initialized || reflectionFailed) {
            return;
        }

        try {

            Class<?> serverLevelClass = ServerLevel.class;

            String[] possibleFieldNames = {
                "entityManager",
                "entityGetter",
                "entities"
            };

            for (String fieldName : possibleFieldNames) {
                try {
                    entityGetterField = serverLevelClass.getDeclaredField(fieldName);
                    entityGetterField.setAccessible(true);
                    break;
                } catch (NoSuchFieldException e) {

                }
            }

            if (entityGetterField == null) {

                for (Field field : serverLevelClass.getDeclaredFields()) {
                    if (LevelEntityGetter.class.isAssignableFrom(field.getType())) {
                        entityGetterField = field;
                        entityGetterField.setAccessible(true);
                        break;
                    }
                }
            }

            if (entityGetterField != null) {

                Class<?> entityGetterClass = entityGetterField.getType();
                getAllMethod = entityGetterClass.getMethod("getAll");
                getAllMethod.setAccessible(true);

                initialized = true;

                org.virgil.akiasync.mixin.bridge.Bridge bridge =
                    org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
                if (bridge != null && bridge.isTNTDebugEnabled()) {
                    bridge.debugLog("[AkiAsync-DirectEntityQuery] Reflection initialized successfully");
                }
            } else {
                reflectionFailed = true;

            }

        } catch (Exception e) {
            reflectionFailed = true;
            ExceptionHandler.handleExpected("DirectEntityQuery", "reflectionInit", e);
        }
    }

    public static List<Entity> getEntitiesInRange(ServerLevel level, AABB box) {
        if (!initialized && !reflectionFailed) {
            init();
        }

        List<Entity> result = new ArrayList<>();

        org.virgil.akiasync.mixin.bridge.Bridge bridge =
            org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();

        if (reflectionFailed) {

            if (bridge != null && bridge.isTNTDebugEnabled()) {
                bridge.debugLog("[AkiAsync-DirectEntityQuery] Reflection failed, using vanilla method");
            }
            return level.getEntities((Entity) null, box);
        }

        try {

            Object entityGetter = entityGetterField.get(level);

            if (entityGetter == null) {
                if (bridge != null) {
                    bridge.errorLog("[AkiAsync-DirectEntityQuery] Entity getter is null");
                }
                return level.getEntities((Entity) null, box);
            }

            @SuppressWarnings("unchecked")
            Iterable<Entity> allEntities = (Iterable<Entity>) getAllMethod.invoke(entityGetter);

            if (allEntities == null) {
                if (bridge != null) {
                    bridge.errorLog("[AkiAsync-DirectEntityQuery] getAllEntities returned null");
                }
                return result;
            }

            int totalCount = 0;
            int matchCount = 0;

            for (Entity entity : allEntities) {
                totalCount++;

                if (entity == null || entity.isRemoved()) {
                    continue;
                }

                if (entity.getBoundingBox().intersects(box)) {
                    result.add(entity);
                    matchCount++;
                }
            }

            if (bridge != null && bridge.isTNTDebugEnabled()) {
                bridge.debugLog("[AkiAsync-DirectEntityQuery] Scanned %d entities, found %d in range",
                    totalCount, matchCount);
            }

        } catch (Exception e) {
            if (bridge != null && bridge.isTNTDebugEnabled()) {
                bridge.debugLog("[AkiAsync-DirectEntityQuery] Query failed, using vanilla method: " + e.getMessage());
            }

            return level.getEntities((Entity) null, box);
        }

        return result;
    }

    public static boolean isAvailable() {
        if (!initialized && !reflectionFailed) {
            init();
        }
        return initialized && !reflectionFailed;
    }
}
