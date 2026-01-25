package org.virgil.akiasync.mixin.mixins.network.connection;

import net.minecraft.network.CompressionEncoder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.concurrent.atomic.AtomicLong;

@SuppressWarnings("unused")
@Mixin(CompressionEncoder.class)
public class VelocityNativeCompressionMixin {

    @Shadow
    private int threshold;

    @Unique
    private static volatile boolean enabled = true;
    @Unique
    private static volatile boolean initialized = false;
    @Unique
    private static volatile boolean nativeAvailable = false;
    @Unique
    private static volatile int compressionLevel = 6;

    @Unique
    private static final AtomicLong totalCompressed = new AtomicLong(0);
    @Unique
    private static final AtomicLong totalUncompressed = new AtomicLong(0);
    @Unique
    private static final AtomicLong bytesIn = new AtomicLong(0);
    @Unique
    private static final AtomicLong bytesOut = new AtomicLong(0);
    @Unique
    private static final AtomicLong nativeCompressions = new AtomicLong(0);
    @Unique
    private static final AtomicLong javaCompressions = new AtomicLong(0);

    @Unique
    private Object akiasync$nativeCompressor = null;

    @Unique
    private void akiasync$initNativeCompressor() {
        try {
            Class<?> nativesClass = Class.forName("com.velocitypowered.natives.util.Natives");
            Object compressFactory = nativesClass.getField("compress").get(null);
            Object factory = compressFactory.getClass().getMethod("get").invoke(compressFactory);
            akiasync$nativeCompressor = factory.getClass().getMethod("create", int.class).invoke(factory, compressionLevel);
            nativeAvailable = true;
        } catch (Exception e) {
            nativeAvailable = false;
            akiasync$nativeCompressor = null;
        }
    }

    @Unique
    private static synchronized void akiasync$init() {
        if (initialized) {
            return;
        }

        try {
            org.virgil.akiasync.mixin.bridge.Bridge bridge =
                org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();

            if (bridge != null) {
                enabled = bridge.isNativeCompressionEnabled();
                compressionLevel = bridge.getNativeCompressionLevel();

                nativeAvailable = akiasync$checkNativeAvailability();

                bridge.debugLog("[VelocityNativeCompression] Initialized: enabled=%s, nativeAvailable=%s, level=%d",
                    enabled, nativeAvailable, compressionLevel);

                initialized = true;
            }
        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "VelocityNativeCompression", "init", e);
        }
    }

    @Unique
    private static boolean akiasync$checkNativeAvailability() {
        try {
            Class<?> nativesClass = Class.forName("com.velocitypowered.natives.util.Natives");
            Object compressFactory = nativesClass.getField("compress").get(null);
            String variant = (String) compressFactory.getClass().getMethod("getLoadedVariant").invoke(compressFactory);
            return variant != null && !variant.contains("Java");
        } catch (Exception e) {
            return false;
        }
    }

    @Unique
    private static String akiasync$getNativeVariant() {
        try {
            Class<?> nativesClass = Class.forName("com.velocitypowered.natives.util.Natives");
            Object compressFactory = nativesClass.getField("compress").get(null);
            return (String) compressFactory.getClass().getMethod("getLoadedVariant").invoke(compressFactory);
        } catch (Exception e) {
            return "Not available";
        }
    }

    @Unique
    private static void akiasync$recordCompression(int inputSize, int outputSize, boolean usedNative) {
        totalCompressed.incrementAndGet();
        bytesIn.addAndGet(inputSize);
        bytesOut.addAndGet(outputSize);
        if (usedNative) {
            nativeCompressions.incrementAndGet();
        } else {
            javaCompressions.incrementAndGet();
        }
    }

    @Unique
    private static void akiasync$recordUncompressed() {
        totalUncompressed.incrementAndGet();
    }

    @Unique
    private static boolean akiasync$isEnabled() {
        if (!initialized) {
            akiasync$init();
        }
        return enabled && nativeAvailable;
    }

    @Unique
    private static boolean akiasync$isNativeAvailable() {
        return nativeAvailable;
    }

    @Unique
    private static String akiasync$getStatistics() {
        long compressed = totalCompressed.get();
        long uncompressed = totalUncompressed.get();
        long in = bytesIn.get();
        long out = bytesOut.get();
        long nativeC = nativeCompressions.get();
        long javaC = javaCompressions.get();

        double ratio = in > 0 ? (1.0 - (double) out / in) * 100 : 0;
        double nativeRate = compressed > 0 ? (double) nativeC / compressed * 100 : 0;

        return String.format(
            "VelocityNativeCompression: enabled=%s, native=%s, variant=%s, compressed=%d, uncompressed=%d, ratio=%.1f%%, nativeRate=%.1f%%",
            enabled, nativeAvailable, akiasync$getNativeVariant(), compressed, uncompressed, ratio, nativeRate
        );
    }

    @Unique
    private static void akiasync$resetStatistics() {
        totalCompressed.set(0);
        totalUncompressed.set(0);
        bytesIn.set(0);
        bytesOut.set(0);
        nativeCompressions.set(0);
        javaCompressions.set(0);
    }

    @Unique
    private static void akiasync$reload() {
        initialized = false;
    }
}
