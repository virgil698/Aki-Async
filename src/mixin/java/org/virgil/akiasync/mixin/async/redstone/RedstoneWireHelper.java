package org.virgil.akiasync.mixin.async.redstone;

import net.minecraft.server.level.ServerLevel;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RedstoneWireHelper {
    
    private static final Map<ServerLevel, PandaWireEvaluator> evaluatorCache = new ConcurrentHashMap<>();
    
    public static PandaWireEvaluator getOrCreateEvaluator(ServerLevel level, 
                                                          net.minecraft.world.level.block.RedStoneWireBlock wireBlock) {
        return evaluatorCache.computeIfAbsent(level, l -> new PandaWireEvaluator(l, wireBlock));
    }
    
    public static void clearCache(ServerLevel level) {
        evaluatorCache.remove(level);
    }
    
    public static void clearAllCaches() {
        evaluatorCache.clear();
    }
    
    public static void clearLevelCache(ServerLevel level) {
        clearCache(level);
    }
    
    public static int getEvaluatorCount() {
        return evaluatorCache.size();
    }
}
