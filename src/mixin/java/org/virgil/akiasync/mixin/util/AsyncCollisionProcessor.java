package org.virgil.akiasync.mixin.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Predicate;

public class AsyncCollisionProcessor {

    private static volatile ExecutorService collisionExecutor = null;
    private static final int DEFAULT_TIMEOUT_MS = 5;

    public static void setExecutor(ExecutorService executor) {
        collisionExecutor = executor;
    }

    public static int countNearbyEntitiesAsync(Entity entity, double radius) {
        if (collisionExecutor == null || collisionExecutor.isShutdown()) {

            return countNearbyEntitiesSync(entity, radius);
        }

        try {
            Future<Integer> future = collisionExecutor.submit(() ->
                countNearbyEntitiesSync(entity, radius)
            );

            return future.get(DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS);

        } catch (TimeoutException e) {
            ExceptionHandler.handleExpected("AsyncCollisionProcessor", "countNearbyTimeout",
                new Exception("Timeout after " + DEFAULT_TIMEOUT_MS + "ms"));
            return 0;
        } catch (Exception e) {
            ExceptionHandler.handleUnexpected("AsyncCollisionProcessor", "countNearbyAsync", e);
            return 0;
        }
    }

    private static int countNearbyEntitiesSync(Entity entity, double radius) {
        try {
            Level level = entity.level();
            if (level == null) return 0;

            AABB box = entity.getBoundingBox().inflate(radius);
            List<Entity> nearby = level.getEntities(
                entity,
                box,
                EntitySelectorCache.PUSHABLE
            );

            return nearby.size();
        } catch (Exception e) {
            ExceptionHandler.handleUnexpected("AsyncCollisionProcessor", "countNearbySync", e);
            return 0;
        }
    }

    public static List<Entity> getEntitiesAsync(
        Level level,
        Entity except,
        AABB box,
        Predicate<? super Entity> predicate,
        int timeoutMs
    ) {
        if (collisionExecutor == null || collisionExecutor.isShutdown()) {

            return getEntitiesSync(level, except, box, predicate);
        }

        try {
            Future<List<Entity>> future = collisionExecutor.submit(() ->
                getEntitiesSync(level, except, box, predicate)
            );

            return future.get(timeoutMs, TimeUnit.MILLISECONDS);

        } catch (TimeoutException e) {
            ExceptionHandler.handleExpected("AsyncCollisionProcessor", "getEntitiesTimeout",
                new Exception("Timeout after " + timeoutMs + "ms"));
            return new ArrayList<>();
        } catch (Exception e) {
            ExceptionHandler.handleUnexpected("AsyncCollisionProcessor", "getEntitiesAsync", e);
            return new ArrayList<>();
        }
    }

    private static List<Entity> getEntitiesSync(
        Level level,
        Entity except,
        AABB box,
        Predicate<? super Entity> predicate
    ) {
        try {
            if (level == null) return new ArrayList<>();
            return level.getEntities(except, box, predicate);
        } catch (Exception e) {
            ExceptionHandler.handleUnexpected("AsyncCollisionProcessor", "getEntitiesSync", e);
            return new ArrayList<>();
        }
    }

    public static <T> List<T> processBatchAsync(
        List<Callable<T>> tasks,
        int timeoutMs
    ) {
        if (collisionExecutor == null || collisionExecutor.isShutdown() || tasks.isEmpty()) {
            return new ArrayList<>();
        }

        try {
            List<Future<T>> futures = collisionExecutor.invokeAll(
                tasks,
                timeoutMs,
                TimeUnit.MILLISECONDS
            );

            List<T> results = new ArrayList<>();
            for (Future<T> future : futures) {
                try {
                    if (future.isDone() && !future.isCancelled()) {
                        T result = future.get(1, TimeUnit.MILLISECONDS);
                        if (result != null) {
                            results.add(result);
                        }
                    }
                } catch (Exception e) {
                    ExceptionHandler.handleExpected("AsyncCollisionProcessor", "batchTaskResult", e);
                }
            }

            return results;

        } catch (Exception e) {
            ExceptionHandler.handleUnexpected("AsyncCollisionProcessor", "processBatchAsync", e);
            return new ArrayList<>();
        }
    }

    public static boolean isAvailable() {
        return collisionExecutor != null && !collisionExecutor.isShutdown();
    }

    public static String getStatistics() {
        if (collisionExecutor == null) {
            return "CollisionExecutor: not initialized";
        }

        if (collisionExecutor instanceof ThreadPoolExecutor) {
            ThreadPoolExecutor tpe = (ThreadPoolExecutor) collisionExecutor;
            return String.format(
                "CollisionExecutor: Active=%d, Queued=%d, Completed=%d",
                tpe.getActiveCount(),
                tpe.getQueue().size(),
                tpe.getCompletedTaskCount()
            );
        }

        return "CollisionExecutor: initialized";
    }
}
