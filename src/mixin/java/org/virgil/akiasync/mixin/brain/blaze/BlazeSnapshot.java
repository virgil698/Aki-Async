package org.virgil.akiasync.mixin.brain.blaze;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.phys.AABB;

/**
 * Blaze snapshot (火焰弹+火柱空位)
 * @author Virgil
 */
public final class BlazeSnapshot {
    private final double blazeCd;
    private final List<PlayerInfo> players;
    private final BlockPos fireColumnPos;
    
    private BlazeSnapshot(double cd, List<PlayerInfo> p, BlockPos fire) {
        this.blazeCd = cd; this.players = p; this.fireColumnPos = fire;
    }
    
    public static BlazeSnapshot capture(Blaze blaze, ServerLevel level) {
        double blazeCd = blaze.getHealth() < 10.0 ? 0.0 : 50.0;
        
        AABB box = blaze.getBoundingBox().inflate(32.0);
        List<PlayerInfo> players = level.getEntitiesOfClass(
            net.minecraft.world.entity.player.Player.class, box
        ).stream().map(p -> new PlayerInfo(p.getUUID(), p.blockPosition())).collect(Collectors.toList());
        
        BlockPos fire = blaze.blockPosition().above(2);
        
        return new BlazeSnapshot(blazeCd, players, fire);
    }
    
    public double blazeCd() { return blazeCd; }
    public List<PlayerInfo> players() { return players; }
    public BlockPos fireColumn() { return fireColumnPos; }
    
    public static class PlayerInfo {
        final UUID id; final BlockPos pos;
        public PlayerInfo(UUID id, BlockPos pos) { this.id = id; this.pos = pos; }
        public UUID id() { return id; }
        public BlockPos pos() { return pos; }
    }
}

