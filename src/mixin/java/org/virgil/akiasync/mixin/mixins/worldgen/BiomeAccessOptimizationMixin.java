package org.virgil.akiasync.mixin.mixins.worldgen;

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


@Mixin(BiomeManager.class)
public class BiomeAccessOptimizationMixin {

    @Shadow @Final private long biomeZoomSeed;
    @Shadow @Final private BiomeManager.NoiseBiomeSource noiseBiomeSource;

    @Unique
    private static volatile boolean initialized = false;
    @Unique
    private static volatile boolean enabled = true;

    
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

        int bestIndex = 0;
        double bestDistance = Double.POSITIVE_INFINITY;

        for (int i = 0; i < 8; ++i) {
            boolean xFlag = (i & 4) == 0;
            boolean yFlag = (i & 2) == 0;
            boolean zFlag = (i & 1) == 0;

            long sampleX = xFlag ? quartX : quartX + 1;
            long sampleY = yFlag ? quartY : quartY + 1;
            long sampleZ = zFlag ? quartZ : quartZ + 1;

            double offsetX = xFlag ? fracX : fracX - 1.0;
            double offsetY = yFlag ? fracY : fracY - 1.0;
            double offsetZ = zFlag ? fracZ : fracZ - 1.0;

            
            long seed = this.biomeZoomSeed;
            seed = seed * (seed * 6364136223846793005L + 1442695040888963407L) + sampleX;
            seed = seed * (seed * 6364136223846793005L + 1442695040888963407L) + sampleY;
            seed = seed * (seed * 6364136223846793005L + 1442695040888963407L) + sampleZ;
            seed = seed * (seed * 6364136223846793005L + 1442695040888963407L) + sampleX;
            seed = seed * (seed * 6364136223846793005L + 1442695040888963407L) + sampleY;
            seed = seed * (seed * 6364136223846793005L + 1442695040888963407L) + sampleZ;

            double fiddleX = ((double) ((seed >> 24) & 1023) / 1024.0 - 0.5) * 0.9;
            seed = seed * (seed * 6364136223846793005L + 1442695040888963407L) + this.biomeZoomSeed;
            double fiddleY = ((double) ((seed >> 24) & 1023) / 1024.0 - 0.5) * 0.9;
            seed = seed * (seed * 6364136223846793005L + 1442695040888963407L) + this.biomeZoomSeed;
            double fiddleZ = ((double) ((seed >> 24) & 1023) / 1024.0 - 0.5) * 0.9;

            double distance = Mth.square(offsetZ + fiddleZ) + Mth.square(offsetY + fiddleY) + Mth.square(offsetX + fiddleX);

            if (bestDistance > distance) {
                bestIndex = i;
                bestDistance = distance;
            }
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
                bridge.debugLog("[BiomeAccessOptimization] Initialized: enabled=%s", enabled);
            }
        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "BiomeAccessOptimization", "initConfig", e);
        }

        initialized = true;
    }
}
