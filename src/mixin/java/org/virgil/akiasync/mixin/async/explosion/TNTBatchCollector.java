package org.virgil.akiasync.mixin.async.explosion;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
public class TNTBatchCollector {
    private static final Map<BatchKey, ExplosionBatch> batches = new ConcurrentHashMap<>();
    private static final int MAX_BATCH_SIZE = 64;
    public static synchronized boolean addExplosion(ServerLevel level, Vec3 center, float power, boolean fire) {
        long tick = level.getGameTime();
        ChunkPos chunkPos = new ChunkPos(new BlockPos((int)center.x, (int)center.y, (int)center.z));
        BatchKey key = new BatchKey(level, tick, chunkPos);
        ExplosionBatch batch = batches.computeIfAbsent(key, k -> new ExplosionBatch(level, chunkPos, tick));
        batch.addExplosion(center, power, fire);
        if (batch.getSize() >= 2 || batch.getSize() >= MAX_BATCH_SIZE) {
            batches.remove(key);
            return true;
        }
        return false;
    }
    public static synchronized ExplosionBatch getBatch(ServerLevel level, Vec3 center) {
        long tick = level.getGameTime();
        ChunkPos chunkPos = new ChunkPos(new BlockPos((int)center.x, (int)center.y, (int)center.z));
        BatchKey key = new BatchKey(level, tick, chunkPos);
        return batches.remove(key);
    }
    public static synchronized void clearOldBatches(ServerLevel level, long currentTick) {
        batches.entrySet().removeIf(entry -> 
            entry.getKey().level == level && entry.getKey().tick < currentTick
        );
    }
    private static class BatchKey {
        private final ServerLevel level;
        private final long tick;
        private final ChunkPos chunkPos;
        public BatchKey(ServerLevel level, long tick, ChunkPos chunkPos) {
            this.level = level;
            this.tick = tick;
            this.chunkPos = chunkPos;
        }
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof BatchKey)) return false;
            BatchKey key = (BatchKey) o;
            return tick == key.tick && level == key.level && chunkPos.equals(key.chunkPos);
        }
        @Override
        public int hashCode() {
            return java.util.Objects.hash(System.identityHashCode(level), tick, chunkPos);
        }
    }
    @SuppressWarnings("unused")
    public static class ExplosionBatch {
        private final ServerLevel level;
        private final ChunkPos chunkPos;
        private final long tick;
        private final List<Vec3> centers = new ArrayList<>();
        private final List<Float> powers = new ArrayList<>();
        private final List<Boolean> fires = new ArrayList<>();
        public ExplosionBatch(ServerLevel level, ChunkPos chunkPos, long tick) {
            this.level = level;
            this.chunkPos = chunkPos;
            this.tick = tick;
        }
        public void addExplosion(Vec3 center, float power, boolean fire) {
            if (centers.size() >= MAX_BATCH_SIZE) return;
            centers.add(center);
            powers.add(power);
            fires.add(fire);
        }
        public int getSize() {
            return centers.size();
        }
        public Vec3 getMergedCenter() {
            double x = centers.stream().mapToDouble(Vec3::x).average().orElse(0);
            double y = centers.stream().mapToDouble(Vec3::y).average().orElse(0);
            double z = centers.stream().mapToDouble(Vec3::z).average().orElse(0);
            return new Vec3(x, y, z);
        }
        public float getMergedPower() {
            double sumPowerSquared = 0;
            for (float power : powers) {
                sumPowerSquared += power * power;
            }
            return (float) Math.sqrt(sumPowerSquared);
        }
        public boolean hasAnyFire() {
            return fires.stream().anyMatch(f -> f);
        }
        public ServerLevel getLevel() {
            return level;
        }
    }
}