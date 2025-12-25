package org.virgil.akiasync.util;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public class LandProtectionIntegration {

    private static volatile Boolean residenceEnabled = null;
    private static volatile Boolean dominionEnabled = null;
    private static volatile Boolean worldGuardEnabled = null;
    private static volatile Boolean landsEnabled = null;

    private static volatile Object residenceAPI = null;
    private static volatile Object dominionCache = null;
    private static volatile Object worldGuardQuery = null;
    private static volatile Object landsIntegration = null;

    private static final ConcurrentHashMap<String, CacheEntry> CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, ChunkCacheEntry> CHUNK_CACHE = new ConcurrentHashMap<>();
    private static final long CACHE_DURATION_MS = 30000; 
    private static final int MAX_CACHE_SIZE = 50000;
    private static final int MAX_CHUNK_CACHE_SIZE = 1000;

    private static class CacheEntry {
        final boolean allowed;
        final long timestamp;

        CacheEntry(boolean allowed) {
            this.allowed = allowed;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isValid() {
            return System.currentTimeMillis() - timestamp < CACHE_DURATION_MS;
        }
    }

    private static class ChunkCacheEntry {
        final Boolean allAllowed;
        final long timestamp;

        ChunkCacheEntry(Boolean allAllowed) {
            this.allAllowed = allAllowed;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isValid() {
            return System.currentTimeMillis() - timestamp < CACHE_DURATION_MS;
        }
    }

    public static boolean canTNTExplode(World world, int x, int y, int z) {
        return canTNTExplode(world, x, y, z, false);
    }

    public static boolean canTNTExplode(World world, int x, int y, int z, boolean debug) {
        try {
            
            String cacheKey = getCacheKey(world, x, y, z);
            CacheEntry cached = CACHE.get(cacheKey);
            if (cached != null && cached.isValid()) {
                return cached.allowed;
            }

            String chunkKey = getChunkCacheKey(world, x, z);
            ChunkCacheEntry chunkCached = CHUNK_CACHE.get(chunkKey);
            if (chunkCached != null && chunkCached.isValid() && chunkCached.allAllowed != null) {
                boolean allowed = chunkCached.allAllowed;
                CACHE.putIfAbsent(cacheKey, new CacheEntry(allowed));
                return allowed;
            }

            if (CACHE.size() > MAX_CACHE_SIZE) {
                CACHE.entrySet().removeIf(entry -> !entry.getValue().isValid());
            }
            if (CHUNK_CACHE.size() > MAX_CHUNK_CACHE_SIZE) {
                CHUNK_CACHE.entrySet().removeIf(entry -> !entry.getValue().isValid());
            }

            Location location = new Location(world, x, y, z);

            boolean allowed = true;

            if (isResidenceEnabled()) {
                allowed = checkResidence(location, debug);
            }

            if (allowed && isDominionEnabled()) {
                allowed = checkDominion(location);
            }

            if (allowed && isWorldGuardEnabled()) {
                allowed = checkWorldGuard(location);
            }

            if (allowed && isLandsEnabled()) {
                allowed = checkLands(location);
            }

            CACHE.put(cacheKey, new CacheEntry(allowed));

            return allowed;
        } catch (Exception e) {
            System.err.println("[LandProtection] Error checking land protection: " + e.getMessage());
            return true; 
        }
    }

    public static Boolean checkChunkProtection(World world, int chunkX, int chunkZ) {
        try {
            String chunkKey = getChunkCacheKeyDirect(world, chunkX, chunkZ);
            ChunkCacheEntry cached = CHUNK_CACHE.get(chunkKey);
            if (cached != null && cached.isValid()) {
                return cached.allAllowed;
            }

            int baseX = chunkX << 4;
            int baseZ = chunkZ << 4;
            
            Boolean firstResult = null;
            boolean allSame = true;

            int[] sampleX = {0, 8, 15, 0, 15, 0, 8, 15, 8};
            int[] sampleZ = {0, 0, 0, 8, 8, 15, 15, 15, 8};

            for (int i = 0; i < sampleX.length; i++) {
                boolean allowed = canTNTExplode(world, baseX + sampleX[i], 64, baseZ + sampleZ[i]);
                
                if (firstResult == null) {
                    firstResult = allowed;
                } else if (firstResult != allowed) {
                    allSame = false;
                    break;
                }
            }

            Boolean result = allSame ? firstResult : Boolean.FALSE;
            CHUNK_CACHE.put(chunkKey, new ChunkCacheEntry(result));
            
            return result;
        } catch (Exception e) {
            System.err.println("[LandProtection] Error checking chunk protection: " + e.getMessage());
            return Boolean.FALSE; 
        }
    }

    private static boolean checkResidence(Location location, boolean debug) {
        try {
            if (residenceAPI == null) {
                Class<?> residenceClass = Class.forName("com.bekvon.bukkit.residence.Residence");
                Method getInstanceMethod = residenceClass.getMethod("getInstance");
                Object residence = getInstanceMethod.invoke(null);
                
                if (residence == null) {
                    residenceEnabled = false;
                    return true;
                }
                
                Method getResidenceManagerMethod = residenceClass.getMethod("getResidenceManager");
                residenceAPI = getResidenceManagerMethod.invoke(residence);
                
                if (residenceAPI == null) {
                    residenceEnabled = false;
                    return true;
                }
            }

            Method getByLocMethod = residenceAPI.getClass().getMethod("getByLoc", Location.class);
            Object res = getByLocMethod.invoke(residenceAPI, location);
            
            if (res == null) {
                return true;
            }

            Method getPermissionsMethod = res.getClass().getMethod("getPermissions");
            Object permissions = getPermissionsMethod.invoke(res);
            Method hasMethod = permissions.getClass().getMethod("has", String.class, boolean.class);
            boolean allowed = (Boolean) hasMethod.invoke(permissions, "tnt", false);
            
            if (!allowed && debug) {
                Method getNameMethod = res.getClass().getMethod("getName");
                String resName = (String) getNameMethod.invoke(res);
                org.virgil.akiasync.util.DebugLogger.debug("[LandProtection] Residence claim '" + resName + 
                    "' denying TNT at " + location.getBlockX() + "," + 
                    location.getBlockY() + "," + location.getBlockZ());
            }
            return allowed;

        } catch (Exception e) {
            if (residenceEnabled == null) {
                residenceEnabled = false;
            }
            return true;
        }
    }

    private static boolean checkDominion(Location location) {
        try {
            if (dominionCache == null) {
                Class<?> dominionClass = Class.forName("cn.lunadeer.dominion.api.DominionAPI");
                Method getInstanceMethod = dominionClass.getMethod("getInstance");
                dominionCache = getInstanceMethod.invoke(null);
                
                if (dominionCache == null) {
                    dominionEnabled = false;
                    return true;
                }
            }

            Class<?> flagsClass = Class.forName("cn.lunadeer.dominion.api.dtos.flag.Flags");
            Object tntExplodeFlag = flagsClass.getField("TNT_EXPLODE").get(null);
            
            Method checkFlagMethod = dominionCache.getClass().getMethod("checkEnvironmentFlag", 
                Location.class, Class.forName("cn.lunadeer.dominion.api.dtos.flag.Flag"));
            return (Boolean) checkFlagMethod.invoke(dominionCache, location, tntExplodeFlag);

        } catch (Exception e) {
            if (dominionEnabled == null) {
                dominionEnabled = false;
            }
            return true;
        }
    }

    private static boolean checkWorldGuard(Location location) {
        try {
            if (worldGuardQuery == null) {
                Class<?> wgClass = Class.forName("com.sk89q.worldguard.WorldGuard");
                Object wg = wgClass.getMethod("getInstance").invoke(null);
                if (wg == null) {
                    worldGuardEnabled = false;
                    return true;
                }
                
                Object platform = wgClass.getMethod("getPlatform").invoke(wg);
                Class<?> platformClass = Class.forName("com.sk89q.worldguard.internal.platform.WorldGuardPlatform");
                Object container = platformClass.getMethod("getRegionContainer").invoke(platform);
                
                if (container == null) {
                    worldGuardEnabled = false;
                    return true;
                }
                
                Class<?> containerClass = Class.forName("com.sk89q.worldguard.protection.regions.RegionContainer");
                worldGuardQuery = containerClass.getMethod("createQuery").invoke(container);
                if (worldGuardQuery == null) {
                    worldGuardEnabled = false;
                    return true;
                }
            }

            Class<?> adapterClass = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter");
            Object wgLocation = adapterClass.getMethod("adapt", Location.class).invoke(null, location);
            
            Class<?> flagsClass = Class.forName("com.sk89q.worldguard.protection.flags.Flags");
            Object tntFlag = flagsClass.getField("TNT").get(null);
            
            Class<?> queryClass = Class.forName("com.sk89q.worldguard.protection.regions.RegionQuery");
            Class<?> wgLocClass = Class.forName("com.sk89q.worldedit.util.Location");
            Class<?> stateFlagClass = Class.forName("com.sk89q.worldguard.protection.flags.StateFlag");
            
            Object state = queryClass.getMethod("testState", wgLocClass, stateFlagClass)
                .invoke(worldGuardQuery, wgLocation, tntFlag);
            
            return state == null || !"DENY".equals(state.toString());

        } catch (Exception e) {
            if (worldGuardEnabled == null) {
                worldGuardEnabled = false;
            }
            return true;
        }
    }

    private static boolean checkLands(Location location) {
        try {
            if (landsIntegration == null) {
                Class<?> integrationClass = Class.forName("me.angeschossen.lands.api.integration.LandsIntegration");
                
                org.bukkit.plugin.Plugin landsPlugin = Bukkit.getPluginManager().getPlugin("Lands");
                if (landsPlugin == null) {
                    landsEnabled = false;
                    return true;
                }
                
                Object integration = integrationClass.getMethod("of", org.bukkit.plugin.Plugin.class)
                    .invoke(null, landsPlugin);
                if (integration == null) {
                    landsEnabled = false;
                    return true;
                }
                landsIntegration = integration;
            }

            int chunkX = location.getBlockX() >> 4;
            int chunkZ = location.getBlockZ() >> 4;

            Object land = landsIntegration.getClass()
                .getMethod("getLandByChunk", World.class, int.class, int.class)
                .invoke(landsIntegration, location.getWorld(), chunkX, chunkZ);
            
            if (land == null) {
                return true;
            }

            Class<?> flagsClass = Class.forName("me.angeschossen.lands.api.flags.type.Flags");
            Object blockIgniteFlag = flagsClass.getField("BLOCK_IGNITE").get(null);
            
            Class<?> landClass = land.getClass();
            Class<?> roleFlagClass = Class.forName("me.angeschossen.lands.api.flags.type.RoleFlag");
            
            Object result = landClass.getMethod("hasRoleFlag", 
                Class.forName("me.angeschossen.lands.api.player.LandPlayer"), 
                roleFlagClass, 
                boolean.class)
                .invoke(land, null, blockIgniteFlag, true);
            
            return result instanceof Boolean && (Boolean) result;

        } catch (Exception e) {
            if (landsEnabled == null) {
                landsEnabled = false;
            }
            return true;
        }
    }

    private static boolean isResidenceEnabled() {
        if (residenceEnabled == null) {
            residenceEnabled = Bukkit.getPluginManager().isPluginEnabled("Residence");
        }
        return residenceEnabled;
    }

    private static boolean isDominionEnabled() {
        if (dominionEnabled == null) {
            dominionEnabled = Bukkit.getPluginManager().isPluginEnabled("Dominion");
        }
        return dominionEnabled;
    }

    private static boolean isWorldGuardEnabled() {
        if (worldGuardEnabled == null) {
            worldGuardEnabled = Bukkit.getPluginManager().isPluginEnabled("WorldGuard");
        }
        return worldGuardEnabled;
    }

    private static boolean isLandsEnabled() {
        if (landsEnabled == null) {
            landsEnabled = Bukkit.getPluginManager().isPluginEnabled("Lands");
        }
        return landsEnabled;
    }

    private static String getCacheKey(World world, int x, int y, int z) {
        return world.getName() + ":" + x + "," + y + "," + z;
    }

    private static String getChunkCacheKey(World world, int x, int z) {
        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        return getChunkCacheKeyDirect(world, chunkX, chunkZ);
    }

    private static String getChunkCacheKeyDirect(World world, int chunkX, int chunkZ) {
        return world.getName() + ":chunk:" + chunkX + "," + chunkZ;
    }

    public static void clearCache() {
        CACHE.clear();
        CHUNK_CACHE.clear();
    }

    public static void reset() {
        residenceEnabled = null;
        dominionEnabled = null;
        worldGuardEnabled = null;
        landsEnabled = null;
        
        residenceAPI = null;
        dominionCache = null;
        worldGuardQuery = null;
        landsIntegration = null;
        
        clearCache();
    }
}
