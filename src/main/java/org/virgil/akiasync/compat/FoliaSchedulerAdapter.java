package org.virgil.akiasync.compat;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

public class FoliaSchedulerAdapter {

    private static final boolean IS_FOLIA;
    private static Method globalRegionScheduleMethod;
    private static Method entityScheduleMethod;
    private static Method locationScheduleMethod;

    static {
        boolean foliaDetected = false;
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            foliaDetected = true;

            Class<?> foliaGlobalRegionScheduler = Class.forName("io.papermc.paper.threadedregions.scheduler.FoliaGlobalRegionScheduler");
            globalRegionScheduleMethod = foliaGlobalRegionScheduler.getMethod("run", Plugin.class, Runnable.class);

            Class<?> foliaEntityScheduler = Class.forName("io.papermc.paper.threadedregions.scheduler.FoliaEntityScheduler");
            entityScheduleMethod = foliaEntityScheduler.getMethod("run", Plugin.class, Runnable.class, Runnable.class);

            Class<?> foliaRegionScheduler = Class.forName("io.papermc.paper.threadedregions.scheduler.FoliaRegionScheduler");
            locationScheduleMethod = foliaRegionScheduler.getMethod("run", Plugin.class, Location.class, Runnable.class);

        } catch (Exception e) {
            
        }
        IS_FOLIA = foliaDetected;
    }

    public static boolean isFolia() {
        return IS_FOLIA;
    }

    public static BukkitTask runTask(Plugin plugin, Runnable task) {
        if (IS_FOLIA) {
            try {
                Object globalScheduler = Bukkit.getServer().getClass()
                    .getMethod("getGlobalRegionScheduler")
                    .invoke(Bukkit.getServer());

                return (BukkitTask) globalRegionScheduleMethod.invoke(globalScheduler, plugin, task);
            } catch (Exception e) {
                plugin.getLogger().warning("[FoliaAdapter] Failed to schedule global task: " + e.getMessage());
                task.run();
                return null;
            }
        } else {
            return Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    public static BukkitTask runEntityTask(Plugin plugin, Entity entity, Runnable task) {
        if (IS_FOLIA) {
            try {
                Object entityScheduler = Bukkit.getServer().getClass()
                    .getMethod("getEntityScheduler")
                    .invoke(Bukkit.getServer());

                return (BukkitTask) entityScheduleMethod.invoke(entityScheduler, plugin, task, null);
            } catch (Exception e) {
                plugin.getLogger().warning("[FoliaAdapter] Failed to schedule entity task: " + e.getMessage());
                return runLocationTask(plugin, entity.getLocation(), task);
            }
        } else {
            return Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    public static BukkitTask runLocationTask(Plugin plugin, Location location, Runnable task) {
        if (IS_FOLIA) {
            try {
                Object regionScheduler = Bukkit.getServer().getClass()
                    .getMethod("getRegionScheduler")
                    .invoke(Bukkit.getServer());

                return (BukkitTask) locationScheduleMethod.invoke(regionScheduler, plugin, location, task);
            } catch (Exception e) {
                plugin.getLogger().warning("[FoliaAdapter] Failed to schedule location task: " + e.getMessage());
                task.run();
                return null;
            }
        } else {
            return Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    public static BukkitTask runTaskLater(Plugin plugin, Runnable task, long delayTicks) {
        if (IS_FOLIA) {
            try {
                Object globalScheduler = Bukkit.getServer().getClass()
                    .getMethod("getGlobalRegionScheduler")
                    .invoke(Bukkit.getServer());

                Method runDelayedMethod = globalScheduler.getClass()
                    .getMethod("runDelayed", Plugin.class, Runnable.class, long.class);

                return (BukkitTask) runDelayedMethod.invoke(globalScheduler, plugin, task, delayTicks);
            } catch (Exception e) {
                plugin.getLogger().warning("[FoliaAdapter] Failed to schedule delayed task: " + e.getMessage());
                java.util.concurrent.Executors.newSingleThreadScheduledExecutor()
                    .schedule(task, delayTicks * 50, TimeUnit.MILLISECONDS);
                return null;
            }
        } else {
            return Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
        }
    }
}
