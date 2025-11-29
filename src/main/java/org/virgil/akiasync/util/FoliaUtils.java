package org.virgil.akiasync.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.virgil.akiasync.constants.AkiAsyncConstants;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

public final class FoliaUtils {

    private static volatile Boolean isFolia = null;
    private static volatile Class<?> regionizedServerClass = null;
    private static volatile Object regionizedServerInstance = null;
    private static volatile Object taskQueue = null;
    private static volatile Method queueMethod = null;
    private static volatile Method isOwnedMethod = null;

    private static final ConcurrentHashMap<String, Method> methodCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Class<?>> classCache = new ConcurrentHashMap<>();

    private FoliaUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static boolean isFoliaEnvironment() {
        if (isFolia == null) {
            synchronized (FoliaUtils.class) {
                if (isFolia == null) {
                    isFolia = ExceptionHandler.safeSupply(
                        () -> {
                            try {
                                Class.forName(AkiAsyncConstants.Folia.REGIONIZED_SERVER_CLASS);
                                return true;
                            } catch (ClassNotFoundException e) {
                                return false;
                            }
                        },
                        "Folia environment detection",
                        false
                    );
                }
            }
        }
        return isFolia;
    }

    public static boolean isOwnedByCurrentRegion(ServerLevel level, Vec3 position) {
        if (!isFoliaEnvironment()) {
            return true;
        }

        return ExceptionHandler.safeReflectionSupply(() -> {
            if (isOwnedMethod == null) {
                Class<?> bukkitClass = getOrCacheClass(AkiAsyncConstants.Folia.BUKKIT_CLASS);
                isOwnedMethod = getOrCacheMethod(bukkitClass, AkiAsyncConstants.Folia.IS_OWNED_METHOD,
                    org.bukkit.World.class, int.class, int.class);
            }

            BlockPos blockPos = BlockPos.containing(position);
            org.bukkit.World bukkitWorld = level.getWorld();
            return (Boolean) isOwnedMethod.invoke(null, bukkitWorld,
                blockPos.getX() >> 4, blockPos.getZ() >> 4);
        }, "Region ownership check", true);
    }

    public static void scheduleRegionTask(ServerLevel level, Vec3 position, Runnable task) {
        if (!isFoliaEnvironment()) {
            task.run();
            return;
        }

        ExceptionHandler.safeReflection(() -> {
            initializeFoliaScheduler();

            BlockPos blockPos = BlockPos.containing(position);
            int chunkX = blockPos.getX() >> 4;
            int chunkZ = blockPos.getZ() >> 4;

            queueMethod.invoke(taskQueue, new Object[]{level, chunkX, chunkZ, task});
        }, "Folia region task scheduling");

        if (queueMethod == null) {
            task.run();
        }
    }

    private static void initializeFoliaScheduler() throws ReflectiveOperationException {
        if (regionizedServerClass == null) {
            regionizedServerClass = getOrCacheClass(AkiAsyncConstants.Folia.REGIONIZED_SERVER_CLASS);
        }

        if (regionizedServerInstance == null) {
            Method getInstanceMethod = getOrCacheMethod(regionizedServerClass, "getInstance");
            regionizedServerInstance = getInstanceMethod.invoke(null);
        }

        if (taskQueue == null) {
            taskQueue = regionizedServerClass.getField("taskQueue").get(regionizedServerInstance);
        }

        if (queueMethod == null) {
            queueMethod = getOrCacheMethod(taskQueue.getClass(), AkiAsyncConstants.Folia.QUEUE_METHOD,
                ServerLevel.class, int.class, int.class, Runnable.class);
        }
    }

    private static Class<?> getOrCacheClass(String className) throws ReflectiveOperationException {
        return classCache.computeIfAbsent(className, name -> {
            try {
                return Class.forName(name);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static Method getOrCacheMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes) throws ReflectiveOperationException {
        String key = clazz.getName() + "#" + methodName + "#" + java.util.Arrays.toString(parameterTypes);
        return methodCache.computeIfAbsent(key, k -> {
            try {
                return clazz.getMethod(methodName, parameterTypes);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static void clearCache() {
        isFolia = null;
        regionizedServerClass = null;
        regionizedServerInstance = null;
        taskQueue = null;
        queueMethod = null;
        isOwnedMethod = null;
        methodCache.clear();
        classCache.clear();
    }

    public static String getStatus() {
        if (!isFoliaEnvironment()) {
            return "Non-Folia environment";
        }

        return String.format("Folia environment - Server: %s, TaskQueue: %s, QueueMethod: %s",
            regionizedServerInstance != null ? "initialized" : "null",
            taskQueue != null ? "initialized" : "null",
            queueMethod != null ? "initialized" : "null");
    }
}
