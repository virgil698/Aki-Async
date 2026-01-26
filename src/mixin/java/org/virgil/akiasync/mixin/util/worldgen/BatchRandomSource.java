package org.virgil.akiasync.mixin.util.worldgen;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Batch random number generator for worldgen optimization.
 * Pre-generates random numbers in batches to reduce per-call overhead.
 * Uses Xoroshiro128++ algorithm (same as MC).
 */
public class BatchRandomSource {

    private static final int DEFAULT_BATCH_SIZE = 256;

    private long seedLo;
    private long seedHi;

    // Pre-generated random number pool
    private final long[] longPool;
    private final int[] intPool;
    private final double[] doublePool;
    private final float[] floatPool;

    private int longIndex;
    private int intIndex;
    private int doubleIndex;
    private int floatIndex;

    private final int batchSize;

    public BatchRandomSource(long seed) {
        this(seed, DEFAULT_BATCH_SIZE);
    }

    public BatchRandomSource(long seed, int batchSize) {
        this.batchSize = batchSize;
        this.longPool = new long[batchSize];
        this.intPool = new int[batchSize];
        this.doublePool = new double[batchSize];
        this.floatPool = new float[batchSize];

        // Initialize seed using SplitMix64 (same as MC's RandomSupport.upgradeSeedTo128bit)
        long z = seed;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        this.seedLo = z ^ (z >>> 31);

        z = seed + 0x9E3779B97F4A7C15L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        this.seedHi = z ^ (z >>> 31);

        // Handle zero seed case
        if ((this.seedLo | this.seedHi) == 0L) {
            this.seedLo = -7046029254386353131L;
            this.seedHi = 7640891576956012809L;
        }

        // Pre-fill all pools
        refillAllPools();
    }

    public BatchRandomSource(long seedLo, long seedHi) {
        this(seedLo, seedHi, DEFAULT_BATCH_SIZE);
    }

    public BatchRandomSource(long seedLo, long seedHi, int batchSize) {
        this.batchSize = batchSize;
        this.longPool = new long[batchSize];
        this.intPool = new int[batchSize];
        this.doublePool = new double[batchSize];
        this.floatPool = new float[batchSize];

        this.seedLo = seedLo;
        this.seedHi = seedHi;

        if ((this.seedLo | this.seedHi) == 0L) {
            this.seedLo = -7046029254386353131L;
            this.seedHi = 7640891576956012809L;
        }

        refillAllPools();
    }

    /**
     * Core Xoroshiro128++ next long implementation.
     */
    private long nextLongRaw() {
        long l = this.seedLo;
        long l1 = this.seedHi;
        long result = Long.rotateLeft(l + l1, 17) + l;
        l1 ^= l;
        this.seedLo = Long.rotateLeft(l, 49) ^ l1 ^ (l1 << 21);
        this.seedHi = Long.rotateLeft(l1, 28);
        return result;
    }

    private void refillAllPools() {
        refillLongPool();
        refillIntPool();
        refillDoublePool();
        refillFloatPool();
    }

    private void refillLongPool() {
        for (int i = 0; i < batchSize; i++) {
            longPool[i] = nextLongRaw();
        }
        longIndex = 0;
    }

    private void refillIntPool() {
        for (int i = 0; i < batchSize; i++) {
            intPool[i] = (int) nextLongRaw();
        }
        intIndex = 0;
    }

    private void refillDoublePool() {
        for (int i = 0; i < batchSize; i++) {
            long bits = nextLongRaw() >>> 11;
            doublePool[i] = bits * 1.1102230246251565E-16;
        }
        doubleIndex = 0;
    }

    private void refillFloatPool() {
        for (int i = 0; i < batchSize; i++) {
            int bits = (int) (nextLongRaw() >>> 40);
            floatPool[i] = bits * 5.9604645E-8F;
        }
        floatIndex = 0;
    }

    /**
     * Get next long from pool.
     */
    public long nextLong() {
        if (longIndex >= batchSize) {
            refillLongPool();
        }
        return longPool[longIndex++];
    }

    /**
     * Get next int from pool.
     */
    public int nextInt() {
        if (intIndex >= batchSize) {
            refillIntPool();
        }
        return intPool[intIndex++];
    }

    /**
     * Get next int with bound.
     */
    public int nextInt(int bound) {
        if (bound <= 0) {
            throw new IllegalArgumentException("Bound must be positive");
        }
        long l = Integer.toUnsignedLong(nextInt());
        long l1 = l * bound;
        long l2 = l1 & 4294967295L;
        if (l2 < bound) {
            for (int i = Integer.remainderUnsigned(~bound + 1, bound); l2 < i; l2 = l1 & 4294967295L) {
                l = Integer.toUnsignedLong(nextInt());
                l1 = l * bound;
            }
        }
        return (int) (l1 >> 32);
    }

    /**
     * Get next double from pool.
     */
    public double nextDouble() {
        if (doubleIndex >= batchSize) {
            refillDoublePool();
        }
        return doublePool[doubleIndex++];
    }

    /**
     * Get next float from pool.
     */
    public float nextFloat() {
        if (floatIndex >= batchSize) {
            refillFloatPool();
        }
        return floatPool[floatIndex++];
    }

    /**
     * Get next boolean.
     */
    public boolean nextBoolean() {
        return (nextLong() & 1L) != 0L;
    }

    /**
     * Fill an array with random longs.
     * More efficient than calling nextLong() repeatedly.
     */
    public void fillLongs(long[] array) {
        fillLongs(array, 0, array.length);
    }

    public void fillLongs(long[] array, int offset, int length) {
        int remaining = length;
        int pos = offset;

        while (remaining > 0) {
            int available = batchSize - longIndex;
            if (available <= 0) {
                refillLongPool();
                available = batchSize;
            }

            int toCopy = Math.min(remaining, available);
            System.arraycopy(longPool, longIndex, array, pos, toCopy);
            longIndex += toCopy;
            pos += toCopy;
            remaining -= toCopy;
        }
    }

    /**
     * Fill an array with random doubles.
     */
    public void fillDoubles(double[] array) {
        fillDoubles(array, 0, array.length);
    }

    public void fillDoubles(double[] array, int offset, int length) {
        int remaining = length;
        int pos = offset;

        while (remaining > 0) {
            int available = batchSize - doubleIndex;
            if (available <= 0) {
                refillDoublePool();
                available = batchSize;
            }

            int toCopy = Math.min(remaining, available);
            System.arraycopy(doublePool, doubleIndex, array, pos, toCopy);
            doubleIndex += toCopy;
            pos += toCopy;
            remaining -= toCopy;
        }
    }

    /**
     * Create a fork with new seed derived from current state.
     */
    public BatchRandomSource fork() {
        return new BatchRandomSource(nextLong(), nextLong(), batchSize);
    }

    /**
     * Reset with new seed.
     */
    public void setSeed(long seed) {
        long z = seed;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        this.seedLo = z ^ (z >>> 31);

        z = seed + 0x9E3779B97F4A7C15L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        this.seedHi = z ^ (z >>> 31);

        if ((this.seedLo | this.seedHi) == 0L) {
            this.seedLo = -7046029254386353131L;
            this.seedHi = 7640891576956012809L;
        }

        refillAllPools();
    }

    public int getBatchSize() {
        return batchSize;
    }

    public static String getOptimizationInfo() {
        return String.format("BatchRandomSource: Xoroshiro128++ with batch size %d", DEFAULT_BATCH_SIZE);
    }
}
