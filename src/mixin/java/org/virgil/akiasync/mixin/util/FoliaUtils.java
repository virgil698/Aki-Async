package org.virgil.akiasync.mixin.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;


public final class FoliaUtils {

    
    private static final String REGIONIZED_SERVER_CLASS = "io.papermc.paper.threadedregions.RegionizedServer";
    private static final String BUKKIT_CLASS = "org.bukkit.Bukkit";
    private static final String IS_OWNED_METHOD = "isOwnedByCurrentRegion";
    private static final String QUEUE_METHOD = "queueTickTaskQueue";

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
                                Class.forName(REGIONIZED_SERVER_CLASS);
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
                Class<?> bukkitClass = getOrCacheClass(BUKKIT_CLASS);
                
                Class<?> worldClass = getOrCacheClass("org.bukkit.World");
                isOwnedMethod = getOrCacheMethod(bukkitClass, IS_OWNED_METHOD,
                    worldClass, int.class, int.class);
            }

            BlockPos blockPos = BlockPos.containing(position);
            
            Method getWorldMethod = level.getClass().getMethod("getWorld");
            Object bukkitWorld = getWorldMethod.invoke(level);
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
            regionizedServerClass = getOrCacheClass(REGIONIZED_SERVER_CLASS);
        }

        if (regionizedServerInstance == null) {
            Method getInstanceMethod = getOrCacheMethod(regionizedServerClass, "getInstance");
            regionizedServerInstance = getInstanceMethod.invoke(null);
        }

        if (taskQueue == null) {
            taskQueue = regionizedServerClass.getField("taskQueue").get(regionizedServerInstance);
        }

        if (queueMethod == null) {
            queueMethod = getOrCacheMethod(taskQueue.getClass(), QUEUE_METHOD,
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
