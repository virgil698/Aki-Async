package org.virgil.akiasync.executor;

import java.util.Locale;

/**
 * Enumeration for thread pool calculation strategies.
 * Replaces stringly-typed configuration to prevent typos and improve type safety.
 */
public enum ThreadCalculationStrategy {
    
    CORES_DIV_2("cores/2") {
        @Override
        public int calculate(int cores) {
            return Math.max(1, cores / 2);
        }
    },
    
    CORES_DIV_3("cores/3") {
        @Override
        public int calculate(int cores) {
            return Math.max(1, cores / 3);
        }
    },
    
    CORES_DIV_4("cores/4") {
        @Override
        public int calculate(int cores) {
            return Math.max(1, cores / 4);
        }
    };
    
    private final String configValue;
    
    ThreadCalculationStrategy(String configValue) {
        this.configValue = configValue;
    }
    
    /**
     * Calculate the number of threads based on available cores.
     * @param cores Number of available processor cores
     * @return Calculated thread count
     */
    public abstract int calculate(int cores);
    
    /**
     * Get the configuration string value for this strategy.
     * @return Config value string
     */
    public String getConfigValue() {
        return configValue;
    }
    
    /**
     * Parse a configuration string to a ThreadCalculationStrategy.
     * Handles common typos and variations (case-insensitive, trims whitespace).
     * 
     * @param value The configuration string
     * @return The matching strategy, or null if not found
     */
    public static ThreadCalculationStrategy fromConfig(String value) {
        if (value == null) {
            return null;
        }
        
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        
        for (ThreadCalculationStrategy strategy : values()) {
            if (strategy.configValue.equals(normalized)) {
                return strategy;
            }
        }
        
        // Handle common variations/typos
        if (normalized.matches("cores\\s*/\\s*2") || normalized.equals("cores / 2")) {
            return CORES_DIV_2;
        }
        if (normalized.matches("cores\\s*/\\s*3") || normalized.equals("cores / 3")) {
            return CORES_DIV_3;
        }
        if (normalized.matches("cores\\s*/\\s*4") || normalized.equals("cores / 4")) {
            return CORES_DIV_4;
        }
        
        return null;
    }
    
    /**
     * Get the default strategy.
     * @return Default strategy (CORES_DIV_3)
     */
    public static ThreadCalculationStrategy getDefault() {
        return CORES_DIV_3;
    }
}
