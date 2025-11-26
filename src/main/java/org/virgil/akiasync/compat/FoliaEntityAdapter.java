package org.virgil.akiasync.compat;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

public class FoliaEntityAdapter {

    public static void safeEntityOperation(Plugin plugin, Entity entity, Consumer<Entity> operation) {
        if (FoliaSchedulerAdapter.isFolia()) {
            FoliaSchedulerAdapter.runEntityTask(plugin, entity, () -> {
                try {
                    if (entity.isValid() && !entity.isDead()) {
                        operation.accept(entity);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("[FoliaEntityAdapter] Entity operation failed: " + e.getMessage());
                }
            });
        } else {
            FoliaSchedulerAdapter.runTask(plugin, () -> {
                try {
                    if (entity.isValid() && !entity.isDead()) {
                        operation.accept(entity);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("[FoliaEntityAdapter] Entity operation failed: " + e.getMessage());
                }
            });
        }
    }

    public static <T> CompletableFuture<T> safeEntityQuery(Plugin plugin, Entity entity, Function<Entity, T> query) {
        CompletableFuture<T> future = new CompletableFuture<>();

        if (FoliaSchedulerAdapter.isFolia()) {
            FoliaSchedulerAdapter.runEntityTask(plugin, entity, () -> {
                try {
                    if (entity.isValid() && !entity.isDead()) {
                        T result = query.apply(entity);
                        future.complete(result);
                    } else {
                        future.complete(null);
                    }
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            });
        } else {
            FoliaSchedulerAdapter.runTask(plugin, () -> {
                try {
                    if (entity.isValid() && !entity.isDead()) {
                        T result = query.apply(entity);
                        future.complete(result);
                    } else {
                        future.complete(null);
                    }
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            });
        }

        return future;
    }

    public static void safeLocationOperation(Plugin plugin, Location location, Runnable operation) {
        if (FoliaSchedulerAdapter.isFolia()) {
            FoliaSchedulerAdapter.runLocationTask(plugin, location, () -> {
                try {
                    operation.run();
                } catch (Exception e) {
                    plugin.getLogger().warning("[FoliaEntityAdapter] Location operation failed: " + e.getMessage());
                }
            });
        } else {
            FoliaSchedulerAdapter.runTask(plugin, () -> {
                try {
                    operation.run();
                } catch (Exception e) {
                    plugin.getLogger().warning("[FoliaEntityAdapter] Location operation failed: " + e.getMessage());
                }
            });
        }
    }

    public static boolean isEntityInCurrentRegion(Entity entity) {
        if (!FoliaSchedulerAdapter.isFolia()) {
            return true;
        }

        try {
            Object currentRegion = Thread.currentThread().getClass()
                .getMethod("getCurrentRegion")
                .invoke(Thread.currentThread());

            if (currentRegion == null) {
                return false;
            }

            Object entityRegion = entity.getClass()
                .getMethod("getRegion")
                .invoke(entity);

            return currentRegion.equals(entityRegion);
        } catch (Exception e) {
            return false;
        }
    }
}
