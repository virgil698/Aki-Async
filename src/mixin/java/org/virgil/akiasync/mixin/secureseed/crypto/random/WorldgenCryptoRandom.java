package org.virgil.akiasync.mixin.secureseed.crypto.random;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import org.virgil.akiasync.mixin.secureseed.crypto.Globals;
import org.virgil.akiasync.mixin.secureseed.crypto.Hashing;

public class WorldgenCryptoRandom extends WorldgenRandom {
  
  private static final int MAX_RANDOM_BIT_INDEX = 512;
  
  private static final int BITS_PER_LONG = 64;
  
  private static final int MAX_BITS = 64;
  
  private static final ThreadLocal<long[]> THREAD_LOCAL_BUFFER =
      ThreadLocal.withInitial(() -> new long[16]);

  private final long[] randomBits = new long[8];
  
  private int randomBitIndex = MAX_RANDOM_BIT_INDEX;

  public WorldgenCryptoRandom(int x, int z, Globals.Salt salt, long extra) {
    super(new DummyRandomSource());
    setSecureSeed(x, z, salt, extra);
  }

  private void setSecureSeed(int x, int z, Globals.Salt salt, long extra) {
    long[] message = THREAD_LOCAL_BUFFER.get();

    System.arraycopy(Globals.worldSeed, 0, message, 0, Globals.WORLD_SEED_LONGS);

    message[Globals.WORLD_SEED_LONGS] = packCoordinates(x, z);
    
    message[Globals.WORLD_SEED_LONGS + 1] = salt.ordinal();
    
    message[Globals.WORLD_SEED_LONGS + 2] = extra;
    
    message[Globals.WORLD_SEED_LONGS + 3] = Globals.dimension.get();

    Hashing.hash(message, randomBits, new long[16], 0, false);
    randomBitIndex = 0;
  }

  private long packCoordinates(int x, int z) {
    return ((long) x << 32) | (z & 0xffffffffL);
  }

  private long getBits(int bits) {
    if (bits <= 0 || bits > MAX_BITS) {
      throw new IllegalArgumentException(
          String.format("Bits must be between 1 and %d, got %d", MAX_BITS, bits)
      );
    }

    long result = 0;
    int bitsRemaining = bits;

    while (bitsRemaining > 0) {
      
      if (randomBitIndex >= MAX_RANDOM_BIT_INDEX) {
        refillRandomBits();
      }

      int longIndex = randomBitIndex / BITS_PER_LONG;
      int bitOffset = randomBitIndex % BITS_PER_LONG;
      int bitsAvailable = BITS_PER_LONG - bitOffset;
      int bitsToTake = Math.min(bitsRemaining, bitsAvailable);

      long mask = (1L << bitsToTake) - 1;
      long extractedBits = (randomBits[longIndex] >>> bitOffset) & mask;
      
      result = (result << bitsToTake) | extractedBits;
      randomBitIndex += bitsToTake;
      bitsRemaining -= bitsToTake;
    }

    return result;
  }

  private void refillRandomBits() {
    long[] internalState = new long[16];
    long messageOffset = randomBitIndex / MAX_RANDOM_BIT_INDEX;
    
    Hashing.hash(randomBits, randomBits, internalState, messageOffset, false);
    randomBitIndex = 0;
  }

  @Override
  public int nextInt() {
    return (int) getBits(32);
  }

  @Override
  public int nextInt(int bound) {
    if (bound <= 0) {
      throw new IllegalArgumentException("Bound must be positive, got: " + bound);
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
    return packCoordinates(blockX, blockZ);
  }

  @Override
  public void setFeatureSeed(long populationSeed, int index, int step) {
    int x = (int) (populationSeed >> 32);
    int z = (int) populationSeed;
    long extra = index + 10000L * step;
    
    setSecureSeed(x, z, Globals.Salt.DECORATION, extra);
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
      return 0.0f;
    }

    @Override
    public double nextDouble() {
      return 0.0;
    }

    @Override
    public double nextGaussian() {
      return 0.0;
    }

    @Override
    public void consumeCount(int count) {
      
    }
  }
}
