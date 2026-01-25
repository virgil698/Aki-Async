package org.virgil.akiasync.config;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.logging.Logger;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class ConfigMigrator {

    private final File dataFolder;
    private final Logger logger;

    private static final Map<String, String> KEY_RENAMES = new LinkedHashMap<>();

    private static final Set<String> REMOVED_KEYS = new HashSet<>();

    private static final Map<String, DefaultValueChange> DEFAULT_CHANGES = new HashMap<>();

    private final List<String> migrationReport = new ArrayList<>();

    static {

        KEY_RENAMES.put("chunk-tick-async.enabled", "chunk-system.block-tick.async.enabled");
        KEY_RENAMES.put("chunk-tick-async.timeout-us", "chunk-system.block-tick.async.timeout-us");
        KEY_RENAMES.put("chunk-tick-async.batch-size", "chunk-system.block-tick.async.batch-size");

        KEY_RENAMES.put("chunk-generation.noise-optimization.enabled", "chunk-system.generation.noise-optimization.enabled");
        KEY_RENAMES.put("chunk-generation.jigsaw-optimization.enabled", "chunk-system.generation.jigsaw-optimization.enabled");
        KEY_RENAMES.put("chunk-generation.c2me-chunk-optimizations.chunk-pos-optimization.enabled", "chunk-system.generation.chunk-pos-optimization.enabled");
        KEY_RENAMES.put("chunk-generation.c2me-chunk-optimizations.noise-optimization.enabled", "chunk-system.generation.perlin-noise-optimization.enabled");

        KEY_RENAMES.put("chunk-visibility-filter.enabled", "network-optimization.chunk-visibility-filter.enabled");

        KEY_RENAMES.put("fast-movement-chunk-load.enabled", "network-optimization.fast-movement-chunk-load.enabled");
        KEY_RENAMES.put("fast-movement-chunk-load.speed-threshold", "network-optimization.fast-movement-chunk-load.speed-threshold");
        KEY_RENAMES.put("fast-movement-chunk-load.preload-distance", "network-optimization.fast-movement-chunk-load.preload-distance");
        KEY_RENAMES.put("fast-movement-chunk-load.max-concurrent-loads", "network-optimization.fast-movement-chunk-load.max-concurrent-loads");
        KEY_RENAMES.put("fast-movement-chunk-load.prediction-ticks", "network-optimization.fast-movement-chunk-load.prediction-ticks");
        KEY_RENAMES.put("fast-movement-chunk-load.center-offset.enabled", "network-optimization.fast-movement-chunk-load.center-offset.enabled");
        KEY_RENAMES.put("fast-movement-chunk-load.center-offset.min-speed", "network-optimization.fast-movement-chunk-load.center-offset.min-speed");
        KEY_RENAMES.put("fast-movement-chunk-load.center-offset.max-speed", "network-optimization.fast-movement-chunk-load.center-offset.max-speed");
        KEY_RENAMES.put("fast-movement-chunk-load.center-offset.max-offset-ratio", "network-optimization.fast-movement-chunk-load.center-offset.max-offset-ratio");
        KEY_RENAMES.put("fast-movement-chunk-load.async-loading.batch-size", "network-optimization.fast-movement-chunk-load.async-loading.batch-size");
        KEY_RENAMES.put("fast-movement-chunk-load.async-loading.batch-delay-ms", "network-optimization.fast-movement-chunk-load.async-loading.batch-delay-ms");
        KEY_RENAMES.put("fast-movement-chunk-load.player-join-warmup.enabled", "network-optimization.fast-movement-chunk-load.player-join-warmup.enabled");
        KEY_RENAMES.put("fast-movement-chunk-load.player-join-warmup.warmup-duration-ms", "network-optimization.fast-movement-chunk-load.player-join-warmup.warmup-duration-ms");
        KEY_RENAMES.put("fast-movement-chunk-load.player-join-warmup.initial-rate", "network-optimization.fast-movement-chunk-load.player-join-warmup.initial-rate");
        KEY_RENAMES.put("fast-movement-chunk-load.map-rendering.enabled", "network-optimization.fast-movement-chunk-load.map-rendering.enabled");

        KEY_RENAMES.put("advanced-concurrency.virtual-threads", "advanced-optimizations.virtual-threads");
        KEY_RENAMES.put("advanced-concurrency.work-stealing", "advanced-optimizations.work-stealing");

        REMOVED_KEYS.add("math-optimization.c2me-optimizations.resource-location-cache.enabled");

    }

    public ConfigMigrator(File dataFolder, Logger logger) {
        this.dataFolder = dataFolder;
        this.logger = logger;
    }

    public boolean migrate(int oldVersion, int newVersion) {
        migrationReport.clear();
        migrationReport.add("==========================================");
        migrationReport.add("AkiAsync Config Migration Report");
        migrationReport.add("==========================================");
        migrationReport.add("Old Version: " + oldVersion);
        migrationReport.add("New Version: " + newVersion);
        migrationReport.add("Migration Time: " + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        migrationReport.add("");

        try {
            File configFile = new File(dataFolder, "config.yml");
            File backupFile = new File(dataFolder, "config.yml.v" + oldVersion + ".bak");
            File templateFile = new File(dataFolder, "config.yml.template");

            if (!configFile.exists()) {
                logger.warning("[ConfigMigrator] Config file not found, skipping migration");
                return false;
            }

            FileConfiguration oldConfig = YamlConfiguration.loadConfiguration(configFile);

            if (!backupConfig(configFile, backupFile)) {
                logger.severe("[ConfigMigrator] Failed to backup old config");
                return false;
            }
            migrationReport.add("[OK] Backed up old config to: " + backupFile.getName());

            extractDefaultConfig(templateFile);
            FileConfiguration newConfig = YamlConfiguration.loadConfiguration(templateFile);

            int mergedCount = smartMerge(oldConfig, newConfig, oldVersion, newVersion);
            migrationReport.add("");
            migrationReport.add("[OK] Migrated " + mergedCount + " user settings");

            newConfig.set("version", newVersion);

            newConfig.save(configFile);
            migrationReport.add("[OK] Saved new config");

            if (templateFile.exists()) {
                templateFile.delete();
            }

            migrationReport.add("");
            migrationReport.add("==========================================");
            migrationReport.add("Migration Complete!");
            migrationReport.add("==========================================");

            saveMigrationReport(oldVersion, newVersion);
            printMigrationReport();

            return true;

        } catch (Exception e) {
            logger.severe("[ConfigMigrator] Migration failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private int smartMerge(FileConfiguration oldConfig, FileConfiguration newConfig,
                           int oldVersion, int newVersion) {
        int mergedCount = 0;
        Set<String> processedKeys = new HashSet<>();
        List<String> newKeys = new ArrayList<>();

        for (String oldKey : getAllKeys(oldConfig)) {

            if (oldKey.equals("version")) {
                continue;
            }

            if (REMOVED_KEYS.contains(oldKey)) {
                migrationReport.add("  [SKIP] Removed key: " + oldKey);
                continue;
            }

            String newKey = KEY_RENAMES.getOrDefault(oldKey, oldKey);
            if (!newKey.equals(oldKey)) {
                migrationReport.add("  [RENAME] " + oldKey + " -> " + newKey);
            }

            Object oldValue = oldConfig.get(oldKey);
            Object newDefault = newConfig.get(newKey);

            if (oldValue instanceof ConfigurationSection) {
                continue;
            }

            if (newDefault == null && !newConfig.contains(newKey)) {

                migrationReport.add("  [SKIP] Key not in new config: " + oldKey);
                continue;
            }

            DefaultValueChange defaultChange = DEFAULT_CHANGES.get(newKey);
            if (defaultChange != null && Objects.equals(oldValue, defaultChange.oldDefault)) {

                migrationReport.add("  [UPDATE] Default changed: " + newKey +
                                   " (" + defaultChange.oldDefault + " -> " + defaultChange.newDefault + ")");
                continue;
            }

            if (!Objects.equals(oldValue, newDefault)) {

                newConfig.set(newKey, oldValue);
                migrationReport.add("  [KEEP] User setting: " + newKey + " = " + formatValue(oldValue));
                mergedCount++;
            }

            processedKeys.add(newKey);
        }

        for (String newKey : getAllKeys(newConfig)) {
            if (!processedKeys.contains(newKey) && !newKey.equals("version")) {

                boolean existsInOld = false;
                for (Map.Entry<String, String> rename : KEY_RENAMES.entrySet()) {
                    if (rename.getValue().equals(newKey)) {
                        existsInOld = true;
                        break;
                    }
                }
                if (!existsInOld && !oldConfig.contains(newKey)) {
                    newKeys.add(newKey);
                }
            }
        }

        if (!newKeys.isEmpty()) {
            migrationReport.add("");
            migrationReport.add("[NEW] New Config Keys (v" + newVersion + "):");

            Map<String, List<String>> grouped = new LinkedHashMap<>();
            for (String key : newKeys) {
                String prefix = key.contains(".") ? key.substring(0, key.indexOf('.')) : key;
                grouped.computeIfAbsent(prefix, k -> new ArrayList<>()).add(key);
            }
            for (Map.Entry<String, List<String>> entry : grouped.entrySet()) {
                if (entry.getValue().size() > 5) {
                    migrationReport.add("  + " + entry.getKey() + ".* (" + entry.getValue().size() + " items)");
                } else {
                    for (String key : entry.getValue()) {
                        migrationReport.add("  + " + key);
                    }
                }
            }
        }

        return mergedCount;
    }

    private Set<String> getAllKeys(FileConfiguration config) {
        Set<String> keys = new LinkedHashSet<>();
        collectKeys(config, "", keys);
        return keys;
    }

    private void collectKeys(ConfigurationSection section, String prefix, Set<String> keys) {
        for (String key : section.getKeys(false)) {
            String fullKey = prefix.isEmpty() ? key : prefix + "." + key;
            Object value = section.get(key);

            if (value instanceof ConfigurationSection) {
                collectKeys((ConfigurationSection) value, fullKey, keys);
            } else {
                keys.add(fullKey);
            }
        }
    }

    private boolean backupConfig(File source, File backup) {
        try {

            if (backup.exists()) {
                String timestamp = new java.text.SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
                backup = new File(dataFolder, backup.getName().replace(".bak", "-" + timestamp + ".bak"));
            }

            Files.copy(source.toPath(), backup.toPath());
            return true;
        } catch (IOException e) {
            logger.severe("[ConfigMigrator] Backup failed: " + e.getMessage());
            return false;
        }
    }

    private void extractDefaultConfig(File templateFile) throws IOException {

        try (java.io.InputStream is = ConfigMigrator.class.getResourceAsStream("/config.yml")) {
            if (is == null) {
                throw new IOException("Default config.yml not found in jar");
            }
            Files.copy(is, templateFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private String formatValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String) {
            return "\"" + value + "\"";
        }
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            if (list.size() > 3) {
                return "[" + list.size() + " items]";
            }
            return value.toString();
        }
        return value.toString();
    }

    private void saveMigrationReport(int oldVersion, int newVersion) {
        try {
            File reportFile = new File(dataFolder, "migration-report-v" + oldVersion + "-to-v" + newVersion + ".txt");
            StringBuilder sb = new StringBuilder();
            for (String line : migrationReport) {
                sb.append(line).append("\n");
            }
            Files.write(reportFile.toPath(), sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            logger.info("[ConfigMigrator] Migration report saved to: " + reportFile.getName());
        } catch (IOException e) {
            logger.warning("[ConfigMigrator] Failed to save migration report: " + e.getMessage());
        }
    }

    private void printMigrationReport() {
        logger.info("");
        for (String line : migrationReport) {
            if (line.startsWith("✓") || line.startsWith("→") || line.startsWith("↑")) {
                logger.info("[ConfigMigrator] " + line);
            } else if (line.startsWith("⊘")) {
                logger.warning("[ConfigMigrator] " + line);
            } else if (line.startsWith("=")) {
                logger.info("[ConfigMigrator] " + line);
            } else {
                logger.info("[ConfigMigrator] " + line);
            }
        }
        logger.info("");
    }

    public List<String> getMigrationReport() {
        return new ArrayList<>(migrationReport);
    }

    public static boolean needsMigration(FileConfiguration config, int currentVersion) {
        int configVersion = config.getInt("version", 0);
        return configVersion != 0 && configVersion != currentVersion;
    }

    public static int getConfigVersion(FileConfiguration config) {
        return config.getInt("version", 0);
    }

    public void registerVersionMigrations() {

    }

    public static void addKeyRename(String oldKey, String newKey) {
        KEY_RENAMES.put(oldKey, newKey);
    }

    public static void addRemovedKey(String key) {
        REMOVED_KEYS.add(key);
    }

    public static void addDefaultChange(String key, Object oldDefault, Object newDefault) {
        DEFAULT_CHANGES.put(key, new DefaultValueChange(oldDefault, newDefault));
    }

    private static class DefaultValueChange {
        final Object oldDefault;
        final Object newDefault;

        DefaultValueChange(Object oldDefault, Object newDefault) {
            this.oldDefault = oldDefault;
            this.newDefault = newDefault;
        }
    }
}
