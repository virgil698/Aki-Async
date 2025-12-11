package org.virgil.akiasync.mixin.brain.allay;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.virgil.akiasync.mixin.brain.core.AiQueryHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class AllaySnapshot {
    
    private final double health;
    private final BlockPos position;
    private final ItemStack heldItem;
    private final List<PlayerInfo> nearbyPlayers;
    private final List<ItemEntityInfo> nearbyItems;
    private final BlockPos nearestNoteBlock;
    private final boolean isDancing;
    private final long gameTime;
    
    private AllaySnapshot(
        double health,
        BlockPos position,
        ItemStack heldItem,
        List<PlayerInfo> players,
        List<ItemEntityInfo> items,
        BlockPos nearestNoteBlock,
        boolean isDancing,
        long gameTime
    ) {
        this.health = health;
        this.position = position;
        this.heldItem = heldItem;
        this.nearbyPlayers = players;
        this.nearbyItems = items;
        this.nearestNoteBlock = nearestNoteBlock;
        this.isDancing = isDancing;
        this.gameTime = gameTime;
    }
    
    public static AllaySnapshot capture(Allay allay, ServerLevel level) {
        double health = allay.getHealth();
        BlockPos position = allay.blockPosition();
        ItemStack heldItem = allay.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND).copy();
        long gameTime = level.getGameTime();
        
        List<PlayerInfo> players = new ArrayList<>();
        for (Player player : AiQueryHelper.getNearbyPlayers(allay, 32.0)) {
            players.add(new PlayerInfo(
                player.getUUID(),
                player.blockPosition(),
                player.distanceToSqr(allay)
            ));
        }
        
        List<ItemEntityInfo> items = new ArrayList<>();
        
        BlockPos nearestNoteBlock = findNearestNoteBlock(allay, level);
        
        boolean isDancing = allay.isDancing();
        
        return new AllaySnapshot(
            health, position, heldItem, players, items, nearestNoteBlock, isDancing, gameTime
        );
    }
    
    private static BlockPos findNearestNoteBlock(Allay allay, ServerLevel level) {
        BlockPos allayPos = allay.blockPosition();
        BlockPos nearest = null;
        double minDist = Double.MAX_VALUE;
        
        for (int x = -16; x <= 16; x += 2) {
            for (int y = -8; y <= 8; y += 2) {
                for (int z = -16; z <= 16; z += 2) {
                    BlockPos pos = allayPos.offset(x, y, z);
                    if (level.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.NOTE_BLOCK)) {
                        double dist = allayPos.distSqr(pos);
                        if (dist < minDist) {
                            minDist = dist;
                            nearest = pos;
                        }
                    }
                }
            }
        }
        
        return nearest;
    }
    
    public double getHealth() { return health; }
    public BlockPos getPosition() { return position; }
    public ItemStack getHeldItem() { return heldItem; }
    public List<PlayerInfo> getNearbyPlayers() { return nearbyPlayers; }
    public List<ItemEntityInfo> getNearbyItems() { return nearbyItems; }
    public BlockPos getNearestNoteBlock() { return nearestNoteBlock; }
    public boolean isDancing() { return isDancing; }
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
    
    public static class ItemEntityInfo {
        private final UUID itemEntityId;
        private final BlockPos position;
        private final double distanceSq;
        
        public ItemEntityInfo(UUID itemEntityId, BlockPos position, double distanceSq) {
            this.itemEntityId = itemEntityId;
            this.position = position;
            this.distanceSq = distanceSq;
        }
        
        public UUID getItemEntityId() { return itemEntityId; }
        public BlockPos getPosition() { return position; }
        public double getDistanceSq() { return distanceSq; }
    }
}
