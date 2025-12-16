package org.virgil.akiasync.mixin.brain.sniffer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class SnifferSnapshot {
    
    private final Vec3 position;
    private final BlockPos blockPosition;
    private final double health;
    private final double maxHealth;
    private final boolean isInWater;
    private final boolean onGround;
    private final long gameTime;
    private final int tickCount;
    private final boolean isBaby;
    private final List<BlockInfo> nearbyDirtBlocks;
    private final List<PlayerInfo> nearbyPlayers;
    
    private SnifferSnapshot(Vec3 position, BlockPos blockPosition, double health, double maxHealth,
                           boolean isInWater, boolean onGround, long gameTime, int tickCount,
                           boolean isBaby, List<BlockInfo> nearbyDirtBlocks,
                           List<PlayerInfo> nearbyPlayers) {
        this.position = position;
        this.blockPosition = blockPosition;
        this.health = health;
        this.maxHealth = maxHealth;
        this.isInWater = isInWater;
        this.onGround = onGround;
        this.gameTime = gameTime;
        this.tickCount = tickCount;
        this.isBaby = isBaby;
        this.nearbyDirtBlocks = nearbyDirtBlocks;
        this.nearbyPlayers = nearbyPlayers;
    }
    
    public static SnifferSnapshot capture(net.minecraft.world.entity.animal.Animal sniffer, 
                                         ServerLevel level, int tickCount) {
        Vec3 pos = sniffer.position();
        BlockPos blockPos = sniffer.blockPosition();
        double health = sniffer.getHealth();
        double maxHealth = sniffer.getMaxHealth();
        boolean isInWater = sniffer.isInWater();
        boolean onGround = sniffer.onGround();
        long gameTime = level.getGameTime();
        boolean isBaby = sniffer.isBaby();
        
        List<BlockInfo> dirtBlocks = new ArrayList<>();
        for (int x = -8; x <= 8; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -8; z <= 8; z++) {
                    BlockPos checkPos = blockPos.offset(x, y, z);
                    BlockState state = level.getBlockState(checkPos);
                    
                    if (state.is(net.minecraft.tags.BlockTags.DIRT) || 
                        state.is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)) {
                        dirtBlocks.add(new BlockInfo(checkPos, state.toString()));
                    }
                }
            }
        }
        
        List<PlayerInfo> players = new ArrayList<>();
        List<net.minecraft.world.entity.player.Player> nearbyPlayers = 
            level.getEntitiesOfClass(
                net.minecraft.world.entity.player.Player.class,
                sniffer.getBoundingBox().inflate(16.0),
                p -> !p.isSpectator() && p.isAlive()
            );
        
        for (net.minecraft.world.entity.player.Player player : nearbyPlayers) {
            players.add(new PlayerInfo(
                player.getUUID(),
                player.position(),
                sniffer.distanceToSqr(player)
            ));
        }
        
        return new SnifferSnapshot(pos, blockPos, health, maxHealth, isInWater, onGround,
                                  gameTime, tickCount, isBaby, dirtBlocks, players);
    }
    
    public Vec3 position() { return position; }
    public BlockPos blockPosition() { return blockPosition; }
    public double health() { return health; }
    public double maxHealth() { return maxHealth; }
    public boolean isInWater() { return isInWater; }
    public boolean onGround() { return onGround; }
    public long gameTime() { return gameTime; }
    public int tickCount() { return tickCount; }
    public boolean isBaby() { return isBaby; }
    public List<BlockInfo> nearbyDirtBlocks() { return nearbyDirtBlocks; }
    public List<PlayerInfo> nearbyPlayers() { return nearbyPlayers; }
    
    public record BlockInfo(BlockPos pos, String blockType) {}
    public record PlayerInfo(UUID id, Vec3 pos, double distanceSq) {}
}
