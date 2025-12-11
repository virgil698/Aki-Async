package org.virgil.akiasync.mixin.brain.universal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.ExpirableValue;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import org.virgil.akiasync.mixin.brain.core.AiQueryHelper;

public final class UniversalAiSnapshot {
    
    private final double health;
    private final double level;
    private final List<PlayerInfo> nearbyPlayers;
    private final List<MobInfo> nearbyMobs;
    
    private final Map<MemoryModuleType<?>, MemoryEntry<?>> brainMemories;
    private final long gameTime;
    
    private final Map<BlockPos, PoiRecord> nearbyPOIs;
    
    private UniversalAiSnapshot(double health, double level, List<PlayerInfo> players,
                                List<MobInfo> mobs, 
                                Map<MemoryModuleType<?>, MemoryEntry<?>> brainMem,
                                long gameTime,
                                Map<BlockPos, PoiRecord> pois) {
        this.health = health;
        this.level = level;
        this.nearbyPlayers = players;
        this.nearbyMobs = mobs;
        this.brainMemories = brainMem != null ? Collections.unmodifiableMap(brainMem) : null;
        this.gameTime = gameTime;
        this.nearbyPOIs = pois != null ? Collections.unmodifiableMap(pois) : null;
    }
    
    public static UniversalAiSnapshot capture(Mob mob, ServerLevel world, 
                                             boolean captureBrainMemory,
                                             boolean capturePoiSnapshot) {
        double health = mob.getHealth();
        double yLevel = mob.position().y;
        long gameTime = world.getGameTime();
        
        List<PlayerInfo> players = AiQueryHelper.getNearbyPlayers(mob, 32.0)
            .stream()
            .map(p -> new PlayerInfo(p.getUUID(), p.blockPosition()))
            .collect(Collectors.toList());
        
        List<MobInfo> mobs = AiQueryHelper.getNearbyEntities(mob, Mob.class, 16.0)
            .stream()
            .map(m -> new MobInfo(m.blockPosition()))
            .collect(Collectors.toList());
        
        Map<MemoryModuleType<?>, MemoryEntry<?>> brainMem = null;
        if (captureBrainMemory && mob.getBrain() != null) {
            brainMem = captureBrainMemories(mob.getBrain(), world);
        }
        
        Map<BlockPos, PoiRecord> pois = null;
        if (capturePoiSnapshot) {
            List<PoiRecord> poiList = AiQueryHelper.getNearbyPoi(mob, 48);
            pois = new HashMap<>();
            for (PoiRecord record : poiList) {
                pois.put(record.getPos(), record);
            }
        }
        
        return new UniversalAiSnapshot(health, yLevel, players, mobs, brainMem, gameTime, pois);
    }
    
    public static UniversalAiSnapshot capture(Mob mob, ServerLevel world) {
        return capture(mob, world, false, false);
    }
    
    private static Map<MemoryModuleType<?>, MemoryEntry<?>> captureBrainMemories(
        Brain<?> brain, ServerLevel level
    ) {
        Map<MemoryModuleType<?>, MemoryEntry<?>> copy = new HashMap<>();
        
        for (Map.Entry<MemoryModuleType<?>, ? extends Optional<?>> entry : brain.getMemories().entrySet()) {
            MemoryModuleType<?> memoryType = entry.getKey();
            Optional<?> optValue = entry.getValue();
            
            if (optValue != null && optValue.isPresent()) {
                Object rawValue = optValue.get();
                
                if (rawValue instanceof ExpirableValue<?>) {
                    ExpirableValue<?> expirable = (ExpirableValue<?>) rawValue;
                    Object innerValue = expirable.getValue();
                    long ttl = expirable.getTimeToLive();
                    
                    MemoryEntry<?> entry2 = new MemoryEntry<>(innerValue, ttl, true);
                    copy.put(memoryType, entry2);
                } else {
                    MemoryEntry<?> entry2 = new MemoryEntry<>(rawValue, 0, false);
                    copy.put(memoryType, entry2);
                }
            }
        }
        
        return copy;
    }
    
    public double health() { return health; }
    public double level() { return level; }
    public List<PlayerInfo> players() { return nearbyPlayers; }
    public List<MobInfo> mobs() { return nearbyMobs; }
    public long gameTime() { return gameTime; }
    
    public boolean hasBrainMemories() { return brainMemories != null && !brainMemories.isEmpty(); }
    public Map<MemoryModuleType<?>, MemoryEntry<?>> getBrainMemories() { return brainMemories; }
    
    public boolean hasPOIs() { return nearbyPOIs != null && !nearbyPOIs.isEmpty(); }
    public Map<BlockPos, PoiRecord> getPOIs() { return nearbyPOIs; }
    
    public static class PlayerInfo {
        final UUID id; final BlockPos pos;
        public PlayerInfo(UUID id, BlockPos pos) { this.id = id; this.pos = pos; }
        public UUID id() { return id; }
        public BlockPos pos() { return pos; }
    }
    
    public static class MobInfo {
        final BlockPos pos;
        public MobInfo(BlockPos pos) { this.pos = pos; }
        public BlockPos pos() { return pos; }
    }
    
    public static final class MemoryEntry<U> {
        private final U value;
        private final long ttl;
        private final boolean isExpirable;
        
        MemoryEntry(U value, long ttl, boolean isExpirable) {
            this.value = value;
            this.ttl = ttl;
            this.isExpirable = isExpirable;
        }
        
        public U getValue() { return value; }
        public long getTtl() { return ttl; }
        public boolean isExpirable() { return isExpirable; }
    }
}
