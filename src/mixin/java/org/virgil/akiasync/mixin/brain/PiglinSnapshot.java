package org.virgil.akiasync.mixin.brain;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

/**
 * Piglin snapshot (1.21.8 specific)
 * 
 * Snapshot contents (main thread < 0.05 ms):
 * 1. inventory - Item list (copied ItemStack array)
 * 2. nearbyPlayers - Player positions + gold holding status within 16 blocks
 * 3. nearbyThreats - Soul fire blocks within 12 blocks
 * 4. isHunted - HUNTED_RECENTLY flag
 * 
 * @author Virgil
 */
public final class PiglinSnapshot {
    
    // Inventory snapshot (ItemStack array copy required for snapshot)
    private final ItemStack[] inventoryItems;
    
    // Nearby players snapshot (position + gold holding status only)
    private final java.util.List<PlayerGoldInfo> nearbyPlayers;
    
    // Nearby threats snapshot (soul fire blocks)
    private final java.util.List<net.minecraft.core.BlockPos> nearbyThreats;
    
    // Hunted flag (Boolean)
    private final boolean isHunted;
    
    private PiglinSnapshot(
            ItemStack[] inv,
            java.util.List<PlayerGoldInfo> players,
            java.util.List<net.minecraft.core.BlockPos> threats,
            boolean hunted
    ) {
        this.inventoryItems = inv;
        this.nearbyPlayers = players;
        this.nearbyThreats = threats;
        this.isHunted = hunted;
    }
    
    /**
     * Capture piglin snapshot (main thread invocation, full version)
     * 
     * @param piglin Piglin entity (has inventory)
     * @param level ServerLevel
     * @return Snapshot
     */
    public static PiglinSnapshot capture(Piglin piglin, ServerLevel level) {
        // 1. Copy inventory list (SimpleContainer → ItemStack[])
        SimpleContainer inv = piglin.getInventory();
        ItemStack[] items = new ItemStack[inv.getContainerSize()];
        for (int i = 0; i < items.length; i++) {
            items[i] = inv.getItem(i).copy();  // copy to avoid reference sharing
        }
        
        // 2. Scan players within 16 blocks (UUID + position + gold status)
        AABB scanBox = piglin.getBoundingBox().inflate(16.0);
        java.util.List<PlayerGoldInfo> players = level.getEntitiesOfClass(
            net.minecraft.world.entity.player.Player.class,
            scanBox
        ).stream()
            .map(player -> new PlayerGoldInfo(
                player.getUUID(),
                player.blockPosition(),
                isHoldingGold(player)
            ))
            .collect(java.util.stream.Collectors.toList());
        
        // 3. Scan threats within 12 blocks (soul fire blocks)
        java.util.List<net.minecraft.core.BlockPos> threats = new java.util.ArrayList<>();
        net.minecraft.core.BlockPos piglinPos = piglin.blockPosition();
        for (int x = -12; x <= 12; x++) {
            for (int y = -12; y <= 12; y++) {
                for (int z = -12; z <= 12; z++) {
                    net.minecraft.core.BlockPos pos = piglinPos.offset(x, y, z);
                    if (level.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.SOUL_FIRE)) {
                        threats.add(pos);
                    }
                }
            }
        }
        
        // 4. Read hunted flag (Boolean)
        boolean hunted = piglin.getBrain()
            .getMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.HUNTED_RECENTLY)
            .orElse(false);
        
        return new PiglinSnapshot(items, players, threats, hunted);
    }
    
    /**
     * Capture simplified snapshot (PiglinBrute specific, no inventory)
     * 
     * @param brute PiglinBrute entity (no inventory)
     * @param level ServerLevel
     * @return Snapshot
     */
    public static PiglinSnapshot captureSimple(
            net.minecraft.world.entity.monster.piglin.PiglinBrute brute,
            ServerLevel level
    ) {
        // 1. Empty inventory (brutes don't barter)
        ItemStack[] items = new ItemStack[0];
        
        // 2-4. Same logic for other aspects (scan players, threats, hunted flag)
        AABB scanBox = brute.getBoundingBox().inflate(16.0);
        java.util.List<PlayerGoldInfo> players = level.getEntitiesOfClass(
            net.minecraft.world.entity.player.Player.class,
            scanBox
        ).stream()
            .map(player -> new PlayerGoldInfo(
                player.getUUID(),
                player.blockPosition(),
                isHoldingGold(player)
            ))
            .collect(java.util.stream.Collectors.toList());
        
        java.util.List<net.minecraft.core.BlockPos> threats = new java.util.ArrayList<>();
        net.minecraft.core.BlockPos brutePos = brute.blockPosition();
        for (int x = -12; x <= 12; x++) {
            for (int y = -12; y <= 12; y++) {
                for (int z = -12; z <= 12; z++) {
                    net.minecraft.core.BlockPos pos = brutePos.offset(x, y, z);
                    if (level.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.SOUL_FIRE)) {
                        threats.add(pos);
                    }
                }
            }
        }
        
        boolean hunted = brute.getBrain()
            .getMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.HUNTED_RECENTLY)
            .orElse(false);
        
        return new PiglinSnapshot(items, players, threats, hunted);
    }
    
    /**
     * Check if player is holding gold items
     */
    private static boolean isHoldingGold(net.minecraft.world.entity.player.Player player) {
        ItemStack mainHand = player.getMainHandItem();
        return mainHand.is(net.minecraft.world.item.Items.GOLD_INGOT) ||
               mainHand.is(net.minecraft.world.item.Items.GOLD_BLOCK);
    }
    
    // Getters
    public ItemStack[] getInventoryItems() { return inventoryItems; }
    public java.util.List<PlayerGoldInfo> getNearbyPlayers() { return nearbyPlayers; }
    public java.util.List<net.minecraft.core.BlockPos> getNearbyThreats() { return nearbyThreats; }
    public boolean isHunted() { return isHunted; }
    
    /**
     * Player gold info (virtual reference: UUID + BlockPos)
     */
    public static class PlayerGoldInfo {
        final java.util.UUID playerId;
        final net.minecraft.core.BlockPos pos;
        final boolean holdingGold;
        
        public PlayerGoldInfo(java.util.UUID id, net.minecraft.core.BlockPos pos, boolean holdingGold) {
            this.playerId = id;
            this.pos = pos;
            this.holdingGold = holdingGold;
        }
        
        public java.util.UUID playerId() { return playerId; }
        public net.minecraft.core.BlockPos pos() { return pos; }
        public boolean holdingGold() { return holdingGold; }
    }
}

