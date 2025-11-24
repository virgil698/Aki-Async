package org.virgil.akiasync.mixin.secureseed.crypto;

import com.google.common.collect.Iterables;
import org.virgil.akiasync.mixin.secureseed.duck.IWorldOptionsFeatureSeed;
import net.minecraft.server.level.ServerLevel;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class Globals {
    public static final int WORLD_SEED_LONGS = 16;
    public static final int WORLD_SEED_BITS = WORLD_SEED_LONGS * 64;

    public static final long[] worldSeed = new long[WORLD_SEED_LONGS];
    public static final ThreadLocal<Integer> dimension = ThreadLocal.withInitial(() -> 0);
    
    private static final ConcurrentHashMap<String, long[]> seedCache = new ConcurrentHashMap<>();
    private static final int MAX_CACHE_SIZE = 100;

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
        POTENTIONAL_FEATURE,
        GENERATE_FEATURE,
        JIGSAW_PLACEMENT,
        STRONGHOLDS,
        POPULATION,
        DECORATION,
        SLIME_CHUNK
    }

    public static void setupGlobals(ServerLevel world) {
        long[] seed = ((IWorldOptionsFeatureSeed) world.getServer().getWorldData().worldGenOptions()).secureSeed$featureSeed();
        System.arraycopy(seed, 0, worldSeed, 0, WORLD_SEED_LONGS);
        int worldIndex = Iterables.indexOf(world.getServer().levelKeys(), it -> it == world.dimension());
        if (worldIndex == -1)
            worldIndex = world.getServer().levelKeys().size();
        dimension.set(worldIndex);
    }

    public static long[] createRandomWorldSeed() {
        long[] seed = new long[WORLD_SEED_LONGS];
        SecureRandom rand = new SecureRandom();
        for (int i = 0; i < WORLD_SEED_LONGS; i++) {
            seed[i] = rand.nextLong();
        }
        return seed;
    }

    public static Optional<long[]> parseSeed(String seedStr) {
        if (seedStr.isEmpty()) return Optional.empty();

        long[] cached = seedCache.get(seedStr);
        if (cached != null) {
            return Optional.of(cached.clone());
        }

        if (seedStr.length() != WORLD_SEED_BITS) {
            throw new IllegalArgumentException("Secure seed length must be " + WORLD_SEED_BITS + "-bit but found " + seedStr.length() + "-bit.");
        }

        long[] seed = new long[WORLD_SEED_LONGS];

        for (int i = 0; i < WORLD_SEED_LONGS; i++) {
            int start = i * 64;
            int end = start + 64;
            String seedSection = seedStr.substring(start, end);

            BigInteger seedInDecimal = new BigInteger(seedSection, 2);
            seed[i] = seedInDecimal.longValue();
        }

        if (seedCache.size() < MAX_CACHE_SIZE) {
            seedCache.put(seedStr, seed.clone());
        }

        return Optional.of(seed);
    }

    public static String seedToString(long[] seed) {
        StringBuilder sb = new StringBuilder(WORLD_SEED_BITS);

        for (long longV : seed) {
            String binaryStr = String.format("%64s", Long.toBinaryString(longV)).replace(' ', '0');
            sb.append(binaryStr);
        }

        return sb.toString();
    }
    
    public static void clearCache() {
        seedCache.clear();
    }
}
