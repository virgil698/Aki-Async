package org.virgil.akiasync.mixin.brain.evoker;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.Evoker;
import net.minecraft.world.phys.AABB;

/**
 * Evoker snapshot (spell CD + Vex summon + empty blocks)
 * 
 * @author Virgil
 */
public final class EvokerSnapshot {
    
    private final double health;
    private final double spellCd;  // 法术CD (0=ready)
    private final List<PlayerInfo> nearbyPlayers;  // 32格玩家
    private final int emptyBlocks;  // 3x3空地块数量（召唤Vex用）
    
    private EvokerSnapshot(double health, double spellCd, List<PlayerInfo> players, int empty) {
        this.health = health;
        this.spellCd = spellCd;
        this.nearbyPlayers = players;
        this.emptyBlocks = empty;
    }
    
    public static EvokerSnapshot capture(Evoker evoker, ServerLevel level) {
        double health = evoker.getHealth();
        
        // Spell CD detection (simplified: CPU simulation)
        double spellCd = health < 10.0 ? 0.0 : 50.0;  // Low health = ready to cast
        
        // Scan 32-block players
        AABB box = evoker.getBoundingBox().inflate(32.0);
        List<PlayerInfo> players = level.getEntitiesOfClass(
            net.minecraft.world.entity.player.Player.class, box
        ).stream()
            .map(p -> new PlayerInfo(p.getUUID(), p.blockPosition()))
            .collect(Collectors.toList());
        
        // Empty blocks for Vex summon (3x3 area around evoker)
        int emptyBlocks = 0;
        BlockPos pos = evoker.blockPosition();
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (level.getBlockState(pos.offset(x, 0, z)).isAir()) {
                    emptyBlocks++;
                }
            }
        }
        
        return new EvokerSnapshot(health, spellCd, players, emptyBlocks);
    }
    
    public double health() { return health; }
    public double spellCd() { return spellCd; }
    public List<PlayerInfo> players() { return nearbyPlayers; }
    public int emptyBlocks() { return emptyBlocks; }
    
    public static class PlayerInfo {
        final UUID id;
        final BlockPos pos;
        
        public PlayerInfo(UUID id, BlockPos pos) { this.id = id; this.pos = pos; }
        public UUID id() { return id; }
        public BlockPos pos() { return pos; }
    }
}

