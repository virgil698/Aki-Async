package org.virgil.akiasync.mixin.util;

import net.minecraft.world.phys.BlockHitResult;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class RayTraceCacheHolder {
    private final Map<Long, BlockHitResult> cache;
    private final LinkedList<Long> accessOrder;
    private final int cacheSize;

    public RayTraceCacheHolder(int cacheSize) {
        this.cacheSize = cacheSize;
        this.cache = new HashMap<Long, BlockHitResult>();
        this.accessOrder = new LinkedList<Long>();
    }

    public BlockHitResult get(Long key) {
        return cache.get(key);
    }

    public void put(Long key, BlockHitResult value) {
        if (!cache.containsKey(key)) {
            if (cache.size() >= cacheSize) {
                Long oldest = accessOrder.removeFirst();
                cache.remove(oldest);
            }
            accessOrder.addLast(key);
        }
        cache.put(key, value);
    }
}
