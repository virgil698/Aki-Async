package org.virgil.akiasync.mixin.brain.universal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
public final class UniversalAiSnapshot {
    private final double health;
    private final double level;
    private final List<PlayerInfo> nearbyPlayers;
    private final List<MobInfo> nearbyMobs;
    private final List<BlockPos> nearbyPOIs;
    private UniversalAiSnapshot(double health, double level, List<PlayerInfo> players,
                                List<MobInfo> mobs, List<BlockPos> pois) {
        this.health = health;
        this.level = level;
        this.nearbyPlayers = players;
        this.nearbyMobs = mobs;
        this.nearbyPOIs = pois;
    }
    public static UniversalAiSnapshot capture(Mob mob, ServerLevel world) {
        double health = mob.getHealth();
        double level = mob.position().y;
        AABB box = mob.getBoundingBox().inflate(32.0);
        List<PlayerInfo> players = world.getEntitiesOfClass(
            net.minecraft.world.entity.player.Player.class, box
        ).stream().map(p -> new PlayerInfo(p.getUUID(), p.blockPosition())).collect(Collectors.toList());
        AABB mobBox = mob.getBoundingBox().inflate(16.0);
        List<MobInfo> mobs = world.getEntitiesOfClass(
            Mob.class, mobBox, e -> e instanceof Mob
        ).stream().map(m -> new MobInfo(m.blockPosition())).collect(Collectors.toList());
        List<BlockPos> pois = java.util.Collections.emptyList();
        return new UniversalAiSnapshot(health, level, players, mobs, pois);
    }
    public double health() { return health; }
    public double level() { return level; }
    public List<PlayerInfo> players() { return nearbyPlayers; }
    public List<MobInfo> mobs() { return nearbyMobs; }
    public List<BlockPos> pois() { return nearbyPOIs; }
    public static class PlayerInfo {
        final UUID id; final BlockPos pos;
        public PlayerInfo(UUID id, BlockPos pos) { this.id = id; this.pos = pos; }
        public UUID id() { return id; }
        public BlockPos pos() { return pos; }
    }
    public static class MobInfo {
        final BlockPos pos;
        public MobInfo(BlockPos pos) { this.pos = pos; }
        public BlockPos pos() { return pos; }
    }
}