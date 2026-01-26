package org.virgil.akiasync.test.async;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Timeout;
import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Tests for async structure locator functionality.
 * Validates async search, cancellation, and result handling.
 */
public class StructureLocatorTest {

    @Test
    @DisplayName("Structure search should complete asynchronously")
    @Timeout(5)
    void testAsyncSearch() throws Exception {
        TestStructureLocator locator = new TestStructureLocator();
        
        CompletableFuture<int[]> future = locator.findStructureAsync("village", 0, 0, 1000);
        
        assertFalse(future.isDone(), "Should not be immediately done");
        
        int[] result = future.get(3, TimeUnit.SECONDS);
        assertNotNull(result);
    }

    @Test
    @DisplayName("Search should be cancellable")
    @Timeout(5)
    void testSearchCancellation() {
        TestStructureLocator locator = new TestStructureLocator();
        locator.setSearchDelay(2000); // Long delay
        
        CompletableFuture<int[]> future = locator.findStructureAsync("fortress", 0, 0, 5000);
        
        // Cancel after short delay
        future.cancel(true);
        
        assertTrue(future.isCancelled() || future.isCompletedExceptionally());
    }

    @Test
    @DisplayName("Multiple concurrent searches should work")
    @Timeout(10)
    void testConcurrentSearches() throws Exception {
        TestStructureLocator locator = new TestStructureLocator();
        
        CompletableFuture<int[]> search1 = locator.findStructureAsync("village", 0, 0, 1000);
        CompletableFuture<int[]> search2 = locator.findStructureAsync("temple", 1000, 0, 1000);
        CompletableFuture<int[]> search3 = locator.findStructureAsync("monument", -1000, 0, 1000);
        
        CompletableFuture.allOf(search1, search2, search3).get(5, TimeUnit.SECONDS);
        
        assertNotNull(search1.get());
        assertNotNull(search2.get());
        assertNotNull(search3.get());
    }

    @Test
    @DisplayName("Search result should be within radius")
    @Timeout(5)
    void testSearchRadius() throws Exception {
        TestStructureLocator locator = new TestStructureLocator();
        locator.setConstrainToRadius(true); // Ensure result is within radius
        int centerX = 100, centerZ = 100, radius = 500;
        
        CompletableFuture<int[]> future = locator.findStructureAsync("village", centerX, centerZ, radius);
        int[] result = future.get(3, TimeUnit.SECONDS);
        
        if (result != null) {
            double distance = Math.sqrt(
                Math.pow(result[0] - centerX, 2) + 
                Math.pow(result[1] - centerZ, 2)
            );
            assertTrue(distance <= radius, "Result should be within search radius, got distance: " + distance);
        }
    }

    @Test
    @DisplayName("Search should handle not found gracefully")
    @Timeout(5)
    void testNotFound() throws Exception {
        TestStructureLocator locator = new TestStructureLocator();
        locator.setAlwaysNotFound(true);
        
        CompletableFuture<int[]> future = locator.findStructureAsync("nonexistent", 0, 0, 100);
        int[] result = future.get(3, TimeUnit.SECONDS);
        
        assertNull(result, "Should return null when not found");
    }

    @Test
    @DisplayName("Search callback should be invoked")
    @Timeout(5)
    void testSearchCallback() throws Exception {
        TestStructureLocator locator = new TestStructureLocator();
        AtomicBoolean callbackInvoked = new AtomicBoolean(false);
        AtomicReference<int[]> callbackResult = new AtomicReference<>();
        
        locator.findStructureAsync("village", 0, 0, 1000)
            .thenAccept(result -> {
                callbackInvoked.set(true);
                callbackResult.set(result);
            })
            .get(3, TimeUnit.SECONDS);
        
        assertTrue(callbackInvoked.get(), "Callback should be invoked");
    }

    @Test
    @DisplayName("Search should respect thread pool limits")
    @Timeout(10)
    void testThreadPoolLimits() throws Exception {
        TestStructureLocator locator = new TestStructureLocator(2); // Only 2 threads
        locator.setSearchDelay(100);
        
        long startTime = System.currentTimeMillis();
        
        // Submit 4 searches with 2 threads
        CompletableFuture<?>[] futures = new CompletableFuture[4];
        for (int i = 0; i < 4; i++) {
            futures[i] = locator.findStructureAsync("village", i * 100, 0, 500);
        }
        
        CompletableFuture.allOf(futures).get(5, TimeUnit.SECONDS);
        
        long elapsed = System.currentTimeMillis() - startTime;
        // With 2 threads and 4 tasks of 100ms each, should take ~200ms minimum
        assertTrue(elapsed >= 150, "Should be limited by thread pool");
        
        locator.shutdown();
    }

    @Test
    @DisplayName("Spiral search pattern should cover area")
    void testSpiralSearchPattern() {
        TestSpiralSearch spiral = new TestSpiralSearch();
        
        int[] visited = new int[100];
        int visitCount = 0;
        
        for (int i = 0; i < 100; i++) {
            int[] pos = spiral.next();
            int index = (pos[0] + 5) * 10 + (pos[1] + 5);
            if (index >= 0 && index < 100) {
                visited[index]++;
                visitCount++;
            }
        }
        
        assertTrue(visitCount > 50, "Should visit many positions");
    }

    // Test implementations
    static class TestStructureLocator {
        private final ExecutorService executor;
        private int searchDelay = 50;
        private boolean alwaysNotFound = false;
        private boolean constrainToRadius = false;

        TestStructureLocator() {
            this(4);
        }

        TestStructureLocator(int threads) {
            this.executor = Executors.newFixedThreadPool(threads);
        }

        void setSearchDelay(int ms) { this.searchDelay = ms; }
        void setAlwaysNotFound(boolean value) { this.alwaysNotFound = value; }
        void setConstrainToRadius(boolean value) { this.constrainToRadius = value; }

        CompletableFuture<int[]> findStructureAsync(String type, int centerX, int centerZ, int radius) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    Thread.sleep(searchDelay);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return null;
                }
                
                if (alwaysNotFound) return null;
                
                // Simulate finding a structure
                int offsetX, offsetZ;
                if (constrainToRadius) {
                    // Generate within a circle, not a square
                    double angle = Math.random() * 2 * Math.PI;
                    double dist = Math.random() * radius;
                    offsetX = (int) (Math.cos(angle) * dist);
                    offsetZ = (int) (Math.sin(angle) * dist);
                } else {
                    offsetX = (int) (Math.random() * radius * 2) - radius;
                    offsetZ = (int) (Math.random() * radius * 2) - radius;
                }
                
                return new int[]{centerX + offsetX, centerZ + offsetZ};
            }, executor);
        }

        void shutdown() {
            executor.shutdown();
        }
    }

    static class TestSpiralSearch {
        private int x = 0, z = 0;
        private int dx = 0, dz = -1;
        private int segmentLength = 1;
        private int segmentPassed = 0;
        private int segmentCount = 0;

        int[] next() {
            int[] result = {x, z};
            
            // Move to next position
            x += dx;
            z += dz;
            segmentPassed++;
            
            if (segmentPassed >= segmentLength) {
                segmentPassed = 0;
                
                // Rotate direction
                int temp = dx;
                dx = -dz;
                dz = temp;
                
                segmentCount++;
                if (segmentCount >= 2) {
                    segmentCount = 0;
                    segmentLength++;
                }
            }
            
            return result;
        }
    }
}
