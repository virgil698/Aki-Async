package org.virgil.akiasync.mixin.brain.evoker;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.Evoker;
import net.minecraft.world.phys.AABB;
public final class EvokerSnapshot {
    private final double health;
    private final double spellCd;
    private final List<PlayerInfo> nearbyPlayers;
    private final int emptyBlocks;
    private EvokerSnapshot(double health, double spellCd, List<PlayerInfo> players, int empty) {
        this.health = health;
        this.spellCd = spellCd;
        this.nearbyPlayers = players;
        this.emptyBlocks = empty;
    }
    public static EvokerSnapshot capture(Evoker evoker, ServerLevel level) {
        double health = evoker.getHealth();
        double spellCd = health < 10.0 ? 0.0 : 50.0;
        
        List<PlayerInfo> players = org.virgil.akiasync.mixin.brain.core.AiQueryHelper
            .getNearbyPlayers(evoker, 32.0)
            .stream()
            .map(p -> new PlayerInfo(p.getUUID(), p.blockPosition()))
            .collect(Collectors.toList());
        
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
