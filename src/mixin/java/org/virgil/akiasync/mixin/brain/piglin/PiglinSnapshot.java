package org.virgil.akiasync.mixin.brain.piglin;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
public final class PiglinSnapshot {
    private final ItemStack[] inventoryItems;
    private final java.util.List<PlayerGoldInfo> nearbyPlayers;
    private final java.util.List<net.minecraft.core.BlockPos> nearbyThreats;
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
    public static PiglinSnapshot capture(Piglin piglin, ServerLevel level) {
        SimpleContainer inv = piglin.getInventory();
        ItemStack[] items = new ItemStack[inv.getContainerSize()];
        for (int i = 0; i < items.length; i++) {
            items[i] = inv.getItem(i).copy();
        }
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
        boolean hunted = piglin.getBrain()
            .getMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.HUNTED_RECENTLY)
            .orElse(false);
        return new PiglinSnapshot(items, players, threats, hunted);
    }
    public static PiglinSnapshot captureSimple(
            net.minecraft.world.entity.monster.piglin.PiglinBrute brute,
            ServerLevel level
    ) {
        ItemStack[] items = new ItemStack[0];
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
    private static boolean isHoldingGold(net.minecraft.world.entity.player.Player player) {
        ItemStack mainHand = player.getMainHandItem();
        return mainHand.is(net.minecraft.world.item.Items.GOLD_INGOT) ||
               mainHand.is(net.minecraft.world.item.Items.GOLD_BLOCK);
    }
    public ItemStack[] getInventoryItems() { return inventoryItems; }
    public java.util.List<PlayerGoldInfo> getNearbyPlayers() { return nearbyPlayers; }
    public java.util.List<net.minecraft.core.BlockPos> getNearbyThreats() { return nearbyThreats; }
    public boolean isHunted() { return isHunted; }
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
