package org.virgil.akiasync.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

public class LandProtectionIntegration {
    
    private static volatile Boolean residenceEnabled = null;
    private static volatile Boolean dominionEnabled = null;
    private static volatile Boolean worldGuardEnabled = null;
    private static volatile Boolean landsEnabled = null;
    
    private static volatile Object residenceAPI = null;
    private static volatile Method residenceGetByLocMethod = null;
    private static volatile Method residenceHasFlagMethod = null;
    
    private static volatile Object dominionAPI = null;
    private static volatile Method dominionGetDominionMethod = null;
    private static volatile Method dominionGetFlagMethod = null;
    
    private static volatile Object worldGuardPlugin = null;
    private static volatile Object regionContainer = null;
    private static volatile Method createQueryMethod = null;
    
    private static volatile Object landsIntegration = null;
    private static volatile Method getLandByChunkMethod = null;
    private static volatile Method hasRoleFlagMethod = null;
    
    private static final ConcurrentHashMap<String, CacheEntry> CACHE = new ConcurrentHashMap<>();
    private static final long CACHE_DURATION_MS = 5000;
    private static final int MAX_CACHE_SIZE = 1000;
    
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
    
    public static boolean canTNTExplode(ServerLevel level, BlockPos pos) {
        try {
            String cacheKey = getCacheKey(level, pos);
            CacheEntry cached = CACHE.get(cacheKey);
            if (cached != null && cached.isValid()) {
                return cached.allowed;
            }
            
            if (CACHE.size() > MAX_CACHE_SIZE) {
                CACHE.entrySet().removeIf(entry -> !entry.getValue().isValid());
            }
            
            World bukkitWorld = level.getWorld();
            Location location = new Location(bukkitWorld, pos.getX(), pos.getY(), pos.getZ());
            
            boolean allowed = true;
            
            if (isResidenceEnabled()) {
                boolean residenceAllowed = checkResidence(location);
                if (!residenceAllowed) {
                    allowed = false;
                }
            }
            
            if (allowed && isDominionEnabled()) {
                boolean dominionAllowed = checkDominion(location);
                if (!dominionAllowed) {
                    allowed = false;
                }
            }
            
            if (allowed && isWorldGuardEnabled()) {
                boolean worldGuardAllowed = checkWorldGuard(location);
                if (!worldGuardAllowed) {
                    allowed = false;
                }
            }
            
            if (allowed && isLandsEnabled()) {
                boolean landsAllowed = checkLands(location);
                if (!landsAllowed) {
                    allowed = false;
                }
            }
            
            CACHE.put(cacheKey, new CacheEntry(allowed));
            
            return allowed;
        } catch (Exception e) {
            DebugLogger.error("[LandProtection] Error checking land protection: " + e.getMessage());
            return true;
        }
    }
    
    private static boolean checkResidence(Location location) {
        try {
            if (residenceAPI == null) {
                Plugin residence = Bukkit.getPluginManager().getPlugin("Residence");
                if (residence == null) {
                    residenceEnabled = false;
                    return true;
                }
                
                Class<?> residenceClass = Class.forName("com.bekvon.bukkit.residence.Residence");
                Method getAPIMethod = residenceClass.getMethod("getInstance");
                residenceAPI = getAPIMethod.invoke(null);
                
                Class<?> residenceAPIClass = Class.forName("com.bekvon.bukkit.residence.api.ResidenceApi");
                residenceGetByLocMethod = residenceAPIClass.getMethod("getByLoc", Location.class);
                
                Class<?> claimedResidenceClass = Class.forName("com.bekvon.bukkit.residence.protection.ClaimedResidence");
                residenceHasFlagMethod = claimedResidenceClass.getMethod("hasFlag", String.class);
            }
            
            Object claimedResidence = residenceGetByLocMethod.invoke(residenceAPI, location);
            if (claimedResidence == null) {
                return true;
            }
            
            Boolean hasTNTFlag = (Boolean) residenceHasFlagMethod.invoke(claimedResidence, "tnt");
            
            return hasTNTFlag != null && hasTNTFlag;
            
        } catch (ClassNotFoundException e) {
            residenceEnabled = false;
            return true;
        } catch (Exception e) {
            DebugLogger.error("[LandProtection] Error checking Residence: " + e.getMessage());
            return true;
        }
    }
    
    private static boolean checkDominion(Location location) {
        try {
            if (dominionAPI == null) {
                Plugin dominion = Bukkit.getPluginManager().getPlugin("Dominion");
                if (dominion == null) {
                    dominionEnabled = false;
                    return true;
                }
                
                Class<?> cacheClass = Class.forName("cn.lunadeer.dominion.Cache");
                Method getInstanceMethod = cacheClass.getMethod("instance");
                dominionAPI = getInstanceMethod.invoke(null);
                
                dominionGetDominionMethod = cacheClass.getMethod("getDominionByLoc", Location.class);
                
                Class<?> dominionDTOClass = Class.forName("cn.lunadeer.dominion.dtos.DominionDTO");
                dominionGetFlagMethod = dominionDTOClass.getMethod("getFlagValue", 
                    Class.forName("cn.lunadeer.dominion.dtos.Flag$FlagType"));
            }
            
            Object dominionDTO = dominionGetDominionMethod.invoke(dominionAPI, location);
            if (dominionDTO == null) {
                return true;
            }
            
            Class<?> flagTypeClass = Class.forName("cn.lunadeer.dominion.dtos.Flag$FlagType");
            Object tntFlag = null;
            for (Object enumConstant : flagTypeClass.getEnumConstants()) {
                if (enumConstant.toString().equals("TNT_EXPLODE")) {
                    tntFlag = enumConstant;
                    break;
                }
            }
            
            if (tntFlag == null) {
                DebugLogger.error("[LandProtection] TNT_EXPLODE flag not found in Dominion");
                return true;
            }
            
            Boolean allowed = (Boolean) dominionGetFlagMethod.invoke(dominionDTO, tntFlag);
            
            return allowed != null && allowed;
            
        } catch (ClassNotFoundException e) {
            dominionEnabled = false;
            return true;
        } catch (Exception e) {
            DebugLogger.error("[LandProtection] Error checking Dominion: " + e.getMessage());
            return true;
        }
    }
    
    private static boolean checkWorldGuard(Location location) {
        try {
            if (worldGuardPlugin == null) {
                Plugin wg = Bukkit.getPluginManager().getPlugin("WorldGuard");
                if (wg == null) {
                    worldGuardEnabled = false;
                    return true;
                }
                worldGuardPlugin = wg;
                
                Class<?> worldGuardClass = Class.forName("com.sk89q.worldguard.WorldGuard");
                Method getInstanceMethod = worldGuardClass.getMethod("getInstance");
                Object worldGuardInstance = getInstanceMethod.invoke(null);
                
                Method getPlatformMethod = worldGuardClass.getMethod("getPlatform");
                Object platform = getPlatformMethod.invoke(worldGuardInstance);
                
                Class<?> platformClass = Class.forName("com.sk89q.worldguard.platform.Platform");
                Method getRegionContainerMethod = platformClass.getMethod("getRegionContainer");
                regionContainer = getRegionContainerMethod.invoke(platform);
                
                Class<?> regionContainerClass = Class.forName("com.sk89q.worldguard.protection.regions.RegionContainer");
                createQueryMethod = regionContainerClass.getMethod("createQuery");
            }
            
            Object query = createQueryMethod.invoke(regionContainer);
            
            Class<?> wgLocationClass = Class.forName("com.sk89q.worldedit.util.Location");
            Class<?> bukkitAdapterClass = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter");
            Method adaptLocationMethod = bukkitAdapterClass.getMethod("adapt", Location.class);
            Object wgLocation = adaptLocationMethod.invoke(null, location);
            
            Class<?> flagsClass = Class.forName("com.sk89q.worldguard.protection.flags.Flags");
            Object tntFlag = flagsClass.getField("TNT").get(null);
            
            Class<?> regionQueryClass = Class.forName("com.sk89q.worldguard.protection.regions.RegionQuery");
            Class<?> stateFlagClass = Class.forName("com.sk89q.worldguard.protection.flags.StateFlag");
            Method testStateMethod = regionQueryClass.getMethod("testState", wgLocationClass, stateFlagClass);
            Object result = testStateMethod.invoke(query, wgLocation, tntFlag);
            
            if (result == null) {
                return true;
            }
            
            String stateName = result.toString();
            return "ALLOW".equals(stateName);
            
        } catch (ClassNotFoundException e) {
            worldGuardEnabled = false;
            return true;
        } catch (Exception e) {
            DebugLogger.error("[LandProtection] Error checking WorldGuard: " + e.getMessage());
            return true;
        }
    }
    
    private static boolean checkLands(Location location) {
        try {
            if (landsIntegration == null) {
                Plugin lands = Bukkit.getPluginManager().getPlugin("Lands");
                if (lands == null) {
                    landsEnabled = false;
                    return true;
                }
                
                Class<?> landsIntegrationClass = Class.forName("me.angeschossen.lands.api.integration.LandsIntegration");
                Method getInstanceMethod = landsIntegrationClass.getMethod("of", Plugin.class);
                landsIntegration = getInstanceMethod.invoke(null, lands);
                
                getLandByChunkMethod = landsIntegrationClass.getMethod("getLandByChunk", World.class, int.class, int.class);
                
                Class<?> landClass = Class.forName("me.angeschossen.lands.api.land.Land");
                Class<?> roleFlagClass = Class.forName("me.angeschossen.lands.api.flags.type.RoleFlag");
                hasRoleFlagMethod = landClass.getMethod("hasRoleFlag", 
                    Class.forName("me.angeschossen.lands.api.player.LandPlayer"),
                    roleFlagClass,
                    boolean.class);
            }
            
            int chunkX = location.getBlockX() >> 4;
            int chunkZ = location.getBlockZ() >> 4;
            
            Object land = getLandByChunkMethod.invoke(landsIntegration, location.getWorld(), chunkX, chunkZ);
            if (land == null) {
                return true;
            }
            
            Class<?> roleFlagsClass = Class.forName("me.angeschossen.lands.api.flags.type.Flags");
            Object tntFlag = roleFlagsClass.getField("BLOCK_IGNITE").get(null);
            
            Boolean allowed = (Boolean) hasRoleFlagMethod.invoke(land, null, tntFlag, true);
            
            return allowed != null && allowed;
            
        } catch (ClassNotFoundException e) {
            landsEnabled = false;
            return true;
        } catch (Exception e) {
            DebugLogger.error("[LandProtection] Error checking Lands: " + e.getMessage());
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
    
    private static String getCacheKey(ServerLevel level, BlockPos pos) {
        return level.dimension().location().toString() + ":" + 
               pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }
    
    public static void clearCache() {
        CACHE.clear();
    }
    
    public static void reset() {
        residenceEnabled = null;
        dominionEnabled = null;
        worldGuardEnabled = null;
        landsEnabled = null;
        residenceAPI = null;
        dominionAPI = null;
        worldGuardPlugin = null;
        regionContainer = null;
        landsIntegration = null;
        residenceGetByLocMethod = null;
        residenceHasFlagMethod = null;
        dominionGetDominionMethod = null;
        dominionGetFlagMethod = null;
        createQueryMethod = null;
        getLandByChunkMethod = null;
        hasRoleFlagMethod = null;
        clearCache();
    }
}
