package org.virgil.akiasync.mixin.brain.pillager;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.AbstractIllager;
import org.virgil.akiasync.mixin.brain.core.AiQueryHelper;

public final class PillagerSnapshot {
    private final double health;
    private final boolean isChargingCrossbow;
    private final List<PlayerHealthInfo> nearbyPlayers;
    private final BlockPos raidCenter;
    private final List<BlockPos> nearbyPOIs;
    
    private PillagerSnapshot(double health, boolean charging, List<PlayerHealthInfo> players,
                            BlockPos raid, List<BlockPos> pois) {
        this.health = health;
        this.isChargingCrossbow = charging;
        this.nearbyPlayers = players;
        this.raidCenter = raid;
        this.nearbyPOIs = pois;
    }
    
    public static PillagerSnapshot capture(AbstractIllager illager, ServerLevel level) {
        double health = illager.getHealth();
        boolean charging = illager instanceof net.minecraft.world.entity.monster.Pillager ?
            ((net.minecraft.world.entity.monster.Pillager) illager).isChargingCrossbow() : false;
        
        List<PlayerHealthInfo> players = AiQueryHelper.getNearbyPlayers(illager, 32.0)
            .stream()
            .map(p -> new PlayerHealthInfo(p.getUUID(), p.blockPosition(), p.getHealth() / p.getMaxHealth()))
            .collect(Collectors.toList());
        
        net.minecraft.world.entity.raid.Raid raid = level.getRaidAt(illager.blockPosition());
        BlockPos raidCenter = raid != null ? raid.getCenter() : null;
        
        List<BlockPos> pois = AiQueryHelper.getNearbyPoi(illager, 32)
            .stream()
            .map(poi -> poi.getPos())
            .limit(16)
            .collect(Collectors.toList());
        
        return new PillagerSnapshot(health, charging, players, raidCenter, pois);
    }
    public double health() { return health; }
    public boolean charging() { return isChargingCrossbow; }
    public List<PlayerHealthInfo> players() { return nearbyPlayers; }
    public BlockPos raid() { return raidCenter; }
    public List<BlockPos> pois() { return nearbyPOIs; }
    public static class PlayerHealthInfo {
        final UUID id;
        final BlockPos pos;
        final float healthPct;
        public PlayerHealthInfo(UUID id, BlockPos pos, float pct) {
            this.id = id; this.pos = pos; this.healthPct = pct;
        }
        public UUID id() { return id; }
        public BlockPos pos() { return pos; }
        public float health() { return healthPct; }
    }
}
