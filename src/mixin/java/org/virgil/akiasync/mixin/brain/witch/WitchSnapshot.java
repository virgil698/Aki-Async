package org.virgil.akiasync.mixin.brain.witch;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.phys.AABB;
public final class WitchSnapshot {
    private final List<PlayerInfo> players;
    private WitchSnapshot(List<PlayerInfo> p) { this.players = p; }
    public static WitchSnapshot capture(Witch witch, ServerLevel level) {
        AABB box = witch.getBoundingBox().inflate(16.0);
        List<PlayerInfo> players = level.getEntitiesOfClass(
            net.minecraft.world.entity.player.Player.class, box
        ).stream().map(p -> new PlayerInfo(p.getUUID(), p.blockPosition())).collect(Collectors.toList());
        return new WitchSnapshot(players);
    }
    public List<PlayerInfo> players() { return players; }
    public static class PlayerInfo {
        final UUID id; final BlockPos pos;
        public PlayerInfo(UUID id, BlockPos pos) { this.id = id; this.pos = pos; }
        public UUID id() { return id; }
        public BlockPos pos() { return pos; }
    }
}