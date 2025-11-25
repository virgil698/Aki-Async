package org.virgil.akiasync.throttling;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.virgil.akiasync.AkiAsyncPlugin;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class EntityThrottlingManager {

    private final AkiAsyncPlugin plugin;
    private FileConfiguration throttlingConfig;
    private File throttlingFile;

    private final Map<EntityType, EntityLimit> entityLimits = new ConcurrentHashMap<>();

    private final Map<EntityType, EntityCounter> entityCounters = new ConcurrentHashMap<>();

    private int taskId = -1;

    private boolean enabled;
    private int checkInterval;
    private int throttleInterval;
    private int removalBatchSize;

    public EntityThrottlingManager(AkiAsyncPlugin plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        enabled = plugin.getConfigManager().isEntityThrottlingEnabled();
        if (!enabled) {
            plugin.getLogger().info("[EntityThrottling] Entity throttling is disabled");
            return;
        }

        checkInterval = plugin.getConfigManager().getEntityThrottlingCheckInterval();
        throttleInterval = plugin.getConfigManager().getEntityThrottlingThrottleInterval();
        removalBatchSize = plugin.getConfigManager().getEntityThrottlingRemovalBatchSize();

        loadThrottlingConfig();

        startCheckTask();

        plugin.getLogger().info("[EntityThrottling] Entity throttling initialized:");
        plugin.getLogger().info("  - Check interval: " + checkInterval + " ticks");
        plugin.getLogger().info("  - Throttle interval: " + throttleInterval + " ticks");
        plugin.getLogger().info("  - Removal batch size: " + removalBatchSize);
        plugin.getLogger().info("  - Configured entity types: " + entityLimits.size());
    }

    private void loadThrottlingConfig() {
        String configFileName = plugin.getConfigManager().getEntityThrottlingConfigFile();
        throttlingFile = new File(plugin.getDataFolder(), configFileName);

        if (!throttlingFile.exists()) {
            plugin.saveResource(configFileName, false);
        }

        throttlingConfig = YamlConfiguration.loadConfiguration(throttlingFile);

        entityLimits.clear();
        for (String key : throttlingConfig.getKeys(false)) {
            if (throttlingConfig.isConfigurationSection(key)) {
                ConfigurationSection section = throttlingConfig.getConfigurationSection(key);
                if (section != null) {
                    int limit = section.getInt("limit", -1);
                    int removal = section.getInt("removal", -1);

                    if (limit > 0 && removal > 0 && limit < removal) {
                        try {
                            EntityType type = EntityType.valueOf(key.toUpperCase().replace("MINECRAFT:", ""));
                            entityLimits.put(type, new EntityLimit(limit, removal));
                        } catch (IllegalArgumentException e) {
                            plugin.getLogger().warning("[EntityThrottling] Unknown entity type: " + key);
                        }
                    } else if (limit > 0 || removal > 0) {
                        plugin.getLogger().warning("[EntityThrottling] Invalid configuration for " + key + 
                            " (limit=" + limit + ", removal=" + removal + "). Limit must be less than removal.");
                    }
                }
            }
        }
    }

    private void startCheckTask() {
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            try {
                checkAndThrottle();
            } catch (Exception e) {
                plugin.getLogger().warning("[EntityThrottling] Error during check: " + e.getMessage());
            }
        }, checkInterval, checkInterval);
    }

    private void checkAndThrottle() {
        entityCounters.clear();

        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                EntityType type = entity.getType();
                if (entityLimits.containsKey(type)) {
                    EntityCounter counter = entityCounters.computeIfAbsent(type, k -> new EntityCounter());
                    counter.addEntity(entity);
                }
            }
        }

        for (Map.Entry<EntityType, EntityLimit> entry : entityLimits.entrySet()) {
            EntityType type = entry.getKey();
            EntityLimit limit = entry.getValue();
            EntityCounter counter = entityCounters.get(type);

            if (counter == null) continue;

            int count = counter.getCount();

            if (count > limit.getRemoval()) {
                int toRemove = Math.min(count - limit.getRemoval(), removalBatchSize);
                removeOldestEntities(counter, toRemove);
                
                if (plugin.getConfigManager().isDebugLoggingEnabled()) {
                    plugin.getLogger().info(String.format(
                        "[EntityThrottling] %s: %d/%d (removed %d oldest)",
                        type.name(), count, limit.getRemoval(), toRemove
                    ));
                }
            }
            else if (count > limit.getLimit()) {
                counter.setThrottled(true);
                
                if (plugin.getConfigManager().isDebugLoggingEnabled()) {
                    plugin.getLogger().info(String.format(
                        "[EntityThrottling] %s: %d/%d (throttled)",
                        type.name(), count, limit.getLimit()
                    ));
                }
            }
        }
    }

    private void removeOldestEntities(EntityCounter counter, int count) {
        List<Entity> entities = counter.getEntities();
        
        entities.sort(Comparator.comparingInt(Entity::getTicksLived).reversed());

        int removed = 0;
        for (Entity entity : entities) {
            if (removed >= count) break;
            
            if (entity.customName() != null) continue;
            
            if (entity instanceof org.bukkit.entity.LivingEntity) {
                org.bukkit.entity.LivingEntity living = (org.bukkit.entity.LivingEntity) entity;
                if (living.isLeashed()) continue;
            }
            
            if (entity.isInsideVehicle()) continue;
            
            entity.remove();
            removed++;
        }
    }

    public boolean shouldThrottle(Entity entity) {
        if (!enabled) return false;

        EntityType type = entity.getType();
        EntityCounter counter = entityCounters.get(type);
        
        if (counter == null || !counter.isThrottled()) {
            return false;
        }

        return entity.getTicksLived() % throttleInterval != 0;
    }

    public void reload() {
        shutdown();
        initialize();
    }

    public void shutdown() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
        entityLimits.clear();
        entityCounters.clear();
    }

    private static class EntityLimit {
        private final int limit;
        private final int removal;

        public EntityLimit(int limit, int removal) {
            this.limit = limit;
            this.removal = removal;
        }

        public int getLimit() {
            return limit;
        }

        public int getRemoval() {
            return removal;
        }
    }

    private static class EntityCounter {
        private final List<Entity> entities = new ArrayList<>();
        private boolean throttled = false;

        public void addEntity(Entity entity) {
            entities.add(entity);
        }

        public int getCount() {
            return entities.size();
        }

        public List<Entity> getEntities() {
            return entities;
        }

        public boolean isThrottled() {
            return throttled;
        }

        public void setThrottled(boolean throttled) {
            this.throttled = throttled;
        }
    }
}
