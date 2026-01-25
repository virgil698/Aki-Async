package org.virgil.akiasync.network;

import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

public class AdvancedNetworkOptimization {

    private static final Logger LOGGER = Logger.getLogger("AkiAsync-AdvancedNetwork");

    private static volatile boolean initialized = false;
    private static volatile boolean enabled = false;

    private static volatile int mtuSize = 1460;
    private static volatile int safetyMargin = 64;
    private static volatile boolean mtuAwareEnabled = true;

    private static volatile boolean flushConsolidationEnabled = true;

    private static volatile boolean nativeCompressionEnabled = true;
    private static volatile int compressionLevel = 6;
    private static volatile int compressionThreshold = 256;

    private static volatile boolean nativeEncryptionEnabled = true;

    private static volatile boolean explosionOptimizationEnabled = true;
    private static volatile int explosionBlockThreshold = 512;

    private static final AtomicLong totalPacketsSent = new AtomicLong(0);
    private static final AtomicLong batchedPackets = new AtomicLong(0);
    private static final AtomicLong flushCount = new AtomicLong(0);
    private static final AtomicLong mtuFlushes = new AtomicLong(0);
    private static final AtomicLong tickFlushes = new AtomicLong(0);
    private static final AtomicLong explosionOptimizations = new AtomicLong(0);
    private static final AtomicLong savedBlockUpdates = new AtomicLong(0);

    public static synchronized void initialize(
            boolean enable,
            boolean enableMtuAware,
            int mtu,
            int safety,
            boolean enableFlushConsolidation,
            boolean enableNativeCompression,
            int compLevel,
            int compThreshold,
            boolean enableNativeEncryption,
            boolean enableExplosionOpt,
            int explosionThreshold
    ) {
        if (initialized) {
            LOGGER.info("[AdvancedNetworkOptimization] Already initialized");
            return;
        }

        enabled = enable;
        mtuAwareEnabled = enableMtuAware;
        mtuSize = mtu;
        safetyMargin = safety;
        flushConsolidationEnabled = enableFlushConsolidation;
        nativeCompressionEnabled = enableNativeCompression;
        compressionLevel = compLevel;
        compressionThreshold = compThreshold;
        nativeEncryptionEnabled = enableNativeEncryption;
        explosionOptimizationEnabled = enableExplosionOpt;
        explosionBlockThreshold = explosionThreshold;

        initialized = true;

        LOGGER.info(String.format(
            "[AdvancedNetworkOptimization] Initialized: enabled=%s, mtuAware=%s (mtu=%d), flushConsolidation=%s, nativeCompression=%s, nativeEncryption=%s, explosionOpt=%s",
            enabled, mtuAwareEnabled, mtuSize, flushConsolidationEnabled, nativeCompressionEnabled, nativeEncryptionEnabled, explosionOptimizationEnabled
        ));
    }

    public static void shutdown() {
        if (!initialized) {
            return;
        }

        LOGGER.info(String.format(
            "[AdvancedNetworkOptimization] Shutdown - Stats: totalPackets=%d, batched=%d, flushes=%d (mtu=%d, tick=%d), explosionOpts=%d, savedBlockUpdates=%d",
            totalPacketsSent.get(), batchedPackets.get(), flushCount.get(), mtuFlushes.get(), tickFlushes.get(),
            explosionOptimizations.get(), savedBlockUpdates.get()
        ));

        resetStatistics();
        initialized = false;
        enabled = false;
    }

    public static void reload(
            boolean enable,
            boolean enableMtuAware,
            int mtu,
            int safety,
            boolean enableFlushConsolidation,
            boolean enableNativeCompression,
            int compLevel,
            int compThreshold,
            boolean enableNativeEncryption,
            boolean enableExplosionOpt,
            int explosionThreshold
    ) {
        enabled = enable;
        mtuAwareEnabled = enableMtuAware;
        mtuSize = mtu;
        safetyMargin = safety;
        flushConsolidationEnabled = enableFlushConsolidation;
        nativeCompressionEnabled = enableNativeCompression;
        compressionLevel = compLevel;
        compressionThreshold = compThreshold;
        nativeEncryptionEnabled = enableNativeEncryption;
        explosionOptimizationEnabled = enableExplosionOpt;
        explosionBlockThreshold = explosionThreshold;

        LOGGER.info("[AdvancedNetworkOptimization] Configuration reloaded");
    }

    public static boolean isEnabled() {
        return initialized && enabled;
    }

    public static boolean isMtuAwareEnabled() {
        return isEnabled() && mtuAwareEnabled;
    }

    public static int getMtuSize() {
        return mtuSize;
    }

    public static int getSafetyMargin() {
        return safetyMargin;
    }

    public static int getEffectiveMtuLimit() {
        return mtuSize - safetyMargin;
    }

    public static boolean isFlushConsolidationEnabled() {
        return isEnabled() && flushConsolidationEnabled;
    }

    public static boolean isNativeCompressionEnabled() {
        return isEnabled() && nativeCompressionEnabled;
    }

    public static int getCompressionLevel() {
        return compressionLevel;
    }

    public static int getCompressionThreshold() {
        return compressionThreshold;
    }

    public static boolean isNativeEncryptionEnabled() {
        return isEnabled() && nativeEncryptionEnabled;
    }

    public static boolean isExplosionOptimizationEnabled() {
        return isEnabled() && explosionOptimizationEnabled;
    }

    public static int getExplosionBlockThreshold() {
        return explosionBlockThreshold;
    }

    public static void recordPacketSent() {
        totalPacketsSent.incrementAndGet();
    }

    public static void recordBatchedPacket() {
        batchedPackets.incrementAndGet();
    }

    public static void recordFlush(boolean isMtuFlush) {
        flushCount.incrementAndGet();
        if (isMtuFlush) {
            mtuFlushes.incrementAndGet();
        } else {
            tickFlushes.incrementAndGet();
        }
    }

    public static void recordExplosionOptimization(int savedUpdates) {
        explosionOptimizations.incrementAndGet();
        savedBlockUpdates.addAndGet(savedUpdates);
    }

    public static void resetStatistics() {
        totalPacketsSent.set(0);
        batchedPackets.set(0);
        flushCount.set(0);
        mtuFlushes.set(0);
        tickFlushes.set(0);
        explosionOptimizations.set(0);
        savedBlockUpdates.set(0);
    }

    public static String getStatistics() {
        if (!initialized) {
            return "AdvancedNetworkOptimization: Not initialized";
        }

        long total = totalPacketsSent.get();
        long batched = batchedPackets.get();
        long flushes = flushCount.get();
        long mtuF = mtuFlushes.get();
        long tickF = tickFlushes.get();
        long expOpts = explosionOptimizations.get();
        long savedBlocks = savedBlockUpdates.get();

        double batchRate = total > 0 ? (double) batched / total * 100 : 0;
        double avgBatchSize = flushes > 0 ? (double) batched / flushes : 0;

        return String.format(
            "AdvancedNetworkOptimization: enabled=%s, packets=%d, batched=%d (%.1f%%), flushes=%d (mtu=%d, tick=%d), avgBatchSize=%.1f, explosionOpts=%d, savedBlockUpdates=%d",
            enabled, total, batched, batchRate, flushes, mtuF, tickF, avgBatchSize, expOpts, savedBlocks
        );
    }
}
