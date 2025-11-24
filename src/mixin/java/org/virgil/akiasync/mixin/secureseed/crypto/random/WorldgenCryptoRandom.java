package org.virgil.akiasync.mixin.secureseed.crypto.random;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import org.virgil.akiasync.mixin.secureseed.crypto.Globals;
import org.virgil.akiasync.mixin.secureseed.crypto.Hashing;

public class WorldgenCryptoRandom extends WorldgenRandom {
    private static final int MAX_RANDOM_BIT_INDEX = 512;
    private static final ThreadLocal<long[]> THREAD_LOCAL_BUFFER = ThreadLocal.withInitial(() -> new long[16]);
    
    private final long[] randomBits = new long[8];
    private int randomBitIndex = MAX_RANDOM_BIT_INDEX;

    public WorldgenCryptoRandom(int x, int z, Globals.Salt salt, long extra) {
        super(new DummyRandomSource());
        setSecureSeed(x, z, salt, extra);
    }

    private void setSecureSeed(int x, int z, Globals.Salt salt, long extra) {
        long[] message = THREAD_LOCAL_BUFFER.get();
        
        System.arraycopy(Globals.worldSeed, 0, message, 0, Globals.WORLD_SEED_LONGS);
        
        message[Globals.WORLD_SEED_LONGS] = ((long) x << 32) | (z & 0xffffffffL);
        message[Globals.WORLD_SEED_LONGS + 1] = salt.ordinal();
        message[Globals.WORLD_SEED_LONGS + 2] = extra;
        message[Globals.WORLD_SEED_LONGS + 3] = Globals.dimension.get();
        
        Hashing.hash(message, randomBits, new long[16], 0, false);
        randomBitIndex = 0;
    }

    private long getBits(int bits) {
        if (bits <= 0 || bits > 64) {
            throw new IllegalArgumentException("Bits must be between 1 and 64");
        }

        long result = 0;
        int bitsRemaining = bits;

        while (bitsRemaining > 0) {
            if (randomBitIndex >= MAX_RANDOM_BIT_INDEX) {
                refillRandomBits();
            }

            int bitsToTake = Math.min(bitsRemaining, 64);
            int longIndex = randomBitIndex / 64;
            int bitOffset = randomBitIndex % 64;

            if (bitOffset + bitsToTake <= 64) {
                long mask = (1L << bitsToTake) - 1;
                result = (result << bitsToTake) | ((randomBits[longIndex] >>> bitOffset) & mask);
                randomBitIndex += bitsToTake;
                bitsRemaining -= bitsToTake;
            } else {
                int firstBits = 64 - bitOffset;
                long mask1 = (1L << firstBits) - 1;
                result = (result << firstBits) | ((randomBits[longIndex] >>> bitOffset) & mask1);
                randomBitIndex += firstBits;
                bitsRemaining -= firstBits;
            }
        }

        return result;
    }

    private void refillRandomBits() {
        long[] internalState = new long[16];
        Hashing.hash(randomBits, randomBits, internalState, randomBitIndex / MAX_RANDOM_BIT_INDEX, false);
        randomBitIndex = 0;
    }

    @Override
    public int nextInt() {
        return (int) getBits(32);
    }

    @Override
    public int nextInt(int bound) {
        if (bound <= 0) {
            throw new IllegalArgumentException("bound must be positive");
        }
        
        int bits = Mth.ceillog2(bound);
        int result;
        do {
            result = (int) getBits(bits);
        } while (result >= bound);

        return result;
    }

    @Override
    public long nextLong() {
        return getBits(64);
    }

    @Override
    public boolean nextBoolean() {
        return getBits(1) != 0;
    }

    @Override
    public float nextFloat() {
        return (float) (getBits(24) * 0x1.0p-24);
    }

    @Override
    public double nextDouble() {
        return getBits(53) * 0x1.0p-53;
    }

    @Override
    public long setDecorationSeed(long worldSeed, int blockX, int blockZ) {
        setSecureSeed(blockX, blockZ, Globals.Salt.POPULATION, 0);
        return ((long) blockX << 32) | ((long) blockZ & 0xffffffffL);
    }

    @Override
    public void setFeatureSeed(long populationSeed, int index, int step) {
        setSecureSeed((int) (populationSeed >> 32), (int) populationSeed, 
                     Globals.Salt.DECORATION, index + 10000L * step);
    }

    @Override
    public void setLargeFeatureSeed(long worldSeed, int chunkX, int chunkZ) {
        super.setLargeFeatureSeed(worldSeed, chunkX, chunkZ);
    }

    @Override
    public void setLargeFeatureWithSalt(long worldSeed, int regionX, int regionZ, int salt) {
        super.setLargeFeatureWithSalt(worldSeed, regionX, regionZ, salt);
    }

    public static RandomSource seedSlimeChunk(int chunkX, int chunkZ) {
        return new WorldgenCryptoRandom(chunkX, chunkZ, Globals.Salt.SLIME_CHUNK, 0);
    }

    private static class DummyRandomSource implements RandomSource {
        @Override
        public RandomSource fork() {
            return this;
        }

        @Override
        public net.minecraft.world.level.levelgen.PositionalRandomFactory forkPositional() {
            return null;
        }

        @Override
        public void setSeed(long seed) {
        }

        @Override
        public int nextInt() {
            return 0;
        }

        @Override
        public int nextInt(int bound) {
            return 0;
        }

        @Override
        public long nextLong() {
            return 0;
        }

        @Override
        public boolean nextBoolean() {
            return false;
        }

        @Override
        public float nextFloat() {
            return 0;
        }

        @Override
        public double nextDouble() {
            return 0;
        }

        @Override
        public double nextGaussian() {
            return 0;
        }
        
        @Override
        public void consumeCount(int count) {
        }
    }
}
