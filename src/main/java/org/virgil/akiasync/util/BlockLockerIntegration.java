package org.virgil.akiasync.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

public class BlockLockerIntegration {

    private static volatile Boolean blockLockerEnabled = null;
    private static volatile Object blockLockerAPI = null;
    private static volatile Method isProtectedMethod = null;

    private static final ConcurrentHashMap<String, CacheEntry> CACHE = new ConcurrentHashMap<>();
    private static final long CACHE_DURATION_MS = 5000; 
    private static final int MAX_CACHE_SIZE = 500;

    private static class CacheEntry {
        final boolean protected_;
        final long timestamp;

        CacheEntry(boolean protected_) {
            this.protected_ = protected_;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isValid() {
            return System.currentTimeMillis() - timestamp < CACHE_DURATION_MS;
        }
    }

    public static boolean isProtected(ServerLevel level, BlockPos pos, BlockState state) {
        try {
            
            if (!isBlockLockerEnabled()) {
                return false;
            }

            String cacheKey = getCacheKey(level, pos);
            CacheEntry cached = CACHE.get(cacheKey);
            if (cached != null && cached.isValid()) {
                return cached.protected_;
            }

            if (CACHE.size() > MAX_CACHE_SIZE) {
                CACHE.entrySet().removeIf(entry -> !entry.getValue().isValid());
            }

            if (!isContainer(state)) {
                CACHE.put(cacheKey, new CacheEntry(false));
                return false;
            }

            World bukkitWorld = level.getWorld();
            Location location = new Location(bukkitWorld, pos.getX(), pos.getY(), pos.getZ());
            Block block = bukkitWorld.getBlockAt(location);

            boolean protected_ = checkBlockLockerProtection(block);

            CACHE.put(cacheKey, new CacheEntry(protected_));

            return protected_;

        } catch (Exception e) {
            DebugLogger.error("[BlockLocker] Error checking protection: " + e.getMessage());
            return false; 
        }
    }

    private static boolean checkBlockLockerProtection(Block block) {
        try {
            
            if (blockLockerAPI == null) {
                Plugin blockLocker = Bukkit.getPluginManager().getPlugin("BlockLocker");
                if (blockLocker == null) {
                    blockLockerEnabled = false;
                    return false;
                }

                Class<?> apiClass = Class.forName("nl.rutgerkok.blocklocker.BlockLockerAPIv2");
                Method getPluginMethod = apiClass.getMethod("getPlugin");
                blockLockerAPI = getPluginMethod.invoke(null);

                isProtectedMethod = apiClass.getMethod("isProtected", Block.class);

                DebugLogger.debug("[BlockLocker] API loaded successfully");
            }

            Boolean result = (Boolean) isProtectedMethod.invoke(blockLockerAPI, block);
            return result != null && result;

        } catch (ClassNotFoundException e) {
            blockLockerEnabled = false;
            DebugLogger.error("[BlockLocker] API not found - plugin version may be incompatible");
            return false;
        } catch (Exception e) {
            DebugLogger.error("[BlockLocker] Error calling API: " + e.getMessage());
            return false;
        }
    }

    private static boolean isContainer(BlockState state) {
        net.minecraft.world.level.block.Block block = state.getBlock();

        return block instanceof net.minecraft.world.level.block.ChestBlock ||
               block instanceof net.minecraft.world.level.block.TrappedChestBlock ||
               block instanceof net.minecraft.world.level.block.FurnaceBlock ||
               block instanceof net.minecraft.world.level.block.BlastFurnaceBlock ||
               block instanceof net.minecraft.world.level.block.SmokerBlock ||
               block instanceof net.minecraft.world.level.block.BarrelBlock ||
               block instanceof net.minecraft.world.level.block.HopperBlock ||
               block instanceof net.minecraft.world.level.block.DropperBlock ||
               block instanceof net.minecraft.world.level.block.DispenserBlock ||
               block instanceof net.minecraft.world.level.block.ShulkerBoxBlock ||
               block instanceof net.minecraft.world.level.block.BrewingStandBlock ||
               block instanceof net.minecraft.world.level.block.EnderChestBlock ||
               state.hasBlockEntity(); 
    }

    private static boolean isBlockLockerEnabled() {
        Boolean enabled = blockLockerEnabled;
        if (enabled == null) {
            synchronized (BlockLockerIntegration.class) {
                enabled = blockLockerEnabled;
                if (enabled == null) {
                    blockLockerEnabled = enabled = Bukkit.getPluginManager().isPluginEnabled("BlockLocker");
                    if (enabled) {
                        DebugLogger.debug("[BlockLocker] Plugin detected");
                    }
                }
            }
        }
        return enabled;
    }

    private static String getCacheKey(ServerLevel level, BlockPos pos) {
        return level.dimension().location().toString() + ":" +
               pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }

    public static void clearCache() {
        CACHE.clear();
    }

    public static void reset() {
        blockLockerEnabled = null;
        blockLockerAPI = null;
        isProtectedMethod = null;
        clearCache();
        DebugLogger.debug("[BlockLocker] Integration reset");
    }
}
