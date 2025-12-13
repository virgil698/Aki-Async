package org.virgil.akiasync.util;

import org.virgil.akiasync.constants.AkiAsyncConstants;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class ExecutorHealthChecker {

    private ExecutorHealthChecker() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static class HealthStatus {
        private final boolean healthy;
        private final String status;
        private final long responseTime;
        private final String details;

        public HealthStatus(boolean healthy, String status, long responseTime, String details) {
            this.healthy = healthy;
            this.status = status;
            this.responseTime = responseTime;
            this.details = details;
        }

        public boolean isHealthy() { return healthy; }
        public String getStatus() { return status; }
        public long getResponseTime() { return responseTime; }
        public String getDetails() { return details; }

        @Override
        public String toString() {
            return String.format("HealthStatus{healthy=%s, status='%s', responseTime=%dms, details='%s'}",
                healthy, status, responseTime, details);
        }
    }

    public static HealthStatus checkHealth(ExecutorService executor, String executorName) {
        if (executor == null) {
            return new HealthStatus(false, "NULL", 0, "Executor is null");
        }

        if (executor.isShutdown()) {
            return new HealthStatus(false, "SHUTDOWN", 0, "Executor is shutdown");
        }

        if (executor.isTerminated()) {
            return new HealthStatus(false, "TERMINATED", 0, "Executor is terminated");
        }

        long startTime = System.currentTimeMillis();
        AtomicBoolean testCompleted = new AtomicBoolean(false);
        AtomicLong testResponseTime = new AtomicLong(0);

        try {
            CompletableFuture<Boolean> testFuture = CompletableFuture.supplyAsync(() -> {
                testResponseTime.set(System.currentTimeMillis() - startTime);
                testCompleted.set(true);
                return true;
            }, executor);

            Boolean result = testFuture.get(AkiAsyncConstants.Threading.EXECUTOR_TEST_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            long responseTime = testResponseTime.get();

            if (result && testCompleted.get()) {
                String status = getPerformanceStatus(responseTime);
                return new HealthStatus(true, status, responseTime,
                    String.format("%s executor is healthy (response: %dms)", executorName, responseTime));
            } else {
                return new HealthStatus(false, "TEST_FAILED", responseTime,
                    "Health test returned unexpected result");
            }

        } catch (java.util.concurrent.TimeoutException e) {
            return new HealthStatus(false, "TIMEOUT", AkiAsyncConstants.Threading.EXECUTOR_TEST_TIMEOUT_MS,
                String.format("%s executor response timeout (>%dms)", executorName, AkiAsyncConstants.Threading.EXECUTOR_TEST_TIMEOUT_MS));
        } catch (Exception e) {
            return new HealthStatus(false, "ERROR", System.currentTimeMillis() - startTime,
                String.format("%s executor test failed: %s", executorName, e.getMessage()));
        }
    }

    private static String getPerformanceStatus(long responseTime) {
        if (responseTime < 10) {
            return "EXCELLENT";
        } else if (responseTime < 50) {
            return "GOOD";
        } else if (responseTime < 100) {
            return "FAIR";
        } else {
            return "SLOW";
        }
    }

    public static java.util.Map<String, HealthStatus> checkMultipleExecutors(java.util.Map<String, ExecutorService> executors) {
        java.util.Map<String, HealthStatus> results = new java.util.concurrent.ConcurrentHashMap<>();

        executors.entrySet().parallelStream().forEach(entry -> {
            String name = entry.getKey();
            ExecutorService executor = entry.getValue();
            HealthStatus status = checkHealth(executor, name);
            results.put(name, status);
        });

        return results;
    }

    public static String generateHealthReport(java.util.Map<String, HealthStatus> healthStatuses) {
        StringBuilder report = new StringBuilder();
        report.append("=== Executor Health Report ===\n");

        int healthy = 0;
        int total = healthStatuses.size();
        long totalResponseTime = 0;

        for (java.util.Map.Entry<String, HealthStatus> entry : healthStatuses.entrySet()) {
            String name = entry.getKey();
            HealthStatus status = entry.getValue();

            report.append(String.format("%-20s: %s%n", name, status.toString()));

            if (status.isHealthy()) {
                healthy++;
                totalResponseTime += status.getResponseTime();
            }
        }

        report.append(String.format("%nSummary: %d/%d healthy", healthy, total));
        if (healthy > 0) {
            report.append(String.format(", avg response: %.1fms", (double) totalResponseTime / healthy));
        }

        return report.toString();
    }

    public static boolean safeShutdown(ExecutorService executor, String executorName, long timeoutMs) {
        if (executor == null || executor.isShutdown()) {
            return true;
        }

        org.virgil.akiasync.mixin.util.ExceptionHandler.logDebug("Shutting down executor: " + executorName);

        executor.shutdown();

        try {
            if (!executor.awaitTermination(timeoutMs, TimeUnit.MILLISECONDS)) {
                org.virgil.akiasync.mixin.util.ExceptionHandler.logDebug("Executor " + executorName + " did not terminate gracefully, forcing shutdown");
                executor.shutdownNow();

                if (!executor.awaitTermination(timeoutMs, TimeUnit.MILLISECONDS)) {
                    org.virgil.akiasync.mixin.util.ExceptionHandler.logError("Executor " + executorName + " did not terminate after forced shutdown", null);
                    return false;
                }
            }
            org.virgil.akiasync.mixin.util.ExceptionHandler.logDebug("Executor " + executorName + " shutdown successfully");
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
            org.virgil.akiasync.mixin.util.ExceptionHandler.logError("Interrupted while shutting down executor " + executorName, e);
            return false;
        }
    }
}
