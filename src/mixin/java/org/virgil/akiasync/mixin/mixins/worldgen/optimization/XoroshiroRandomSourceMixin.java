package org.virgil.akiasync.mixin.mixins.worldgen.optimization;

import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.levelgen.Xoroshiro128PlusPlus;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Optimizes XoroshiroRandomSource with batch random number generation.
 * Pre-generates random numbers in batches to reduce per-call overhead.
 */
@Mixin(XoroshiroRandomSource.class)
public abstract class XoroshiroRandomSourceMixin {

    @Shadow
    private Xoroshiro128PlusPlus randomNumberGenerator;

    @Unique
    private static final int BATCH_SIZE = 64;

    @Unique
    private long[] akiasync$longPool;
    @Unique
    private int akiasync$longIndex = BATCH_SIZE;

    @Unique
    private int[] akiasync$intPool;
    @Unique
    private int akiasync$intIndex = BATCH_SIZE;

    @Unique
    private double[] akiasync$doublePool;
    @Unique
    private int akiasync$doubleIndex = BATCH_SIZE;

    @Unique
    private float[] akiasync$floatPool;
    @Unique
    private int akiasync$floatIndex = BATCH_SIZE;

    @Unique
    private static volatile boolean akiasync$initialized = false;
    @Unique
    private static volatile boolean akiasync$batchEnabled = false;

    @Unique
    private static synchronized void akiasync$initConfig() {
        if (akiasync$initialized) return;
        try {
            org.virgil.akiasync.mixin.bridge.Bridge bridge =
                org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
            if (bridge != null) {
                akiasync$batchEnabled = bridge.isNoiseOptimizationEnabled();
                bridge.debugLog("[XoroshiroRandomSource] Batch mode: %s", akiasync$batchEnabled);
                akiasync$initialized = true;
            }
        } catch (Exception e) {
            akiasync$batchEnabled = false;
            akiasync$initialized = true;
        }
    }

    @Unique
    private void akiasync$ensurePoolsInitialized() {
        if (this.akiasync$longPool == null) {
            this.akiasync$longPool = new long[BATCH_SIZE];
            this.akiasync$intPool = new int[BATCH_SIZE];
            this.akiasync$doublePool = new double[BATCH_SIZE];
            this.akiasync$floatPool = new float[BATCH_SIZE];
        }
    }

    @Unique
    private void akiasync$refillLongPool() {
        akiasync$ensurePoolsInitialized();
        for (int i = 0; i < BATCH_SIZE; i++) {
            this.akiasync$longPool[i] = this.randomNumberGenerator.nextLong();
        }
        this.akiasync$longIndex = 0;
    }

    @Unique
    private void akiasync$refillIntPool() {
        akiasync$ensurePoolsInitialized();
        for (int i = 0; i < BATCH_SIZE; i++) {
            this.akiasync$intPool[i] = (int) this.randomNumberGenerator.nextLong();
        }
        this.akiasync$intIndex = 0;
    }

    @Unique
    private void akiasync$refillDoublePool() {
        akiasync$ensurePoolsInitialized();
        for (int i = 0; i < BATCH_SIZE; i++) {
            long bits = this.randomNumberGenerator.nextLong() >>> 11;
            this.akiasync$doublePool[i] = bits * 1.1102230246251565E-16;
        }
        this.akiasync$doubleIndex = 0;
    }

    @Unique
    private void akiasync$refillFloatPool() {
        akiasync$ensurePoolsInitialized();
        for (int i = 0; i < BATCH_SIZE; i++) {
            int bits = (int) (this.randomNumberGenerator.nextLong() >>> 40);
            this.akiasync$floatPool[i] = bits * 5.9604645E-8F;
        }
        this.akiasync$floatIndex = 0;
    }

    @Inject(method = "nextLong", at = @At("HEAD"), cancellable = true)
    private void akiasync$batchNextLong(CallbackInfoReturnable<Long> cir) {
        if (!akiasync$initialized) {
            akiasync$initConfig();
        }
        if (!akiasync$batchEnabled) {
            return;
        }

        if (this.akiasync$longIndex >= BATCH_SIZE) {
            akiasync$refillLongPool();
        }
        cir.setReturnValue(this.akiasync$longPool[this.akiasync$longIndex++]);
    }

    @Inject(method = "nextInt()I", at = @At("HEAD"), cancellable = true)
    private void akiasync$batchNextInt(CallbackInfoReturnable<Integer> cir) {
        if (!akiasync$initialized) {
            akiasync$initConfig();
        }
        if (!akiasync$batchEnabled) {
            return;
        }

        if (this.akiasync$intIndex >= BATCH_SIZE) {
            akiasync$refillIntPool();
        }
        cir.setReturnValue(this.akiasync$intPool[this.akiasync$intIndex++]);
    }

    @Inject(method = "nextDouble", at = @At("HEAD"), cancellable = true)
    private void akiasync$batchNextDouble(CallbackInfoReturnable<Double> cir) {
        if (!akiasync$initialized) {
            akiasync$initConfig();
        }
        if (!akiasync$batchEnabled) {
            return;
        }

        if (this.akiasync$doubleIndex >= BATCH_SIZE) {
            akiasync$refillDoublePool();
        }
        cir.setReturnValue(this.akiasync$doublePool[this.akiasync$doubleIndex++]);
    }

    @Inject(method = "nextFloat", at = @At("HEAD"), cancellable = true)
    private void akiasync$batchNextFloat(CallbackInfoReturnable<Float> cir) {
        if (!akiasync$initialized) {
            akiasync$initConfig();
        }
        if (!akiasync$batchEnabled) {
            return;
        }

        if (this.akiasync$floatIndex >= BATCH_SIZE) {
            akiasync$refillFloatPool();
        }
        cir.setReturnValue(this.akiasync$floatPool[this.akiasync$floatIndex++]);
    }

    @Inject(method = "setSeed", at = @At("TAIL"))
    private void akiasync$resetPoolsOnSetSeed(long seed, CallbackInfo ci) {
        this.akiasync$longIndex = BATCH_SIZE;
        this.akiasync$intIndex = BATCH_SIZE;
        this.akiasync$doubleIndex = BATCH_SIZE;
        this.akiasync$floatIndex = BATCH_SIZE;
    }
}
