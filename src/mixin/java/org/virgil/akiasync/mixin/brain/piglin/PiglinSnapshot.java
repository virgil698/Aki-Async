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
        int size = inv.getContainerSize();
        ItemStack[] items = new ItemStack[size];
        for (int i = 0; i < size; i++) {
            ItemStack item = inv.getItem(i);
            items[i] = item.isEmpty() ? ItemStack.EMPTY : item.copy();
        }
        
        java.util.List<PlayerGoldInfo> players = new java.util.ArrayList<>();
        
        for (net.minecraft.world.entity.player.Player player : 
            org.virgil.akiasync.mixin.brain.core.AiQueryHelper.getNearbyPlayers(piglin, 12.0)) {
            players.add(new PlayerGoldInfo(
                player.getUUID(),
                player.blockPosition(),
                isHoldingGold(player),
                isWearingGold(player)
            ));
        }
        
        java.util.List<net.minecraft.core.BlockPos> threats = new java.util.ArrayList<>(4);
        net.minecraft.core.BlockPos piglinPos = piglin.blockPosition();
        
        for (int x = -8; x <= 8; x += 2) {
            for (int y = -4; y <= 4; y += 2) {
                for (int z = -8; z <= 8; z += 2) {
                    net.minecraft.core.BlockPos pos = piglinPos.offset(x, y, z);
                    if (level.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.SOUL_FIRE)) {
                        threats.add(pos);
                        
                        if (threats.size() >= 3) {
                            break;
                        }
                    }
                }
                if (threats.size() >= 3) break;
            }
            if (threats.size() >= 3) break;
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
        
        java.util.List<PlayerGoldInfo> players = new java.util.ArrayList<>();
        
        for (net.minecraft.world.entity.player.Player player : 
            org.virgil.akiasync.mixin.brain.core.AiQueryHelper.getNearbyPlayers(
                (net.minecraft.world.entity.Mob) brute, 12.0)) {
            players.add(new PlayerGoldInfo(
                player.getUUID(),
                player.blockPosition(),
                isHoldingGold(player),
                isWearingGold(player)
            ));
        }
        
        java.util.List<net.minecraft.core.BlockPos> threats = new java.util.ArrayList<>(4);
        net.minecraft.core.BlockPos brutePos = brute.blockPosition();
        
        for (int x = -8; x <= 8; x += 2) {
            for (int y = -4; y <= 4; y += 2) {
                for (int z = -8; z <= 8; z += 2) {
                    net.minecraft.core.BlockPos pos = brutePos.offset(x, y, z);
                    if (level.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.SOUL_FIRE)) {
                        threats.add(pos);
                        if (threats.size() >= 3) break;
                    }
                }
                if (threats.size() >= 3) break;
            }
            if (threats.size() >= 3) break;
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
    
    private static boolean isWearingGold(net.minecraft.world.entity.player.Player player) {
        for (net.minecraft.world.entity.EquipmentSlot slot : 
             new net.minecraft.world.entity.EquipmentSlot[]{
                 net.minecraft.world.entity.EquipmentSlot.HEAD,
                 net.minecraft.world.entity.EquipmentSlot.CHEST,
                 net.minecraft.world.entity.EquipmentSlot.LEGS,
                 net.minecraft.world.entity.EquipmentSlot.FEET
             }) {
            ItemStack armorItem = player.getItemBySlot(slot);
            if (!armorItem.isEmpty() && armorItem.is(net.minecraft.tags.ItemTags.PIGLIN_LOVED)) {
                return true;
            }
        }
        return false;
    }
    public ItemStack[] getInventoryItems() { return inventoryItems; }
    public java.util.List<PlayerGoldInfo> getNearbyPlayers() { return nearbyPlayers; }
    public java.util.List<net.minecraft.core.BlockPos> getNearbyThreats() { return nearbyThreats; }
    public boolean isHunted() { return isHunted; }
    public static class PlayerGoldInfo {
        final java.util.UUID playerId;
        final net.minecraft.core.BlockPos pos;
        final boolean holdingGold;
        final boolean wearingGold;
        public PlayerGoldInfo(java.util.UUID id, net.minecraft.core.BlockPos pos, boolean holdingGold, boolean wearingGold) {
            this.playerId = id;
            this.pos = pos;
            this.holdingGold = holdingGold;
            this.wearingGold = wearingGold;
        }
        public java.util.UUID playerId() { return playerId; }
        public net.minecraft.core.BlockPos pos() { return pos; }
        public boolean holdingGold() { return holdingGold; }
        public boolean wearingGold() { return wearingGold; }
    }
}
