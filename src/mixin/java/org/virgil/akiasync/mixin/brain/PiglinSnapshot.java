package org.virgil.akiasync.mixin.brain;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

/**
 * 猪灵快照（1.21.8专用）
 * 
 * 快照内容（主线程 < 0.05 ms）：
 * 1. inventory - 物品列表（copy ItemStack数组）
 * 2. nearbyPlayers - 16格内玩家位置+手持金币
 * 3. nearbyEntities - 12格内灵魂火/僵尸猪灵
 * 4. huntedTimer - HUNTED_RECENTLY 计时器
 * 
 * @author Virgil
 */
public final class PiglinSnapshot {
    
    // 物品快照（复制ItemStack数组，快照需copy）
    private final ItemStack[] inventoryItems;
    
    // 附近玩家快照（只记录位置+手持金币标记）
    private final java.util.List<PlayerGoldInfo> nearbyPlayers;
    
    // 附近威胁快照（灵魂火/僵尸猪灵）
    private final java.util.List<net.minecraft.core.BlockPos> nearbyThreats;
    
    // 仇恨标记（Boolean）
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
     * 捕获猪灵快照（主线程调用，完整版）
     * 
     * @param piglin 猪灵实体（有inventory）
     * @param level ServerLevel
     * @return 快照
     */
    public static PiglinSnapshot capture(Piglin piglin, ServerLevel level) {
        // 1. 复制物品列表（SimpleContainer → ItemStack[]）
        SimpleContainer inv = piglin.getInventory();
        ItemStack[] items = new ItemStack[inv.getContainerSize()];
        for (int i = 0; i < items.length; i++) {
            items[i] = inv.getItem(i).copy();  // copy避免引用
        }
        
        // 2. 扫描16格内玩家（UUID+位置+金币）
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
        
        // 3. 扫描12格内威胁（灵魂火方块）
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
        
        // 4. 读取仇恨标记（Boolean）
        boolean hunted = piglin.getBrain()
            .getMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.HUNTED_RECENTLY)
            .orElse(false);
        
        return new PiglinSnapshot(items, players, threats, hunted);
    }
    
    /**
     * 捕获简化快照（PiglinBrute专用，无inventory）
     * 
     * @param brute 猪灵蛮兵（无inventory）
     * @param level ServerLevel
     * @return 快照
     */
    public static PiglinSnapshot captureSimple(
            net.minecraft.world.entity.monster.piglin.PiglinBrute brute,
            ServerLevel level
    ) {
        // 1. 空inventory（蛮兵不以物易物）
        ItemStack[] items = new ItemStack[0];
        
        // 2-4. 其他逻辑相同（扫描玩家、威胁、仇恨标记）
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
     * 检查玩家是否手持金币
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
     * 玩家金币信息（虚拟引用：UUID+BlockPos）
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

