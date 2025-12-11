package org.virgil.akiasync.mixin.brain.wanderingtrader;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import org.virgil.akiasync.mixin.brain.core.AiQueryHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class WanderingTraderSnapshot {
    
    private final double health;
    private final BlockPos position;
    private final List<PlayerInfo> nearbyPlayers;
    private final Map<BlockPos, PoiRecord> nearbyPOIs;
    private final int despawnDelay;
    private final boolean hasCustomer;
    private final long gameTime;
    
    private WanderingTraderSnapshot(
        double health,
        BlockPos position,
        List<PlayerInfo> players,
        Map<BlockPos, PoiRecord> pois,
        int despawnDelay,
        boolean hasCustomer,
        long gameTime
    ) {
        this.health = health;
        this.position = position;
        this.nearbyPlayers = players;
        this.nearbyPOIs = pois;
        this.despawnDelay = despawnDelay;
        this.hasCustomer = hasCustomer;
        this.gameTime = gameTime;
    }
    
    public static WanderingTraderSnapshot capture(WanderingTrader trader, ServerLevel level) {
        double health = trader.getHealth();
        BlockPos position = trader.blockPosition();
        long gameTime = level.getGameTime();
        
        List<PlayerInfo> players = new ArrayList<>();
        for (Player player : AiQueryHelper.getNearbyPlayers(trader, 48.0)) {
            players.add(new PlayerInfo(
                player.getUUID(),
                player.blockPosition(),
                player.distanceToSqr(trader)
            ));
        }
        
        Map<BlockPos, PoiRecord> pois = new HashMap<>();
        List<PoiRecord> poiList = AiQueryHelper.getNearbyPoi(trader, 48);
        for (PoiRecord record : poiList) {
            pois.put(record.getPos(), record);
        }
        
        int despawnDelay = trader.getDespawnDelay();
        
        boolean hasCustomer = trader.getTradingPlayer() != null;
        
        return new WanderingTraderSnapshot(
            health, position, players, pois, despawnDelay, hasCustomer, gameTime
        );
    }
    
    public double getHealth() { return health; }
    public BlockPos getPosition() { return position; }
    public List<PlayerInfo> getNearbyPlayers() { return nearbyPlayers; }
    public Map<BlockPos, PoiRecord> getNearbyPOIs() { return nearbyPOIs; }
    public int getDespawnDelay() { return despawnDelay; }
    public boolean hasCustomer() { return hasCustomer; }
    public long getGameTime() { return gameTime; }
    
    public static class PlayerInfo {
        private final UUID playerId;
        private final BlockPos position;
        private final double distanceSq;
        
        public PlayerInfo(UUID playerId, BlockPos position, double distanceSq) {
            this.playerId = playerId;
            this.position = position;
            this.distanceSq = distanceSq;
        }
        
        public UUID getPlayerId() { return playerId; }
        public BlockPos getPosition() { return position; }
        public double getDistanceSq() { return distanceSq; }
    }
}
