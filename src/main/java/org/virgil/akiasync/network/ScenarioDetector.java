package org.virgil.akiasync.network;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ScenarioDetector {

    public enum ScenarioType {
        NORMAL("正常", "Normal", 1.0),
        PVP("PVP战斗", "PVP Combat", 1.5),
        PVE("PVE刷怪", "PVE Grinding", 1.8),
        FARM("多生物农场", "Multi-mob Farm", 2.0),
        SAND_DUPER("刷沙机", "Sand Duper", 2.5),
        HIGH_DENSITY("高密度区块", "High-density Chunk", 2.2),
        ELYTRA("鞘翅飞行", "Elytra Flight", 1.6);

        private final String nameCn;
        private final String nameEn;
        private final double loadFactor;

        ScenarioType(String nameCn, String nameEn, double loadFactor) {
            this.nameCn = nameCn;
            this.nameEn = nameEn;
            this.loadFactor = loadFactor;
        }

        public String getNameCn() { return nameCn; }
        public String getNameEn() { return nameEn; }
        public double getLoadFactor() { return loadFactor; }
    }

    private static class PlayerScenarioStats {

        private final AtomicInteger movePackets = new AtomicInteger(0);
        private final AtomicInteger entityPackets = new AtomicInteger(0);
        private final AtomicInteger blockPackets = new AtomicInteger(0);
        private final AtomicInteger damagePackets = new AtomicInteger(0);
        private final AtomicInteger fallingBlockPackets = new AtomicInteger(0);
        private final AtomicInteger containerPackets = new AtomicInteger(0);
        private final AtomicInteger chunkPackets = new AtomicInteger(0);

        private long lastResetTime = System.currentTimeMillis();
        private ScenarioType currentScenario = ScenarioType.NORMAL;

        public void recordPacket(Packet<?> packet) {

            if (packet instanceof ClientboundMoveEntityPacket ||
                packet instanceof ClientboundTeleportEntityPacket ||
                packet instanceof ClientboundPlayerPositionPacket) {
                movePackets.incrementAndGet();
            }

            else if (packet instanceof ClientboundAddEntityPacket ||
                     packet instanceof ClientboundRemoveEntitiesPacket ||
                     packet instanceof ClientboundSetEntityDataPacket) {
                entityPackets.incrementAndGet();
            }

            else if (packet instanceof ClientboundBlockUpdatePacket ||
                     packet instanceof ClientboundSectionBlocksUpdatePacket) {
                blockPackets.incrementAndGet();
            }

            else if (packet instanceof ClientboundDamageEventPacket ||
                     packet instanceof ClientboundHurtAnimationPacket) {
                damagePackets.incrementAndGet();
            }

            else if (packet instanceof ClientboundAddEntityPacket) {

                fallingBlockPackets.incrementAndGet();
            }

            else if (packet instanceof ClientboundContainerSetContentPacket ||
                     packet instanceof ClientboundContainerSetSlotPacket) {
                containerPackets.incrementAndGet();
            }

            else if (packet instanceof ClientboundLevelChunkWithLightPacket) {
                chunkPackets.incrementAndGet();
            }
        }

        public ScenarioType detectScenario() {
            long now = System.currentTimeMillis();

            if (now - lastResetTime > 1000) {
                int move = movePackets.get();
                int entity = entityPackets.get();
                int block = blockPackets.get();
                int damage = damagePackets.get();
                int falling = fallingBlockPackets.get();
                int container = containerPackets.get();
                int chunk = chunkPackets.get();

                ScenarioType detected = ScenarioType.NORMAL;

                if (falling > 50 || block > 100) {
                    detected = ScenarioType.SAND_DUPER;
                }

                else if (entity > 80) {
                    detected = ScenarioType.FARM;
                }

                else if (container > 50) {
                    detected = ScenarioType.HIGH_DENSITY;
                }

                else if (damage > 20 && entity > 30) {
                    detected = ScenarioType.PVE;
                }

                else if (move > 30 && chunk > 10) {
                    detected = ScenarioType.ELYTRA;
                }

                else if (damage > 10 && move > 20) {
                    detected = ScenarioType.PVP;
                }

                currentScenario = detected;

                movePackets.set(0);
                entityPackets.set(0);
                blockPackets.set(0);
                damagePackets.set(0);
                fallingBlockPackets.set(0);
                containerPackets.set(0);
                chunkPackets.set(0);
                lastResetTime = now;
            }

            return currentScenario;
        }

        public ScenarioType getCurrentScenario() {
            return currentScenario;
        }
    }

    private static final ConcurrentHashMap<UUID, PlayerScenarioStats> playerStats =
        new ConcurrentHashMap<>();

    public static void recordPacket(UUID playerId, Packet<?> packet) {
        if (playerId == null || packet == null) {
            return;
        }

        PlayerScenarioStats stats = playerStats.computeIfAbsent(
            playerId, k -> new PlayerScenarioStats()
        );

        stats.recordPacket(packet);
    }

    public static ScenarioType detectScenario(UUID playerId) {
        if (playerId == null) {
            return ScenarioType.NORMAL;
        }

        PlayerScenarioStats stats = playerStats.get(playerId);
        if (stats == null) {
            return ScenarioType.NORMAL;
        }

        return stats.detectScenario();
    }

    public static ScenarioType getCurrentScenario(UUID playerId) {
        if (playerId == null) {
            return ScenarioType.NORMAL;
        }

        PlayerScenarioStats stats = playerStats.get(playerId);
        if (stats == null) {
            return ScenarioType.NORMAL;
        }

        return stats.getCurrentScenario();
    }

    public static void removePlayer(UUID playerId) {
        if (playerId != null) {
            playerStats.remove(playerId);
        }
    }

    public static void clearAll() {
        playerStats.clear();
    }

    public static int getTrackedPlayerCount() {
        return playerStats.size();
    }
}
