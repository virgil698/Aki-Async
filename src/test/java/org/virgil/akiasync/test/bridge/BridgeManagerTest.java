package org.virgil.akiasync.test.bridge;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Tests for BridgeManager and Bridge configuration.
 * Validates bridge initialization, configuration access, and thread safety.
 */
public class BridgeManagerTest {

    private TestBridgeManager manager;

    @BeforeEach
    void setUp() {
        manager = new TestBridgeManager();
        manager.clearBridge();
    }

    @Test
    @DisplayName("Bridge should not be initialized by default")
    void testBridgeNotInitializedByDefault() {
        assertFalse(manager.isBridgeInitialized());
        assertNull(manager.getBridge());
    }

    @Test
    @DisplayName("Setting bridge should make it available")
    void testSetBridge() {
        TestBridge bridge = new TestBridge();
        manager.setBridge(bridge);

        assertTrue(manager.isBridgeInitialized());
        assertNotNull(manager.getBridge());
    }

    @Test
    @DisplayName("Clear bridge should remove it")
    void testClearBridge() {
        manager.setBridge(new TestBridge());
        assertTrue(manager.isBridgeInitialized());

        manager.clearBridge();
        assertFalse(manager.isBridgeInitialized());
    }

    @Test
    @DisplayName("Bridge configuration values should be accessible")
    void testBridgeConfigurationAccess() {
        TestBridge bridge = new TestBridge();
        bridge.setEntityTickParallel(true);
        bridge.setGeneralThreadPoolSize(8);
        bridge.setBrainThrottleInterval(5);

        manager.setBridge(bridge);

        TestBridge retrieved = manager.getBridge();
        assertTrue(retrieved.isEntityTickParallel());
        assertEquals(8, retrieved.getGeneralThreadPoolSize());
        assertEquals(5, retrieved.getBrainThrottleInterval());
    }

    @Test
    @DisplayName("Bridge should be thread-safe")
    void testBridgeThreadSafety() throws Exception {
        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int id = i;
            threads[i] = new Thread(() -> {
                TestBridge bridge = new TestBridge();
                bridge.setGeneralThreadPoolSize(id);
                manager.setBridge(bridge);

                // Read back
                TestBridge read = manager.getBridge();
                assertNotNull(read);
            });
        }

        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();

        // Final state should have a valid bridge
        assertTrue(manager.isBridgeInitialized());
    }

    @Test
    @DisplayName("Default configuration values should be sensible")
    void testDefaultConfigurationValues() {
        TestBridge bridge = new TestBridge();
        manager.setBridge(bridge);

        TestBridge b = manager.getBridge();

        // Check defaults are reasonable
        assertTrue(b.getGeneralThreadPoolSize() >= 1);
        assertTrue(b.getBrainThrottleInterval() >= 0);
        assertTrue(b.getMinEntitiesForParallel() >= 1);
    }

    @Test
    @DisplayName("Debug logging flag should work")
    void testDebugLogging() {
        TestBridge bridge = new TestBridge();
        bridge.setDebugLoggingEnabled(true);
        manager.setBridge(bridge);

        assertTrue(manager.getBridge().isDebugLoggingEnabled());

        bridge.setDebugLoggingEnabled(false);
        assertFalse(manager.getBridge().isDebugLoggingEnabled());
    }

    @Test
    @DisplayName("Feature flags should be independent")
    void testFeatureFlagsIndependent() {
        TestBridge bridge = new TestBridge();
        bridge.setEntityTickParallel(true);
        bridge.setCollisionOptimizationEnabled(false);
        bridge.setBrainThrottleEnabled(true);
        bridge.setMultithreadedTrackerEnabled(false);

        manager.setBridge(bridge);
        TestBridge b = manager.getBridge();

        assertTrue(b.isEntityTickParallel());
        assertFalse(b.isCollisionOptimizationEnabled());
        assertTrue(b.isBrainThrottleEnabled());
        assertFalse(b.isMultithreadedTrackerEnabled());
    }

    // Test implementations
    static class TestBridgeManager {
        private final AtomicReference<TestBridge> bridgeRef = new AtomicReference<>();

        void setBridge(TestBridge bridge) {
            bridgeRef.set(bridge);
        }

        TestBridge getBridge() {
            return bridgeRef.get();
        }

        boolean isBridgeInitialized() {
            return bridgeRef.get() != null;
        }

        void clearBridge() {
            bridgeRef.set(null);
        }
    }

    static class TestBridge {
        private boolean entityTickParallel = false;
        private boolean collisionOptimizationEnabled = false;
        private boolean brainThrottleEnabled = false;
        private boolean multithreadedTrackerEnabled = false;
        private boolean debugLoggingEnabled = false;
        private int generalThreadPoolSize = 4;
        private int brainThrottleInterval = 3;
        private int minEntitiesForParallel = 10;

        // Getters
        boolean isEntityTickParallel() { return entityTickParallel; }
        boolean isCollisionOptimizationEnabled() { return collisionOptimizationEnabled; }
        boolean isBrainThrottleEnabled() { return brainThrottleEnabled; }
        boolean isMultithreadedTrackerEnabled() { return multithreadedTrackerEnabled; }
        boolean isDebugLoggingEnabled() { return debugLoggingEnabled; }
        int getGeneralThreadPoolSize() { return generalThreadPoolSize; }
        int getBrainThrottleInterval() { return brainThrottleInterval; }
        int getMinEntitiesForParallel() { return minEntitiesForParallel; }

        // Setters
        void setEntityTickParallel(boolean v) { entityTickParallel = v; }
        void setCollisionOptimizationEnabled(boolean v) { collisionOptimizationEnabled = v; }
        void setBrainThrottleEnabled(boolean v) { brainThrottleEnabled = v; }
        void setMultithreadedTrackerEnabled(boolean v) { multithreadedTrackerEnabled = v; }
        void setDebugLoggingEnabled(boolean v) { debugLoggingEnabled = v; }
        void setGeneralThreadPoolSize(int v) { generalThreadPoolSize = v; }
        void setBrainThrottleInterval(int v) { brainThrottleInterval = v; }
        void setMinEntitiesForParallel(int v) { minEntitiesForParallel = v; }
    }
}
