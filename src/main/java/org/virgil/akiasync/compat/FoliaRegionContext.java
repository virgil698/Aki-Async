package org.virgil.akiasync.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;

public class FoliaRegionContext {

    private static final boolean IS_FOLIA = FoliaSchedulerAdapter.isFolia();

    private static Method isOwnedByCurrentRegionEntityMethod;
    private static Method isOwnedByCurrentRegionLocationMethod;
    private static Method isOwnedByCurrentRegionChunkMethod;

    static {
        if (IS_FOLIA) {
            try {
                Class<?> serverClass = Bukkit.getServer().getClass();

                isOwnedByCurrentRegionEntityMethod = serverClass.getMethod(
                    "isOwnedByCurrentRegion",
                    org.bukkit.entity.Entity.class
                );

                isOwnedByCurrentRegionLocationMethod = serverClass.getMethod(
                    "isOwnedByCurrentRegion",
                    org.bukkit.Location.class
                );

                isOwnedByCurrentRegionChunkMethod = serverClass.getMethod(
                    "isOwnedByCurrentRegion",
                    org.bukkit.World.class,
                    int.class,
                    int.class
                );
            } catch (Exception e) {
            }
        }
    }

    public static boolean isInRegionThread() {
        if (!IS_FOLIA) {
            return false;
        }

        String threadName = Thread.currentThread().getName();
        return threadName.startsWith("Region Scheduler Thread");
    }

    public static boolean canAccessEntityDirectly(Entity entity) {
        if (!IS_FOLIA) {
            return true;
        }

        if (!isInRegionThread()) {
            return false;
        }

        if (isOwnedByCurrentRegionEntityMethod == null) {
            return true;
        }

        try {
            org.bukkit.entity.Entity bukkitEntity = entity.getBukkitEntity();
            Boolean owned = (Boolean) isOwnedByCurrentRegionEntityMethod.invoke(
                Bukkit.getServer(),
                bukkitEntity
            );
            return owned != null && owned;
        } catch (Exception e) {
            return true;
        }
    }

    public static boolean canAccessChunkDirectly(LevelChunk chunk) {
        if (!IS_FOLIA) {
            return true;
        }

        if (!isInRegionThread()) {
            return false;
        }

        if (isOwnedByCurrentRegionChunkMethod == null) {
            return true;
        }

        try {
            org.bukkit.World bukkitWorld = chunk.getLevel().getWorld();
            int chunkX = chunk.getPos().x;
            int chunkZ = chunk.getPos().z;

            Boolean owned = (Boolean) isOwnedByCurrentRegionChunkMethod.invoke(
                Bukkit.getServer(),
                bukkitWorld,
                chunkX,
                chunkZ
            );
            return owned != null && owned;
        } catch (Exception e) {
            return true;
        }
    }

    public static boolean canAccessBlockPosDirectly(Level level, BlockPos pos) {
        if (!IS_FOLIA) {
            return true;
        }

        if (!isInRegionThread()) {
            return false;
        }

        if (isOwnedByCurrentRegionLocationMethod == null) {
            return true;
        }

        try {
            org.bukkit.World bukkitWorld = level.getWorld();
            Location location = new Location(bukkitWorld, pos.getX(), pos.getY(), pos.getZ());

            Boolean owned = (Boolean) isOwnedByCurrentRegionLocationMethod.invoke(
                Bukkit.getServer(),
                location
            );
            return owned != null && owned;
        } catch (Exception e) {
            return true;
        }
    }

    public static void executeEntityOperation(Plugin plugin, Entity entity, Runnable operation) {
        if (!IS_FOLIA || canAccessEntityDirectly(entity)) {
            operation.run();
        } else {
            org.bukkit.entity.Entity bukkitEntity = entity.getBukkitEntity();
            FoliaSchedulerAdapter.runEntityTask(plugin, bukkitEntity, operation);
        }
    }

    public static void executeChunkOperation(Plugin plugin, LevelChunk chunk, Runnable operation) {
        if (!IS_FOLIA || canAccessChunkDirectly(chunk)) {
            operation.run();
        } else {
            org.bukkit.World bukkitWorld = chunk.getLevel().getWorld();
            Location location = new Location(
                bukkitWorld,
                chunk.getPos().x * 16.0,
                64,
                chunk.getPos().z * 16.0
            );
            FoliaSchedulerAdapter.runLocationTask(plugin, location, operation);
        }
    }

    public static void executeLocationOperation(Plugin plugin, Level level, BlockPos pos, Runnable operation) {
        if (!IS_FOLIA || canAccessBlockPosDirectly(level, pos)) {
            operation.run();
        } else {
            org.bukkit.World bukkitWorld = level.getWorld();
            Location location = new Location(bukkitWorld, pos.getX(), pos.getY(), pos.getZ());
            FoliaSchedulerAdapter.runLocationTask(plugin, location, operation);
        }
    }

    public static boolean isMultiRegionOperation(Level level, BlockPos pos1, BlockPos pos2) {
        if (!IS_FOLIA) {
            return false;
        }

        int chunkX1 = pos1.getX() >> 4;
        int chunkZ1 = pos1.getZ() >> 4;
        int chunkX2 = pos2.getX() >> 4;
        int chunkZ2 = pos2.getZ() >> 4;

        int distX = Math.abs(chunkX2 - chunkX1);
        int distZ = Math.abs(chunkZ2 - chunkZ1);

        return distX > 2 || distZ > 2;
    }

    public static OperationMode getSafeOperationMode() {
        if (!IS_FOLIA) {
            return OperationMode.ASYNC_PARALLEL;
        }

        if (isInRegionThread()) {
            return OperationMode.SYNC_SEQUENTIAL;
        }

        return OperationMode.REGION_SCHEDULED;
    }

    public enum OperationMode {
        ASYNC_PARALLEL,
        SYNC_SEQUENTIAL,
        REGION_SCHEDULED
    }
}
