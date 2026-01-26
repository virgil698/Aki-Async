package org.virgil.akiasync.test.worldgen;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

/**
 * Performance benchmarks for biome sampling algorithms.
 * Run with: ./gradlew test --tests "*BiomeSamplerBenchmark*"
 */
public class BiomeSamplerBenchmark {

    private static final long LCG_MULT = 6364136223846793005L;
    private static final long LCG_ADD = 1442695040888963407L;
    private static final int WARMUP_ITERATIONS = 10000;
    private static final int BENCHMARK_ITERATIONS = 100000;

    @Test
    @DisplayName("Benchmark: Scalar biome corner finding")
    void benchmarkScalarCornerFinding() {
        long seed = 0xDEADBEEFL;

        // Warmup
        int warmupSum = 0;
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            warmupSum += findClosestCornerScalar(seed, i % 1000, 64, i % 500, 0.25, 0.5, 0.75);
        }
        if (warmupSum == Integer.MIN_VALUE) throw new AssertionError("Warmup failed");

        // Benchmark
        long startTime = System.nanoTime();
        int checksum = 0;
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            checksum += findClosestCornerScalar(seed, i % 1000, 64, i % 500,
                (i % 4) / 4.0, ((i + 1) % 4) / 4.0, ((i + 2) % 4) / 4.0);
        }
        long endTime = System.nanoTime();

        double msPerOp = (endTime - startTime) / 1_000_000.0 / BENCHMARK_ITERATIONS;
        double opsPerSec = BENCHMARK_ITERATIONS / ((endTime - startTime) / 1_000_000_000.0);

        System.out.printf("[Benchmark] Scalar corner finding:%n");
        System.out.printf("  Total time: %.2f ms%n", (endTime - startTime) / 1_000_000.0);
        System.out.printf("  Per operation: %.4f µs%n", msPerOp * 1000);
        System.out.printf("  Operations/sec: %.0f%n", opsPerSec);
        System.out.printf("  Checksum: %d (for verification)%n", checksum);
    }

    @Test
    @DisplayName("Benchmark: Unrolled biome corner finding")
    void benchmarkUnrolledCornerFinding() {
        long seed = 0xDEADBEEFL;

        // Warmup
        int warmupSum = 0;
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            warmupSum += findClosestCornerUnrolled(seed, i % 1000, 64, i % 500, 0.25, 0.5, 0.75);
        }
        if (warmupSum == Integer.MIN_VALUE) throw new AssertionError("Warmup failed");

        // Benchmark
        long startTime = System.nanoTime();
        int checksum = 0;
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            checksum += findClosestCornerUnrolled(seed, i % 1000, 64, i % 500,
                (i % 4) / 4.0, ((i + 1) % 4) / 4.0, ((i + 2) % 4) / 4.0);
        }
        long endTime = System.nanoTime();

        double msPerOp = (endTime - startTime) / 1_000_000.0 / BENCHMARK_ITERATIONS;
        double opsPerSec = BENCHMARK_ITERATIONS / ((endTime - startTime) / 1_000_000_000.0);

        System.out.printf("[Benchmark] Unrolled corner finding:%n");
        System.out.printf("  Total time: %.2f ms%n", (endTime - startTime) / 1_000_000.0);
        System.out.printf("  Per operation: %.4f µs%n", msPerOp * 1000);
        System.out.printf("  Operations/sec: %.0f%n", opsPerSec);
        System.out.printf("  Checksum: %d (for verification)%n", checksum);
    }

    @Test
    @DisplayName("Benchmark: Xoroshiro128++ random generation")
    void benchmarkXoroshiroGeneration() {
        TestXoroshiro rng = new TestXoroshiro(12345L, 67890L);

        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            rng.nextLong();
        }

        rng = new TestXoroshiro(12345L, 67890L);

        // Benchmark
        long startTime = System.nanoTime();
        long checksum = 0;
        for (int i = 0; i < BENCHMARK_ITERATIONS * 10; i++) {
            checksum ^= rng.nextLong();
        }
        long endTime = System.nanoTime();

        double nsPerOp = (double)(endTime - startTime) / (BENCHMARK_ITERATIONS * 10);
        double opsPerSec = (BENCHMARK_ITERATIONS * 10) / ((endTime - startTime) / 1_000_000_000.0);

        System.out.printf("[Benchmark] Xoroshiro128++ generation:%n");
        System.out.printf("  Total time: %.2f ms%n", (endTime - startTime) / 1_000_000.0);
        System.out.printf("  Per operation: %.2f ns%n", nsPerOp);
        System.out.printf("  Operations/sec: %.0f M%n", opsPerSec / 1_000_000);
        System.out.printf("  Checksum: %d (for verification)%n", checksum);
    }

    @Test
    @DisplayName("Benchmark: Batch vs Sequential random generation")
    void benchmarkBatchVsSequential() {
        int batchSize = 64;
        int numBatches = BENCHMARK_ITERATIONS / batchSize;

        // Sequential
        TestXoroshiro rng1 = new TestXoroshiro(42L, 42L);
        long startSeq = System.nanoTime();
        long checksumSeq = 0;
        for (int b = 0; b < numBatches; b++) {
            for (int i = 0; i < batchSize; i++) {
                checksumSeq ^= rng1.nextLong();
            }
        }
        long endSeq = System.nanoTime();

        // Batch (simulated)
        TestXoroshiro rng2 = new TestXoroshiro(42L, 42L);
        long[] batch = new long[batchSize];
        long startBatch = System.nanoTime();
        long checksumBatch = 0;
        for (int b = 0; b < numBatches; b++) {
            // Fill batch
            for (int i = 0; i < batchSize; i++) {
                batch[i] = rng2.nextLong();
            }
            // Consume batch
            for (int i = 0; i < batchSize; i++) {
                checksumBatch ^= batch[i];
            }
        }
        long endBatch = System.nanoTime();

        System.out.printf("[Benchmark] Sequential vs Batch random generation:%n");
        System.out.printf("  Sequential: %.2f ms%n", (endSeq - startSeq) / 1_000_000.0);
        System.out.printf("  Batch: %.2f ms%n", (endBatch - startBatch) / 1_000_000.0);
        System.out.printf("  Checksums match: %b%n", checksumSeq == checksumBatch);
    }

    // Scalar implementation
    private int findClosestCornerScalar(long biomeZoomSeed, int quartX, int quartY, int quartZ,
                                         double fracX, double fracY, double fracZ) {
        int bestIndex = 0;
        double bestDistance = Double.POSITIVE_INFINITY;

        for (int i = 0; i < 8; i++) {
            boolean xFlag = (i & 4) == 0;
            boolean yFlag = (i & 2) == 0;
            boolean zFlag = (i & 1) == 0;

            int sampleX = xFlag ? quartX : quartX + 1;
            int sampleY = yFlag ? quartY : quartY + 1;
            int sampleZ = zFlag ? quartZ : quartZ + 1;

            double offsetX = xFlag ? fracX : fracX - 1.0;
            double offsetY = yFlag ? fracY : fracY - 1.0;
            double offsetZ = zFlag ? fracZ : fracZ - 1.0;

            double distance = getFiddledDistance(biomeZoomSeed, sampleX, sampleY, sampleZ,
                offsetX, offsetY, offsetZ);

            if (distance < bestDistance) {
                bestIndex = i;
                bestDistance = distance;
            }
        }
        return bestIndex;
    }

    // Unrolled implementation (matches BiomeAccessOptimizationMixin)
    private int findClosestCornerUnrolled(long biomeZoomSeed, int quartX, int quartY, int quartZ,
                                           double fracX, double fracY, double fracZ) {
        int bestIndex = 0;
        double bestDistance;

        // Corner 0
        bestDistance = computeCornerDistance(biomeZoomSeed, quartX, quartY, quartZ, fracX, fracY, fracZ);

        // Corner 1
        double d1 = computeCornerDistance(biomeZoomSeed, quartX, quartY, quartZ + 1, fracX, fracY, fracZ - 1.0);
        if (d1 < bestDistance) { bestIndex = 1; bestDistance = d1; }

        // Corner 2
        double d2 = computeCornerDistance(biomeZoomSeed, quartX, quartY + 1, quartZ, fracX, fracY - 1.0, fracZ);
        if (d2 < bestDistance) { bestIndex = 2; bestDistance = d2; }

        // Corner 3
        double d3 = computeCornerDistance(biomeZoomSeed, quartX, quartY + 1, quartZ + 1, fracX, fracY - 1.0, fracZ - 1.0);
        if (d3 < bestDistance) { bestIndex = 3; bestDistance = d3; }

        // Corner 4
        double d4 = computeCornerDistance(biomeZoomSeed, quartX + 1, quartY, quartZ, fracX - 1.0, fracY, fracZ);
        if (d4 < bestDistance) { bestIndex = 4; bestDistance = d4; }

        // Corner 5
        double d5 = computeCornerDistance(biomeZoomSeed, quartX + 1, quartY, quartZ + 1, fracX - 1.0, fracY, fracZ - 1.0);
        if (d5 < bestDistance) { bestIndex = 5; bestDistance = d5; }

        // Corner 6
        double d6 = computeCornerDistance(biomeZoomSeed, quartX + 1, quartY + 1, quartZ, fracX - 1.0, fracY - 1.0, fracZ);
        if (d6 < bestDistance) { bestIndex = 6; bestDistance = d6; }

        // Corner 7
        double d7 = computeCornerDistance(biomeZoomSeed, quartX + 1, quartY + 1, quartZ + 1, fracX - 1.0, fracY - 1.0, fracZ - 1.0);
        if (d7 < bestDistance) { bestIndex = 7; }

        return bestIndex;
    }

    private double computeCornerDistance(long seed, int x, int y, int z, double fx, double fy, double fz) {
        seed = seed * (seed * LCG_MULT + LCG_ADD) + x;
        seed = seed * (seed * LCG_MULT + LCG_ADD) + y;
        seed = seed * (seed * LCG_MULT + LCG_ADD) + z;
        seed = seed * (seed * LCG_MULT + LCG_ADD) + x;
        seed = seed * (seed * LCG_MULT + LCG_ADD) + y;
        seed = seed * (seed * LCG_MULT + LCG_ADD) + z;

        double fiddleX = (((seed >> 24) & 1023) / 1024.0 - 0.5) * 0.9;
        seed = seed * (seed * LCG_MULT + LCG_ADD) + seed;
        double fiddleY = (((seed >> 24) & 1023) / 1024.0 - 0.5) * 0.9;
        seed = seed * (seed * LCG_MULT + LCG_ADD) + seed;
        double fiddleZ = (((seed >> 24) & 1023) / 1024.0 - 0.5) * 0.9;

        double dx = fx + fiddleX;
        double dy = fy + fiddleY;
        double dz = fz + fiddleZ;
        return dx * dx + dy * dy + dz * dz;
    }

    private double getFiddledDistance(long seed, int x, int y, int z, double xf, double yf, double zf) {
        long l = seed * (seed * LCG_MULT + LCG_ADD) + x;
        l = l * (l * LCG_MULT + LCG_ADD) + y;
        l = l * (l * LCG_MULT + LCG_ADD) + z;
        l = l * (l * LCG_MULT + LCG_ADD) + x;
        l = l * (l * LCG_MULT + LCG_ADD) + y;
        l = l * (l * LCG_MULT + LCG_ADD) + z;

        double d = (((l >> 24) & 1023) / 1024.0 - 0.5) * 0.9;
        l = l * (l * LCG_MULT + LCG_ADD) + seed;
        double e = (((l >> 24) & 1023) / 1024.0 - 0.5) * 0.9;
        l = l * (l * LCG_MULT + LCG_ADD) + seed;
        double f = (((l >> 24) & 1023) / 1024.0 - 0.5) * 0.9;

        return (zf + f) * (zf + f) + (yf + e) * (yf + e) + (xf + d) * (xf + d);
    }

    static class TestXoroshiro {
        private long seedLo;
        private long seedHi;

        public TestXoroshiro(long seedLo, long seedHi) {
            this.seedLo = seedLo;
            this.seedHi = seedHi;
        }

        public long nextLong() {
            long l = this.seedLo;
            long l1 = this.seedHi;
            long l2 = Long.rotateLeft(l + l1, 17) + l;
            l1 ^= l;
            this.seedLo = Long.rotateLeft(l, 49) ^ l1 ^ l1 << 21;
            this.seedHi = Long.rotateLeft(l1, 28);
            return l2;
        }
    }
}
