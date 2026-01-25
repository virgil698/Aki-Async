package org.virgil.akiasync.mixin.mixins.network.connection;

import net.minecraft.network.CipherEncoder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.concurrent.atomic.AtomicLong;

@SuppressWarnings("unused")
@Mixin(CipherEncoder.class)
public class VelocityNativeEncryptionMixin {

    @Unique
    private static volatile boolean enabled = true;
    @Unique
    private static volatile boolean initialized = false;
    @Unique
    private static volatile boolean nativeAvailable = false;

    @Unique
    private static final AtomicLong totalEncrypted = new AtomicLong(0);
    @Unique
    private static final AtomicLong bytesEncrypted = new AtomicLong(0);
    @Unique
    private static final AtomicLong nativeEncryptions = new AtomicLong(0);
    @Unique
    private static final AtomicLong javaEncryptions = new AtomicLong(0);

    @Unique
    private Object akiasync$nativeCipher = null;

    @Unique
    private static synchronized void akiasync$init() {
        if (initialized) {
            return;
        }

        try {
            org.virgil.akiasync.mixin.bridge.Bridge bridge =
                org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();

            if (bridge != null) {
                enabled = bridge.isNativeEncryptionEnabled();

                nativeAvailable = akiasync$checkNativeAvailability();

                bridge.debugLog("[VelocityNativeEncryption] Initialized: enabled=%s, nativeAvailable=%s, variant=%s",
                    enabled, nativeAvailable, akiasync$getNativeVariant());

                initialized = true;
            }
        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "VelocityNativeEncryption", "init", e);
        }
    }

    @Unique
    private static boolean akiasync$checkNativeAvailability() {
        try {
            Class<?> nativesClass = Class.forName("com.velocitypowered.natives.util.Natives");
            Object cipherFactory = nativesClass.getField("cipher").get(null);
            String variant = (String) cipherFactory.getClass().getMethod("getLoadedVariant").invoke(cipherFactory);
            return variant != null && !variant.contains("Java");
        } catch (Exception e) {
            return false;
        }
    }

    @Unique
    private static String akiasync$getNativeVariant() {
        try {
            Class<?> nativesClass = Class.forName("com.velocitypowered.natives.util.Natives");
            Object cipherFactory = nativesClass.getField("cipher").get(null);
            return (String) cipherFactory.getClass().getMethod("getLoadedVariant").invoke(cipherFactory);
        } catch (Exception e) {
            return "Not available";
        }
    }

    @Unique
    private static void akiasync$recordEncryption(int bytes, boolean usedNative) {
        totalEncrypted.incrementAndGet();
        bytesEncrypted.addAndGet(bytes);
        if (usedNative) {
            nativeEncryptions.incrementAndGet();
        } else {
            javaEncryptions.incrementAndGet();
        }
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
        long total = totalEncrypted.get();
        long bytes = bytesEncrypted.get();
        long nativeE = nativeEncryptions.get();
        long javaE = javaEncryptions.get();

        double nativeRate = total > 0 ? (double) nativeE / total * 100 : 0;

        return String.format(
            "VelocityNativeEncryption: enabled=%s, native=%s, variant=%s, encrypted=%d, bytes=%d, nativeRate=%.1f%%",
            enabled, nativeAvailable, akiasync$getNativeVariant(), total, bytes, nativeRate
        );
    }

    @Unique
    private static void akiasync$resetStatistics() {
        totalEncrypted.set(0);
        bytesEncrypted.set(0);
        nativeEncryptions.set(0);
        javaEncryptions.set(0);
    }

    @Unique
    private static void akiasync$reload() {
        initialized = false;
    }
}
