package org.virgil.akiasync.crypto;

import org.bukkit.plugin.Plugin;
import org.virgil.akiasync.mixin.crypto.quantum.QuantumSeedCache;
import org.virgil.akiasync.mixin.crypto.quantum.QuantumSeedCore;
import org.virgil.akiasync.mixin.crypto.quantum.GenerationType;

import java.util.logging.Logger;

public class QuantumSeedManager {

    private static final Logger LOGGER = Logger.getLogger("AkiAsync-QuantumSeed");

    private final ServerKeyManager keyManager;
    private final QuantumSeedCache cache;
    private volatile boolean enableTimeDecay;
    private volatile boolean debugLogging;

    public QuantumSeedManager(Plugin plugin, int cacheSize, boolean enableTimeDecay, boolean debugLogging) {
        this.keyManager = new ServerKeyManager(plugin);
        this.cache = new QuantumSeedCache(cacheSize);
        this.enableTimeDecay = enableTimeDecay;
        this.debugLogging = debugLogging;
    }

    public void initialize() {
        keyManager.initialize();

        if (debugLogging) {
            LOGGER.info("[QuantumSeed] Manager initialized with cache");
        }

        try {
            org.virgil.akiasync.mixin.crypto.quantum.AsyncSeedEncryptor.preheatCache(
                cache,
                keyManager.getServerKey(),
                0
            );
            if (debugLogging) {
                LOGGER.info("[QuantumSeed] Cache preheating started in background");
            }
        } catch (Exception e) {
            LOGGER.warning("[QuantumSeed] Failed to start cache preheating: " + e.getMessage());
        }
    }

    public long getEncryptedSeed(
        long originalSeed,
        int chunkX,
        int chunkZ,
        String dimension,
        GenerationType type,
        long gameTime
    ) {
        Long cached = cache.get(originalSeed, chunkX, chunkZ, dimension, type);
        if (cached != null) {
            if (debugLogging) {
                LOGGER.fine(String.format("[QuantumSeed] Cache hit for chunk (%d, %d) in %s", chunkX, chunkZ, dimension));
            }
            return cached;
        }

        long startTime = debugLogging ? System.nanoTime() : 0;

        long encrypted = QuantumSeedCore.encrypt(
            originalSeed,
            chunkX,
            chunkZ,
            dimension,
            type,
            keyManager.getServerKey(),
            gameTime,
            enableTimeDecay
        );

        if (debugLogging) {
            long elapsed = System.nanoTime() - startTime;
            LOGGER.fine(String.format("[QuantumSeed] Encrypted seed for chunk (%d, %d) in %s: %d ns, type=%s",
                chunkX, chunkZ, dimension, elapsed, type));
        }

        cache.put(originalSeed, chunkX, chunkZ, dimension, type, encrypted);

        return encrypted;
    }

    public long getEncryptedSeed(
        long originalSeed,
        int chunkX,
        int chunkZ,
        String dimension,
        GenerationType type
    ) {
        return getEncryptedSeed(originalSeed, chunkX, chunkZ, dimension, type, 0);
    }

    public void clearCache() {
        cache.clear();
        LOGGER.info("[QuantumSeed] Cache cleared");
    }

    public QuantumSeedCache.CacheStats getCacheStats() {
        return cache.getStats();
    }

    public void printCacheStats() {
        QuantumSeedCache.CacheStats stats = getCacheStats();
        LOGGER.info("[QuantumSeed] " + stats.toString());
    }

    public void updateConfig(boolean enableTimeDecay, boolean debugLogging) {
        this.enableTimeDecay = enableTimeDecay;
        this.debugLogging = debugLogging;

        if (debugLogging) {
            LOGGER.info("[QuantumSeed] Config updated: timeDecay=" + enableTimeDecay + ", debug=" + debugLogging);
        }
    }

    public void regenerateServerKey() throws Exception {
        keyManager.regenerateKey();
        clearCache();
        LOGGER.warning("[QuantumSeed] Server key regenerated - world generation will change!");
    }

    public boolean isInitialized() {
        return keyManager.isInitialized();
    }
}
