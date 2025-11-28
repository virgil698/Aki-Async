package org.virgil.akiasync.util.resource;

public interface CacheInvalidator {

    void invalidate();

    void invalidateAll();

    boolean isValid();
}
