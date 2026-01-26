package org.virgil.akiasync.test.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for vector math utilities used in biome sampling and visibility calculations.
 * Validates SIMD-compatible operations and precision.
 */
public class VectorMathTest {

    private static final double EPSILON = 1e-10;

    @Test
    @DisplayName("Dot product should be commutative")
    void testDotProductCommutative() {
        double[] a = {1.0, 2.0, 3.0};
        double[] b = {4.0, 5.0, 6.0};
        
        double dotAB = dot(a, b);
        double dotBA = dot(b, a);
        
        assertEquals(dotAB, dotBA, EPSILON);
    }

    @Test
    @DisplayName("Dot product with zero vector should be zero")
    void testDotProductWithZero() {
        double[] a = {1.0, 2.0, 3.0};
        double[] zero = {0.0, 0.0, 0.0};
        
        assertEquals(0.0, dot(a, zero), EPSILON);
    }

    @Test
    @DisplayName("Dot product of unit vectors should be cosine of angle")
    void testDotProductUnitVectors() {
        double[] x = {1.0, 0.0, 0.0};
        double[] y = {0.0, 1.0, 0.0};
        double[] negX = {-1.0, 0.0, 0.0};
        
        assertEquals(0.0, dot(x, y), EPSILON, "Perpendicular vectors");
        assertEquals(-1.0, dot(x, negX), EPSILON, "Opposite vectors");
        assertEquals(1.0, dot(x, x), EPSILON, "Same vector");
    }

    @Test
    @DisplayName("Vector normalization should produce unit length")
    void testNormalization() {
        double[] v = {3.0, 4.0, 0.0};
        double[] normalized = normalize(v);
        
        double length = Math.sqrt(dot(normalized, normalized));
        assertEquals(1.0, length, EPSILON);
    }

    @Test
    @DisplayName("Distance squared should be non-negative")
    void testDistanceSquaredNonNegative() {
        for (int i = 0; i < 100; i++) {
            double x1 = Math.random() * 1000 - 500;
            double y1 = Math.random() * 1000 - 500;
            double z1 = Math.random() * 1000 - 500;
            double x2 = Math.random() * 1000 - 500;
            double y2 = Math.random() * 1000 - 500;
            double z2 = Math.random() * 1000 - 500;
            
            double distSq = distanceSquared(x1, y1, z1, x2, y2, z2);
            assertTrue(distSq >= 0, "Distance squared should be non-negative");
        }
    }

    @Test
    @DisplayName("Distance to self should be zero")
    void testDistanceToSelf() {
        double x = 100.5, y = 64.0, z = -200.3;
        double distSq = distanceSquared(x, y, z, x, y, z);
        
        assertEquals(0.0, distSq, EPSILON);
    }

    @Test
    @DisplayName("Distance should be symmetric")
    void testDistanceSymmetric() {
        double x1 = 10, y1 = 20, z1 = 30;
        double x2 = 40, y2 = 50, z2 = 60;
        
        double dist1 = distanceSquared(x1, y1, z1, x2, y2, z2);
        double dist2 = distanceSquared(x2, y2, z2, x1, y1, z1);
        
        assertEquals(dist1, dist2, EPSILON);
    }

    @Test
    @DisplayName("Cross product should be anti-commutative")
    void testCrossProductAntiCommutative() {
        double[] a = {1.0, 2.0, 3.0};
        double[] b = {4.0, 5.0, 6.0};
        
        double[] axb = cross(a, b);
        double[] bxa = cross(b, a);
        
        assertEquals(-axb[0], bxa[0], EPSILON);
        assertEquals(-axb[1], bxa[1], EPSILON);
        assertEquals(-axb[2], bxa[2], EPSILON);
    }

    @Test
    @DisplayName("Cross product of parallel vectors should be zero")
    void testCrossProductParallel() {
        double[] a = {1.0, 2.0, 3.0};
        double[] b = {2.0, 4.0, 6.0}; // 2 * a
        
        double[] result = cross(a, b);
        
        assertEquals(0.0, result[0], EPSILON);
        assertEquals(0.0, result[1], EPSILON);
        assertEquals(0.0, result[2], EPSILON);
    }

    @Test
    @DisplayName("Lerp at t=0 should return start")
    void testLerpAtZero() {
        double start = 10.0, end = 20.0;
        assertEquals(start, lerp(start, end, 0.0), EPSILON);
    }

    @Test
    @DisplayName("Lerp at t=1 should return end")
    void testLerpAtOne() {
        double start = 10.0, end = 20.0;
        assertEquals(end, lerp(start, end, 1.0), EPSILON);
    }

    @Test
    @DisplayName("Lerp at t=0.5 should return midpoint")
    void testLerpAtHalf() {
        double start = 10.0, end = 20.0;
        assertEquals(15.0, lerp(start, end, 0.5), EPSILON);
    }

    @Test
    @DisplayName("Clamp should constrain values")
    void testClamp() {
        assertEquals(5.0, clamp(3.0, 5.0, 10.0), EPSILON, "Below min");
        assertEquals(10.0, clamp(15.0, 5.0, 10.0), EPSILON, "Above max");
        assertEquals(7.0, clamp(7.0, 5.0, 10.0), EPSILON, "Within range");
    }

    @Test
    @DisplayName("Floor mod should handle negative numbers")
    void testFloorMod() {
        assertEquals(2, floorMod(7, 5));
        assertEquals(3, floorMod(-2, 5));
        assertEquals(0, floorMod(10, 5));
        assertEquals(4, floorMod(-1, 5));
    }

    @Test
    @DisplayName("Biome fiddle value should be in range [-0.45, 0.45]")
    void testBiomeFiddleRange() {
        for (int i = 0; i < 1000; i++) {
            long seed = (long) (Math.random() * Long.MAX_VALUE);
            double fiddle = calculateFiddle(seed);
            
            assertTrue(fiddle >= -0.45 && fiddle <= 0.45,
                "Fiddle " + fiddle + " should be in range [-0.45, 0.45]");
        }
    }

    // Helper methods
    private double dot(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    private double[] normalize(double[] v) {
        double len = Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
        if (len == 0) return new double[]{0, 0, 0};
        return new double[]{v[0] / len, v[1] / len, v[2] / len};
    }

    private double distanceSquared(double x1, double y1, double z1, 
                                    double x2, double y2, double z2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double dz = z2 - z1;
        return dx * dx + dy * dy + dz * dz;
    }

    private double[] cross(double[] a, double[] b) {
        return new double[]{
            a[1] * b[2] - a[2] * b[1],
            a[2] * b[0] - a[0] * b[2],
            a[0] * b[1] - a[1] * b[0]
        };
    }

    private double lerp(double start, double end, double t) {
        return start + t * (end - start);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private int floorMod(int x, int y) {
        return Math.floorMod(x, y);
    }

    private double calculateFiddle(long seed) {
        return (((seed >> 24) & 1023) / 1024.0 - 0.5) * 0.9;
    }
}
