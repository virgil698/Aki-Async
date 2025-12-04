package org.virgil.akiasync.mixin.crypto.secureseed.crypto;

import com.google.common.collect.Iterables;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerLevel;
import org.virgil.akiasync.mixin.crypto.secureseed.duck.IWorldOptionsFeatureSeed;

public final class Globals {
  
  public static final int WORLD_SEED_LONGS = 16;
  
  public static final int WORLD_SEED_BITS = WORLD_SEED_LONGS * 64;
  
  private static final int BITS_PER_LONG = 64;
  
  private static final int MAX_CACHE_SIZE = 100;
  
  public static final long[] worldSeed = new long[WORLD_SEED_LONGS];
  
  public static final ThreadLocal<Integer> dimension = ThreadLocal.withInitial(() -> 0);
  
  private static final ConcurrentHashMap<String, long[]> seedCache = new ConcurrentHashMap<>();
  
  private static final SecureRandom secureRandom = new SecureRandom();

  public enum Salt {
    UNDEFINED,
    BASTION_FEATURE,
    WOODLAND_MANSION_FEATURE,
    MINESHAFT_FEATURE,
    BURIED_TREASURE_FEATURE,
    NETHER_FORTRESS_FEATURE,
    PILLAGER_OUTPOST_FEATURE,
    GEODE_FEATURE,
    NETHER_FOSSIL_FEATURE,
    OCEAN_MONUMENT_FEATURE,
    RUINED_PORTAL_FEATURE,
    POTENTIAL_FEATURE,
    GENERATE_FEATURE,
    JIGSAW_PLACEMENT,
    STRONGHOLDS,
    POPULATION,
    DECORATION,
    SLIME_CHUNK
  }

  private Globals() {
    throw new AssertionError("Utility class should not be instantiated");
  }

  /**
   * 设置全局种子和维度信息
   * 
   * Folia兼容：此方法在世界生成线程调用，无需特殊处理
   * 
   * @param world 服务器世界实例
   */
  public static void setupGlobals(ServerLevel world) {
    // 获取特征种子（Folia安全：getServer()在任何线程都可调用）
    long[] seed = ((IWorldOptionsFeatureSeed) world.getServer()
        .getWorldData()
        .worldGenOptions())
        .secureSeed$featureSeed();
    
    // 复制种子到全局数组（线程安全：每个线程有自己的ThreadLocal）
    System.arraycopy(seed, 0, worldSeed, 0, WORLD_SEED_LONGS);
    
    // 获取维度索引（Folia安全：levelKeys()是不可变集合）
    int worldIndex = Iterables.indexOf(
        world.getServer().levelKeys(),
        key -> key == world.dimension()
    );
    
    if (worldIndex == -1) {
      worldIndex = world.getServer().levelKeys().size();
    }
    
    // 设置维度（线程安全：ThreadLocal隔离）
    dimension.set(worldIndex);
  }

  public static long[] createRandomWorldSeed() {
    long[] seed = new long[WORLD_SEED_LONGS];
    for (int i = 0; i < WORLD_SEED_LONGS; i++) {
      seed[i] = secureRandom.nextLong();
    }
    return seed;
  }
  
  /**
   * 初始化世界种子（从原始种子生成）
   * @param originalSeed 原始Minecraft种子
   * @param bits 种子位数（通常是1024）
   */
  public static void initializeWorldSeed(long originalSeed, int bits) {
    // 使用原始种子作为SecureRandom的种子
    SecureRandom random = new SecureRandom();
    random.setSeed(originalSeed);
    
    // 生成指定位数的种子
    for (int i = 0; i < WORLD_SEED_LONGS; i++) {
      worldSeed[i] = random.nextLong();
    }
  }

  public static Optional<long[]> parseSeed(String seedString) {
    if (seedString.isEmpty()) {
      return Optional.empty();
    }

    long[] cached = seedCache.get(seedString);
    if (cached != null) {
      return Optional.of(cached.clone());
    }

    if (seedString.length() != WORLD_SEED_BITS) {
      throw new IllegalArgumentException(
          String.format(
              "Secure seed must be %d bits, but found %d bits",
              WORLD_SEED_BITS,
              seedString.length()
          )
      );
    }

    long[] seed = new long[WORLD_SEED_LONGS];
    for (int i = 0; i < WORLD_SEED_LONGS; i++) {
      int startIndex = i * BITS_PER_LONG;
      int endIndex = startIndex + BITS_PER_LONG;
      String seedSection = seedString.substring(startIndex, endIndex);
      
      BigInteger seedValue = new BigInteger(seedSection, 2);
      seed[i] = seedValue.longValue();
    }

    if (seedCache.size() < MAX_CACHE_SIZE) {
      seedCache.put(seedString, seed.clone());
    }

    return Optional.of(seed);
  }

  public static String seedToString(long[] seed) {
    StringBuilder builder = new StringBuilder(WORLD_SEED_BITS);
    
    for (long value : seed) {
      String binaryString = String.format(
          "%64s",
          Long.toBinaryString(value)
      ).replace(' ', '0');
      
      builder.append(binaryString);
    }
    
    return builder.toString();
  }

  public static void clearCache() {
    seedCache.clear();
  }
}
