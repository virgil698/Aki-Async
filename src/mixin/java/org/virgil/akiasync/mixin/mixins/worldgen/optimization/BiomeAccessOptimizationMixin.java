package org.virgil.akiasync.mixin.mixins.worldgen.optimization;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.virgil.akiasync.mixin.util.worldgen.VectorizedBiomeSampler;

@Mixin(BiomeManager.class)
public class BiomeAccessOptimizationMixin {

    @Shadow @Final private long biomeZoomSeed;
    @Shadow @Final private BiomeManager.NoiseBiomeSource noiseBiomeSource;

    @Unique
    private static volatile boolean initialized = false;
    @Unique
    private static volatile boolean enabled = true;
    @Unique
    private static volatile boolean vectorAvailable = false;

    @Overwrite
    public Holder<Biome> getBiome(BlockPos pos) {
        if (!initialized) {
            akiasync$initConfig();
        }

        if (!enabled) {
            return akiasync$getBiomeOriginal(pos);
        }

        final int x = pos.getX() - 2;
        final int y = pos.getY() - 2;
        final int z = pos.getZ() - 2;
        final int quartX = x >> 2;
        final int quartY = y >> 2;
        final int quartZ = z >> 2;
        final double fracX = (double) (x & 3) / 4.0;
        final double fracY = (double) (y & 3) / 4.0;
        final double fracZ = (double) (z & 3) / 4.0;

        int bestIndex;

        if (vectorAvailable) {
            // Use SIMD-accelerated sampling
            bestIndex = VectorizedBiomeSampler.findClosestCorner(
                this.biomeZoomSeed,
                quartX, quartY, quartZ,
                fracX, fracY, fracZ
            );
        } else {
            // Optimized scalar path with unrolled loop
            bestIndex = akiasync$findClosestCornerUnrolled(
                quartX, quartY, quartZ,
                fracX, fracY, fracZ
            );
        }

        int resX = (bestIndex & 4) == 0 ? quartX : quartX + 1;
        int resY = (bestIndex & 2) == 0 ? quartY : quartY + 1;
        int resZ = (bestIndex & 1) == 0 ? quartZ : quartZ + 1;

        return this.noiseBiomeSource.getNoiseBiome(resX, resY, resZ);
    }

    @Unique
    private Holder<Biome> akiasync$getBiomeOriginal(BlockPos pos) {

        int i = pos.getX() - 2;
        int j = pos.getY() - 2;
        int k = pos.getZ() - 2;
        int l = i >> 2;
        int m = j >> 2;
        int n = k >> 2;
        double d = (double)(i & 3) / 4.0;
        double e = (double)(j & 3) / 4.0;
        double f = (double)(k & 3) / 4.0;
        int o = 0;
        double g = Double.POSITIVE_INFINITY;

        for (int p = 0; p < 8; ++p) {
            boolean bl = (p & 4) == 0;
            boolean bl2 = (p & 2) == 0;
            boolean bl3 = (p & 1) == 0;
            int q = bl ? l : l + 1;
            int r = bl2 ? m : m + 1;
            int s = bl3 ? n : n + 1;
            double h = bl ? d : d - 1.0;
            double t = bl2 ? e : e - 1.0;
            double u = bl3 ? f : f - 1.0;
            double v = akiasync$getFiddledDistance(this.biomeZoomSeed, q, r, s, h, t, u);
            if (g > v) {
                o = p;
                g = v;
            }
        }

        int q = (o & 4) == 0 ? l : l + 1;
        int r = (o & 2) == 0 ? m : m + 1;
        int s = (o & 1) == 0 ? n : n + 1;
        return this.noiseBiomeSource.getNoiseBiome(q, r, s);
    }

    @Unique
    private static double akiasync$getFiddledDistance(long seed, int x, int y, int z, double xFraction, double yFraction, double zFraction) {
        long l = seed * (seed * 6364136223846793005L + 1442695040888963407L) + (long)x;
        l = l * (l * 6364136223846793005L + 1442695040888963407L) + (long)y;
        l = l * (l * 6364136223846793005L + 1442695040888963407L) + (long)z;
        l = l * (l * 6364136223846793005L + 1442695040888963407L) + (long)x;
        l = l * (l * 6364136223846793005L + 1442695040888963407L) + (long)y;
        l = l * (l * 6364136223846793005L + 1442695040888963407L) + (long)z;
        double d = akiasync$getFiddle(l);
        l = l * (l * 6364136223846793005L + 1442695040888963407L) + seed;
        double e = akiasync$getFiddle(l);
        l = l * (l * 6364136223846793005L + 1442695040888963407L) + seed;
        double f = akiasync$getFiddle(l);
        return Mth.square(zFraction + f) + Mth.square(yFraction + e) + Mth.square(xFraction + d);
    }

    @Unique
    private static double akiasync$getFiddle(long seed) {
        double d = (double)Math.floorMod(seed >> 24, 1024) / 1024.0;
        return (d - 0.5) * 0.9;
    }

    @Unique
    private static synchronized void akiasync$initConfig() {
        if (initialized) {
            return;
        }

        try {
            org.virgil.akiasync.mixin.bridge.Bridge bridge =
                org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();

            if (bridge != null) {
                enabled = bridge.isBiomeAccessOptimizationEnabled();
                vectorAvailable = VectorizedBiomeSampler.isAvailable();
                bridge.debugLog("[BiomeAccessOptimization] Initialized: enabled=%s, vectorAvailable=%s", enabled, vectorAvailable);

                initialized = true;
            }
        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "BiomeAccessOptimization", "initConfig", e);
        }
    }

    @Unique
    private int akiasync$findClosestCornerUnrolled(int quartX, int quartY, int quartZ,
                                                    double fracX, double fracY, double fracZ) {
        int bestIndex = 0;
        double bestDistance = Double.POSITIVE_INFINITY;

        // Corner 0: (0,0,0)
        {
            long seed = this.biomeZoomSeed;
            seed = seed * (seed * 6364136223846793005L + 1442695040888963407L) + quartX;
            seed = seed * (seed * 6364136223846793005L + 1442695040888963407L) + quartY;
            seed = seed * (seed * 6364136223846793005L + 1442695040888963407L) + quartZ;
            seed = seed * (seed * 6364136223846793005L + 1442695040888963407L) + quartX;
            seed = seed * (seed * 6364136223846793005L + 1442695040888963407L) + quartY;
            seed = seed * (seed * 6364136223846793005L + 1442695040888963407L) + quartZ;

            double fiddleX = ((double) ((seed >> 24) & 1023) / 1024.0 - 0.5) * 0.9;
            seed = seed * (seed * 6364136223846793005L + 1442695040888963407L) + this.biomeZoomSeed;
            double fiddleY = ((double) ((seed >> 24) & 1023) / 1024.0 - 0.5) * 0.9;
            seed = seed * (seed * 6364136223846793005L + 1442695040888963407L) + this.biomeZoomSeed;
            double fiddleZ = ((double) ((seed >> 24) & 1023) / 1024.0 - 0.5) * 0.9;

            double dx = fracX + fiddleX;
            double dy = fracY + fiddleY;
            double dz = fracZ + fiddleZ;
            bestDistance = dx * dx + dy * dy + dz * dz;
        }

        // Corner 1: (0,0,1)
        {
            long seed = this.biomeZoomSeed;
            seed = seed * (seed * 6364136223846793005L + 1442695040888963407L) + quartX;
            seed = seed * (seed * 6364136223846793005L + 1442695040888963407L) + quartY;
            seed = seed * (seed * 6364136223846793005L + 1442695040888963407L) + (quartZ + 1);
            seed = seed * (seed * 6364136223846793005L + 1442695040888963407L) + quartX;
            seed = seed * (seed * 6364136223846793005L + 1442695040888963407L) + quartY;
            seed = seed * (seed * 6364136223846793005L + 1442695040888963407L) + (quartZ + 1);

            double fiddleX = ((double) ((seed >> 24) & 1023) / 1024.0 - 0.5) * 0.9;
            seed = seed * (seed * 6364136223846793005L + 1442695040888963407L) + this.biomeZoomSeed;
            double fiddleY = ((double) ((seed >> 24) & 1023) / 1024.0 - 0.5) * 0.9;
            seed = seed * (seed * 6364136223846793005L + 1442695040888963407L) + this.biomeZoomSeed;
            double fiddleZ = ((double) ((seed >> 24) & 1023) / 1024.0 - 0.5) * 0.9;

            double dx = fracX + fiddleX;
            double dy = fracY + fiddleY;
            double dz = (fracZ - 1.0) + fiddleZ;
            double distance = dx * dx + dy * dy + dz * dz;
            if (distance < bestDistance) { bestIndex = 1; bestDistance = distance; }
        }

        // Corner 2: (0,1,0)
        {
            long seed = this.biomeZoomSeed;
            seed = seed * (seed * 6364136223846793005L + 1442695040888963407L) + quartX;
            seed = seed * (seed * 6364136223846793005L + 1442695040888963407L) + (quartY + 1);
            seed = seed * (seed * 6364136223846793005L + 1442695040888963407L) + quartZ;
            seed = seed * (seed * 6364136223846793005L + 1442695040888963407L) + quartX;
            seed = seed * (seed * 6364136223846793005L + 1442695040888963407L) + (quartY + 1);
            seed = seed * (seed * 6364136223846793005L + 1442695040888963407L) + quartZ;

            double fiddleX = ((double) ((seed >> 24) & 1023) / 1024.0 - 0.5) * 0.9;
            seed = seed * (seed * 6364136223846793005L + 1442695040888963407L) + this.biomeZoomSeed;
            double fiddleY = ((double) ((seed >> 24) & 1023) / 1024.0 - 0.5) * 0.9;
            seed = seed * (seed * 6364136223846793005L + 1442695040888963407L) + this.biomeZoomSeed;
            double fiddleZ = ((double) ((seed >> 24) & 1023) / 1024.0 - 0.5) * 0.9;

            double dx = fracX + fiddleX;
            double dy = (fracY - 1.0) + fiddleY;
            double dz = fracZ + fiddleZ;
            double distance = dx * dx + dy * dy + dz * dz;
            if (distance < bestDistance) { bestIndex = 2; bestDistance = distance; }
        }

        // Corner 3: (0,1,1)
        {
            long seed = this.biomeZoomSeed;
            seed = seed * (seed * 6364136223846793005L + 1442695040888963407L) + quartX;
            seed = seed * (seed * 6364136223846793005L + 1442695040888963407L) + (quartY + 1);
            seed = seed * (seed * 6364136223846793005L + 1442695040888963407L) + (quartZ + 1);
            seed = seed * (seed * 6364136223846793005L + 1442695040888963407L) + quartX;
            seed = seed * (seed * 6364136223846793005L + 1442695040888963407L) + (quartY + 1);
            seed = seed * (seed * 6364136223846793005L + 1442695040888963407L) + (quartZ + 1);

            double fiddleX = ((double) ((seed >> 24) & 1023) / 1024.0 - 0.5) * 0.9;
            seed = seed * (seed * 6364136223846793005L + 1442695040888963407L) + this.biomeZoomSeed;
            double fiddleY = ((double) ((seed >> 24) & 1023) / 1024.0 - 0.5) * 0.9;
            seed = seed * (seed * 6364136223846793005L + 1442695040888963407L) + this.biomeZoomSeed;
            double fiddleZ = ((double) ((seed >> 24) & 1023) / 1024.0 - 0.5) * 0.9;

            double dx = fracX + fiddleX;
            double dy = (fracY - 1.0) + fiddleY;
            double dz = (fracZ - 1.0) + fiddleZ;
            double distance = dx * dx + dy * dy + dz * dz;
            if (distance < bestDistance) { bestIndex = 3; bestDistance = distance; }
        }

        // Corner 4: (1,0,0)
        {
            long seed = this.biomeZoomSeed;
            seed = seed * (seed * 6364136223846793005L + 1442695040888963407L) + (quartX + 1);
            seed = seed * (seed * 6364136223846793005L + 1442695040888963407L) + quartY;
            seed = seed * (seed * 6364136223846793005L + 1442695040888963407L) + quartZ;
            seed = seed * (seed * 6364136223846793005L + 1442695040888963407L) + (quartX + 1);
            seed = seed * (seed * 6364136223846793005L + 1442695040888963407L) + quartY;
            seed = seed * (seed * 6364136223846793005L + 1442695040888963407L) + quartZ;

            double fiddleX = ((double) ((seed >> 24) & 1023) / 1024.0 - 0.5) * 0.9;
            seed = seed * (seed * 6364136223846793005L + 1442695040888963407L) + this.biomeZoomSeed;
            double fiddleY = ((double) ((seed >> 24) & 1023) / 1024.0 - 0.5) * 0.9;
            seed = seed * (seed * 6364136223846793005L + 1442695040888963407L) + this.biomeZoomSeed;
            double fiddleZ = ((double) ((seed >> 24) & 1023) / 1024.0 - 0.5) * 0.9;

            double dx = (fracX - 1.0) + fiddleX;
            double dy = fracY + fiddleY;
            double dz = fracZ + fiddleZ;
            double distance = dx * dx + dy * dy + dz * dz;
            if (distance < bestDistance) { bestIndex = 4; bestDistance = distance; }
        }

        // Corner 5: (1,0,1)
        {
            long seed = this.biomeZoomSeed;
            seed = seed * (seed * 6364136223846793005L + 1442695040888963407L) + (quartX + 1);
            seed = seed * (seed * 6364136223846793005L + 1442695040888963407L) + quartY;
            seed = seed * (seed * 6364136223846793005L + 1442695040888963407L) + (quartZ + 1);
            seed = seed * (seed * 6364136223846793005L + 1442695040888963407L) + (quartX + 1);
            seed = seed * (seed * 6364136223846793005L + 1442695040888963407L) + quartY;
            seed = seed * (seed * 6364136223846793005L + 1442695040888963407L) + (quartZ + 1);

            double fiddleX = ((double) ((seed >> 24) & 1023) / 1024.0 - 0.5) * 0.9;
            seed = seed * (seed * 6364136223846793005L + 1442695040888963407L) + this.biomeZoomSeed;
            double fiddleY = ((double) ((seed >> 24) & 1023) / 1024.0 - 0.5) * 0.9;
            seed = seed * (seed * 6364136223846793005L + 1442695040888963407L) + this.biomeZoomSeed;
            double fiddleZ = ((double) ((seed >> 24) & 1023) / 1024.0 - 0.5) * 0.9;

            double dx = (fracX - 1.0) + fiddleX;
            double dy = fracY + fiddleY;
            double dz = (fracZ - 1.0) + fiddleZ;
            double distance = dx * dx + dy * dy + dz * dz;
            if (distance < bestDistance) { bestIndex = 5; bestDistance = distance; }
        }

        // Corner 6: (1,1,0)
        {
            long seed = this.biomeZoomSeed;
            seed = seed * (seed * 6364136223846793005L + 1442695040888963407L) + (quartX + 1);
            seed = seed * (seed * 6364136223846793005L + 1442695040888963407L) + (quartY + 1);
            seed = seed * (seed * 6364136223846793005L + 1442695040888963407L) + quartZ;
            seed = seed * (seed * 6364136223846793005L + 1442695040888963407L) + (quartX + 1);
            seed = seed * (seed * 6364136223846793005L + 1442695040888963407L) + (quartY + 1);
            seed = seed * (seed * 6364136223846793005L + 1442695040888963407L) + quartZ;

            double fiddleX = ((double) ((seed >> 24) & 1023) / 1024.0 - 0.5) * 0.9;
            seed = seed * (seed * 6364136223846793005L + 1442695040888963407L) + this.biomeZoomSeed;
            double fiddleY = ((double) ((seed >> 24) & 1023) / 1024.0 - 0.5) * 0.9;
            seed = seed * (seed * 6364136223846793005L + 1442695040888963407L) + this.biomeZoomSeed;
            double fiddleZ = ((double) ((seed >> 24) & 1023) / 1024.0 - 0.5) * 0.9;

            double dx = (fracX - 1.0) + fiddleX;
            double dy = (fracY - 1.0) + fiddleY;
            double dz = fracZ + fiddleZ;
            double distance = dx * dx + dy * dy + dz * dz;
            if (distance < bestDistance) { bestIndex = 6; bestDistance = distance; }
        }

        // Corner 7: (1,1,1)
        {
            long seed = this.biomeZoomSeed;
            seed = seed * (seed * 6364136223846793005L + 1442695040888963407L) + (quartX + 1);
            seed = seed * (seed * 6364136223846793005L + 1442695040888963407L) + (quartY + 1);
            seed = seed * (seed * 6364136223846793005L + 1442695040888963407L) + (quartZ + 1);
            seed = seed * (seed * 6364136223846793005L + 1442695040888963407L) + (quartX + 1);
            seed = seed * (seed * 6364136223846793005L + 1442695040888963407L) + (quartY + 1);
            seed = seed * (seed * 6364136223846793005L + 1442695040888963407L) + (quartZ + 1);

            double fiddleX = ((double) ((seed >> 24) & 1023) / 1024.0 - 0.5) * 0.9;
            seed = seed * (seed * 6364136223846793005L + 1442695040888963407L) + this.biomeZoomSeed;
            double fiddleY = ((double) ((seed >> 24) & 1023) / 1024.0 - 0.5) * 0.9;
            seed = seed * (seed * 6364136223846793005L + 1442695040888963407L) + this.biomeZoomSeed;
            double fiddleZ = ((double) ((seed >> 24) & 1023) / 1024.0 - 0.5) * 0.9;

            double dx = (fracX - 1.0) + fiddleX;
            double dy = (fracY - 1.0) + fiddleY;
            double dz = (fracZ - 1.0) + fiddleZ;
            double distance = dx * dx + dy * dy + dz * dz;
            if (distance < bestDistance) { bestIndex = 7; }
        }

        return bestIndex;
    }
}
