package org.virgil.akiasync.util;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

public class BlockLockerIntegration {

    private static volatile Boolean blockLockerEnabled = null;
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

    public static boolean isProtected(World world, int x, int y, int z, String blockType) {
        try {
            if (!isBlockLockerEnabled()) {
                return false;
            }

            String cacheKey = getCacheKey(world, x, y, z);
            CacheEntry cached = CACHE.get(cacheKey);
            if (cached != null && cached.isValid()) {
                return cached.protected_;
            }

            if (CACHE.size() > MAX_CACHE_SIZE) {
                CACHE.entrySet().removeIf(entry -> !entry.getValue().isValid());
            }

            if (!isContainerBlock(blockType)) {
                CACHE.put(cacheKey, new CacheEntry(false));
                return false;
            }

            Location location = new Location(world, x, y, z);
            Block block = world.getBlockAt(location);

            boolean protected_ = checkBlockLockerProtection(block);

            CACHE.put(cacheKey, new CacheEntry(protected_));

            return protected_;

        } catch (Exception e) {
            System.err.println("[BlockLocker] Error checking protection: " + e.getMessage());
            return false; 
        }
    }

    private static boolean checkBlockLockerProtection(Block block) {
        try {
            if (isProtectedMethod == null) {
                synchronized (BlockLockerIntegration.class) {
                    if (isProtectedMethod == null) {
                        
                        Class<?> apiClass = Class.forName("nl.rutgerkok.blocklocker.BlockLockerAPIv2");
                        
                        isProtectedMethod = apiClass.getMethod("isProtected", Block.class);
                    }
                }
            }

            Object result = isProtectedMethod.invoke(null, block);
            return result instanceof Boolean && (Boolean) result;

        } catch (ClassNotFoundException e) {
            blockLockerEnabled = false;
            System.err.println("[BlockLocker] API not found - plugin version may be incompatible");
            return false;
        } catch (Exception e) {
            System.err.println("[BlockLocker] Error calling API: " + e.getMessage());
            return false;
        }
    }

    private static boolean isContainerBlock(String blockType) {
        if (blockType == null) return false;
        
        String type = blockType.toLowerCase();
        return type.contains("chest") ||
               type.contains("furnace") ||
               type.contains("barrel") ||
               type.contains("hopper") ||
               type.contains("dropper") ||
               type.contains("dispenser") ||
               type.contains("shulker_box") ||
               type.contains("brewing_stand") ||
               type.contains("ender_chest");
    }

    private static boolean isBlockLockerEnabled() {
        Boolean enabled = blockLockerEnabled;
        if (enabled == null) {
            synchronized (BlockLockerIntegration.class) {
                enabled = blockLockerEnabled;
                if (enabled == null) {
                    blockLockerEnabled = enabled = Bukkit.getPluginManager().isPluginEnabled("BlockLocker");
                }
            }
        }
        return enabled;
    }

    private static String getCacheKey(World world, int x, int y, int z) {
        return world.getName() + ":" + x + "," + y + "," + z;
    }

    public static void clearCache() {
        CACHE.clear();
    }

    public static void reset() {
        blockLockerEnabled = null;
        isProtectedMethod = null;
        clearCache();
    }
}
