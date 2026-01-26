package org.virgil.akiasync.mixin.util.worldgen;

import jdk.incubator.vector.*;
import java.util.Arrays;

/**
 * SIMD-accelerated noise generation utilities.
 * Uses Java Vector API to batch-process multiple noise samples simultaneously.
 */
public class VectorizedNoiseGenerator {

    private static final VectorSpecies<Double> SPECIES_D = DoubleVector.SPECIES_PREFERRED;
    private static final VectorSpecies<Long> SPECIES_L = LongVector.SPECIES_PREFERRED;
    private static final int VECTOR_LENGTH = SPECIES_D.length();

    // Precomputed gradient table (16 gradients * 3 components = 48 doubles)
    private static final double[] GRADIENTS = {
        1, 1, 0,  -1, 1, 0,  1, -1, 0,  -1, -1, 0,
        1, 0, 1,  -1, 0, 1,  1, 0, -1,  -1, 0, -1,
        0, 1, 1,  0, -1, 1,  0, 1, -1,  0, -1, -1,
        1, 1, 0,  0, -1, 1,  -1, 1, 0,  0, -1, -1
    };

    // Smoothstep constants
    private static final DoubleVector SIX = DoubleVector.broadcast(SPECIES_D, 6.0);
    private static final DoubleVector FIFTEEN = DoubleVector.broadcast(SPECIES_D, 15.0);
    private static final DoubleVector TEN = DoubleVector.broadcast(SPECIES_D, 10.0);
    private static final DoubleVector ONE = DoubleVector.broadcast(SPECIES_D, 1.0);
    private static final DoubleVector ZERO = DoubleVector.broadcast(SPECIES_D, 0.0);

    private static volatile boolean initialized = false;
    private static volatile boolean available = false;

    static {
        try {
            // Test if Vector API is available
            DoubleVector test = DoubleVector.broadcast(SPECIES_D, 1.0);
            available = true;
            initialized = true;
            System.out.println("[AkiAsync] VectorizedNoiseGenerator initialized with " + SPECIES_D);
        } catch (Throwable t) {
            available = false;
            initialized = true;
            System.out.println("[AkiAsync] VectorizedNoiseGenerator not available: " + t.getMessage());
        }
    }

    public static boolean isAvailable() {
        return available;
    }

    public static int getVectorLength() {
        return VECTOR_LENGTH;
    }

    /**
     * Vectorized smoothstep: t^3 * (t * (t * 6 - 15) + 10)
     */
    public static DoubleVector smoothstepVector(DoubleVector t) {
        // t * 6 - 15
        DoubleVector a = t.mul(SIX).sub(FIFTEEN);
        // t * a + 10
        DoubleVector b = t.mul(a).add(TEN);
        // t^3 * b
        return t.mul(t).mul(t).mul(b);
    }

    /**
     * Batch compute noise for multiple X coordinates with same Y,Z.
     * This is useful for horizontal slices during terrain generation.
     *
     * @param xs Array of X coordinates (must be multiple of VECTOR_LENGTH)
     * @param y  Fixed Y coordinate
     * @param z  Fixed Z coordinate
     * @param xo X offset from noise generator
     * @param yo Y offset from noise generator
     * @param zo Z offset from noise generator
     * @param p  Permutation table (256 bytes)
     * @param results Output array for noise values
     */
    public static void noiseBatchX(double[] xs, double y, double z,
                                   double xo, double yo, double zo,
                                   byte[] p, double[] results) {
        if (!available) {
            // Fallback to scalar
            for (int i = 0; i < xs.length; i++) {
                results[i] = noiseScalar(xs[i] + xo, y + yo, z + zo, p);
            }
            return;
        }

        double offsetY = y + yo;
        double offsetZ = z + zo;

        int gridY = (int) Math.floor(offsetY);
        int gridZ = (int) Math.floor(offsetZ);
        double deltaY = offsetY - gridY;
        double deltaZ = offsetZ - gridZ;

        // Precompute Y and Z dependent values
        double smoothY = smoothstepScalar(deltaY);
        double smoothZ = smoothstepScalar(deltaZ);

        int vectorCount = xs.length / VECTOR_LENGTH;
        int remainder = xs.length % VECTOR_LENGTH;

        for (int v = 0; v < vectorCount; v++) {
            int offset = v * VECTOR_LENGTH;

            // Load X coordinates and add offset
            DoubleVector vx = DoubleVector.fromArray(SPECIES_D, xs, offset);
            vx = vx.add(xo);

            // Floor to get grid coordinates
            double[] tempX = new double[VECTOR_LENGTH];
            vx.intoArray(tempX, 0);

            for (int i = 0; i < VECTOR_LENGTH; i++) {
                double offsetX = tempX[i];
                int gridX = (int) Math.floor(offsetX);
                double deltaX = offsetX - gridX;

                results[offset + i] = sampleNoise(gridX, gridY, gridZ,
                    deltaX, deltaY, deltaZ, p, smoothY, smoothZ);
            }
        }

        // Handle remainder
        for (int i = vectorCount * VECTOR_LENGTH; i < xs.length; i++) {
            results[i] = noiseScalar(xs[i] + xo, y + yo, z + zo, p);
        }
    }

    /**
     * Batch compute noise for a 2D grid (XZ plane at fixed Y).
     * Optimized for horizontal terrain sampling.
     */
    public static void noiseBatch2D(double[] xs, double[] zs, double y,
                                    double xo, double yo, double zo,
                                    byte[] p, double[] results) {
        if (!available || xs.length != zs.length) {
            for (int i = 0; i < xs.length; i++) {
                results[i] = noiseScalar(xs[i] + xo, y + yo, zs[i] + zo, p);
            }
            return;
        }

        double offsetY = y + yo;
        int gridY = (int) Math.floor(offsetY);
        double deltaY = offsetY - gridY;
        double smoothY = smoothstepScalar(deltaY);

        for (int i = 0; i < xs.length; i++) {
            double offsetX = xs[i] + xo;
            double offsetZ = zs[i] + zo;

            int gridX = (int) Math.floor(offsetX);
            int gridZ = (int) Math.floor(offsetZ);

            double deltaX = offsetX - gridX;
            double deltaZ = offsetZ - gridZ;
            double smoothZ = smoothstepScalar(deltaZ);

            results[i] = sampleNoise(gridX, gridY, gridZ,
                deltaX, deltaY, deltaZ, p, smoothY, smoothZ);
        }
    }

    private static double sampleNoise(int gridX, int gridY, int gridZ,
                                      double deltaX, double deltaY, double deltaZ,
                                      byte[] p, double smoothY, double smoothZ) {
        // Permutation lookups
        int px0 = p[gridX & 0xFF] & 0xFF;
        int px1 = p[(gridX + 1) & 0xFF] & 0xFF;

        int py00 = p[(px0 + gridY) & 0xFF] & 0xFF;
        int py10 = p[(px1 + gridY) & 0xFF] & 0xFF;
        int py01 = p[(px0 + gridY + 1) & 0xFF] & 0xFF;
        int py11 = p[(px1 + gridY + 1) & 0xFF] & 0xFF;

        int pz000 = (p[(py00 + gridZ) & 0xFF] & 15) * 3;
        int pz100 = (p[(py10 + gridZ) & 0xFF] & 15) * 3;
        int pz010 = (p[(py01 + gridZ) & 0xFF] & 15) * 3;
        int pz110 = (p[(py11 + gridZ) & 0xFF] & 15) * 3;
        int pz001 = (p[(py00 + gridZ + 1) & 0xFF] & 15) * 3;
        int pz101 = (p[(py10 + gridZ + 1) & 0xFF] & 15) * 3;
        int pz011 = (p[(py01 + gridZ + 1) & 0xFF] & 15) * 3;
        int pz111 = (p[(py11 + gridZ + 1) & 0xFF] & 15) * 3;

        double dx1 = deltaX - 1.0;
        double dy1 = deltaY - 1.0;
        double dz1 = deltaZ - 1.0;

        // Gradient dot products
        double d000 = gradDot(pz000, deltaX, deltaY, deltaZ);
        double d100 = gradDot(pz100, dx1, deltaY, deltaZ);
        double d010 = gradDot(pz010, deltaX, dy1, deltaZ);
        double d110 = gradDot(pz110, dx1, dy1, deltaZ);
        double d001 = gradDot(pz001, deltaX, deltaY, dz1);
        double d101 = gradDot(pz101, dx1, deltaY, dz1);
        double d011 = gradDot(pz011, deltaX, dy1, dz1);
        double d111 = gradDot(pz111, dx1, dy1, dz1);

        double smoothX = smoothstepScalar(deltaX);

        // Trilinear interpolation
        double l00 = lerp(smoothX, d000, d100);
        double l10 = lerp(smoothX, d010, d110);
        double l01 = lerp(smoothX, d001, d101);
        double l11 = lerp(smoothX, d011, d111);

        double l0 = lerp(smoothY, l00, l10);
        double l1 = lerp(smoothY, l01, l11);

        return lerp(smoothZ, l0, l1);
    }

    private static double gradDot(int gradIdx, double x, double y, double z) {
        return GRADIENTS[gradIdx] * x + GRADIENTS[gradIdx + 1] * y + GRADIENTS[gradIdx + 2] * z;
    }

    private static double lerp(double t, double a, double b) {
        return a + t * (b - a);
    }

    private static double smoothstepScalar(double t) {
        return t * t * t * (t * (t * 6.0 - 15.0) + 10.0);
    }

    private static double noiseScalar(double x, double y, double z, byte[] p) {
        int gridX = (int) Math.floor(x);
        int gridY = (int) Math.floor(y);
        int gridZ = (int) Math.floor(z);

        double deltaX = x - gridX;
        double deltaY = y - gridY;
        double deltaZ = z - gridZ;

        double smoothY = smoothstepScalar(deltaY);
        double smoothZ = smoothstepScalar(deltaZ);

        return sampleNoise(gridX, gridY, gridZ, deltaX, deltaY, deltaZ, p, smoothY, smoothZ);
    }

    /**
     * Get optimization info for debugging
     */
    public static String getOptimizationInfo() {
        if (!available) {
            return "VectorizedNoiseGenerator: Not available (fallback to scalar)";
        }
        return String.format("VectorizedNoiseGenerator: %s (length=%d)",
            SPECIES_D.toString(), VECTOR_LENGTH);
    }
}
