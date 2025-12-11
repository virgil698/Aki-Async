package org.virgil.akiasync.mixin.brain.hoglin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.player.Player;
import org.virgil.akiasync.mixin.brain.core.AiQueryHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class HoglinSnapshot {
    
    private final double health;
    private final BlockPos position;
    private final boolean isBaby;
    private final List<PlayerInfo> nearbyPlayers;
    private final List<BlockPos> nearbyWarpedFungus;
    private final boolean isHuntingCooldown;
    private final long gameTime;
    
    private HoglinSnapshot(
        double health,
        BlockPos position,
        boolean isBaby,
        List<PlayerInfo> players,
        List<BlockPos> warpedFungus,
        boolean isHuntingCooldown,
        long gameTime
    ) {
        this.health = health;
        this.position = position;
        this.isBaby = isBaby;
        this.nearbyPlayers = players;
        this.nearbyWarpedFungus = warpedFungus;
        this.isHuntingCooldown = isHuntingCooldown;
        this.gameTime = gameTime;
    }
    
    public static HoglinSnapshot capture(Hoglin hoglin, ServerLevel level) {
        double health = hoglin.getHealth();
        BlockPos position = hoglin.blockPosition();
        boolean isBaby = hoglin.isBaby();
        long gameTime = level.getGameTime();
        
        List<PlayerInfo> players = new ArrayList<>();
        for (Player player : AiQueryHelper.getNearbyPlayers(hoglin, 16.0)) {
            players.add(new PlayerInfo(
                player.getUUID(),
                player.blockPosition(),
                player.distanceToSqr(hoglin)
            ));
        }
        
        List<BlockPos> warpedFungus = new ArrayList<>();
        BlockPos hoglinPos = hoglin.blockPosition();
        
        for (int x = -8; x <= 8; x += 2) {
            for (int y = -4; y <= 4; y += 2) {
                for (int z = -8; z <= 8; z += 2) {
                    BlockPos pos = hoglinPos.offset(x, y, z);
                    if (level.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.WARPED_FUNGUS)) {
                        warpedFungus.add(pos);
                        if (warpedFungus.size() >= 3) break;
                    }
                }
                if (warpedFungus.size() >= 3) break;
            }
            if (warpedFungus.size() >= 3) break;
        }
        
        boolean isHuntingCooldown = hoglin.isConverting(); 
        
        return new HoglinSnapshot(
            health, position, isBaby, players, warpedFungus, isHuntingCooldown, gameTime
        );
    }
    
    public double getHealth() { return health; }
    public BlockPos getPosition() { return position; }
    public boolean isBaby() { return isBaby; }
    public List<PlayerInfo> getNearbyPlayers() { return nearbyPlayers; }
    public List<BlockPos> getNearbyWarpedFungus() { return nearbyWarpedFungus; }
    public boolean isHuntingCooldown() { return isHuntingCooldown; }
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
