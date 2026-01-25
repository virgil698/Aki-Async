package org.virgil.akiasync.bridge.delegates;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.virgil.akiasync.AkiAsyncPlugin;
import org.virgil.akiasync.config.ConfigManager;

/**
 * Delegate class handling seed encryption related bridge methods.
 * Extracted from AkiAsyncBridge to reduce its complexity.
 */
public class SeedBridgeDelegate {

    private final AkiAsyncPlugin plugin;
    private ConfigManager config;

    public SeedBridgeDelegate(AkiAsyncPlugin plugin, ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
    }

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "ConfigManager is intentionally shared")
    public void updateConfig(ConfigManager newConfig) {
        this.config = newConfig;
    }

    public boolean isSeedProtectionEnabled() {
        return config != null && config.isSeedProtectionEnabled();
    }

    public boolean shouldReturnFakeSeed() {
        return config != null && config.shouldReturnFakeSeed();
    }

    public long getFakeSeedValue() {
        return config != null ? config.getFakeSeedValue() : 0L;
    }

    public boolean isQuantumSeedEnabled() {
        return config != null && config.isQuantumSeedEnabled();
    }

    public byte[] getQuantumServerKey() {
        org.virgil.akiasync.crypto.QuantumSeedManager manager = plugin.getQuantumSeedManager();
        if (manager == null) {
            return null;
        }

        try {
            java.lang.reflect.Field keyManagerField = manager.getClass().getDeclaredField("keyManager");
            keyManagerField.setAccessible(true);
            org.virgil.akiasync.crypto.ServerKeyManager keyManager =
                (org.virgil.akiasync.crypto.ServerKeyManager) keyManagerField.get(manager);
            return keyManager.getServerKey();
        } catch (Exception e) {
            plugin.getLogger().warning("[AkiAsync-Bridge] Failed to get server key: " + e.getMessage());
            return null;
        }
    }

    public long getEncryptedSeed(long originalSeed, int chunkX, int chunkZ, String dimension, String generationType, long gameTime) {
        org.virgil.akiasync.crypto.QuantumSeedManager manager = plugin.getQuantumSeedManager();
        if (manager == null) {
            return originalSeed;
        }

        org.virgil.akiasync.mixin.crypto.quantum.GenerationType type;
        try {
            type = org.virgil.akiasync.mixin.crypto.quantum.GenerationType.valueOf(generationType.toUpperCase());
        } catch (Exception e) {
            type = org.virgil.akiasync.mixin.crypto.quantum.GenerationType.DECORATION;
        }

        return manager.getEncryptedSeed(originalSeed, chunkX, chunkZ, dimension, type, gameTime);
    }

    public boolean isSecureSeedEnabled() {
        return config != null && config.isSeedEncryptionEnabled() &&
               "secure".equalsIgnoreCase(config.getSeedEncryptionScheme());
    }

    public long[] getSecureSeedWorldSeed() {
        return org.virgil.akiasync.mixin.crypto.secureseed.crypto.Globals.worldSeed;
    }

    public void initializeSecureSeed(long originalSeed) {
        if (!isSecureSeedEnabled()) {
            return;
        }

        int bits = getSecureSeedBits();
        plugin.getLogger().info("[AkiAsync-SecureSeed] Initializing with " + bits + " bits");

        org.virgil.akiasync.mixin.crypto.secureseed.crypto.Globals.initializeWorldSeed(
            originalSeed,
            bits
        );

        plugin.getLogger().info("[AkiAsync-SecureSeed] Seed encryption initialized");
    }

    public int getSecureSeedBits() {
        return config != null ? config.getSecureSeedBits() : 1024;
    }

    public boolean isSeedEncryptionProtectStructures() {
        return config != null && config.isSeedEncryptionProtectStructures();
    }

    public boolean isSeedEncryptionProtectOres() {
        return config != null && config.isSeedEncryptionProtectOres();
    }

    public boolean isSeedEncryptionProtectSlimes() {
        return config != null && config.isSeedEncryptionProtectSlimes();
    }

    public boolean isSeedEncryptionProtectBiomes() {
        return config != null && config.isSeedEncryptionProtectBiomes();
    }
}
