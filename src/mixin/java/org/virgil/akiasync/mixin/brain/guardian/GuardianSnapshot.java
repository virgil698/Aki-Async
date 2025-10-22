package org.virgil.akiasync.mixin.brain.guardian;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.phys.AABB;
public final class GuardianSnapshot {
    private final List<PlayerInfo> underwaterPlayers;
    private GuardianSnapshot(List<PlayerInfo> p) { this.underwaterPlayers = p; }
    public static GuardianSnapshot capture(Guardian guardian, ServerLevel level) {
        AABB box = guardian.getBoundingBox().inflate(16.0);
        List<PlayerInfo> players = level.getEntitiesOfClass(
            net.minecraft.world.entity.player.Player.class, box
        ).stream()
            .filter(p -> p.isInWater())
            .map(p -> new PlayerInfo(p.getUUID(), p.blockPosition()))
            .collect(Collectors.toList());
        return new GuardianSnapshot(players);
    }
    public List<PlayerInfo> players() { return underwaterPlayers; }
    public static class PlayerInfo {
        final UUID id; final BlockPos pos;
        public PlayerInfo(UUID id, BlockPos pos) { this.id = id; this.pos = pos; }
        public UUID id() { return id; }
        public BlockPos pos() { return pos; }
    }
}