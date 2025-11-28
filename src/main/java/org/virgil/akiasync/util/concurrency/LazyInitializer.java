package org.virgil.akiasync.util.concurrency;

import javax.annotation.Nonnull;
import java.util.function.Supplier;

public final class LazyInitializer<T> {

    private volatile T value;
    private final Supplier<T> initializer;
    private final Object lock = new Object();

    public LazyInitializer(@Nonnull Supplier<T> initializer) {
        if (initializer == null) {
            throw new IllegalArgumentException("Initializer cannot be null");
        }
        this.initializer = initializer;
    }

    @Nonnull
    public T get() {
        
        T result = value;
        if (result != null) {
            return result;
        }

        synchronized (lock) {
            
            result = value;
            if (result == null) {
                result = initializer.get();
                value = result;
            }
            return result;
        }
    }

    public void reset() {
        synchronized (lock) {
            value = null;
        }
    }

    public boolean isInitialized() {
        return value != null;
    }

    public T getIfInitialized() {
        return value;
    }
}
