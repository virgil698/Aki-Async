package org.virgil.akiasync.mixin.util;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.world.entity.Entity;
import java.util.List;

public final class SpatialGrid {
    public final Long2ObjectOpenHashMap<List<Entity>> cells = new Long2ObjectOpenHashMap<>();
    public int entityCount = 0;
}
