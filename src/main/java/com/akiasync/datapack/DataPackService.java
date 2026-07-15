package com.akiasync.datapack;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public final class DataPackService {
    private final AtomicReference<DataPackRecord> latest = new AtomicReference<>();

    public void accept(DataPackRecord record) {
        latest.set(record);
    }

    public Optional<DataPackRecord> latest() {
        return Optional.ofNullable(latest.get());
    }

    public void clear() {
        latest.set(null);
    }
}
