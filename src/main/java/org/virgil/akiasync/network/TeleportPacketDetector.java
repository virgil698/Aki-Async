package org.virgil.akiasync.network;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;

public class TeleportPacketDetector {

    public static boolean isTeleportPacket(Packet<?> packet) {
        if (packet == null) {
            return false;
        }

        if (packet instanceof ClientboundPlayerPositionPacket) {
            return true;
        }

        if (packet instanceof ClientboundRespawnPacket) {
            return true;
        }

        if (packet instanceof ClientboundGameEventPacket) {
            return true;
        }

        if (packet instanceof ClientboundSetDefaultSpawnPositionPacket) {
            return true;
        }

        if (packet instanceof ClientboundChangeDifficultyPacket) {
            return true;
        }

        return false;
    }

    public static boolean isEssentialDuringTeleport(Packet<?> packet) {
        if (packet == null) {
            return false;
        }

        if (isTeleportPacket(packet)) {
            return true;
        }

        if (packet instanceof ClientboundLevelChunkWithLightPacket) {
            return true;
        }

        if (packet instanceof ClientboundLightUpdatePacket) {
            return true;
        }

        if (packet instanceof ClientboundForgetLevelChunkPacket) {
            return true;
        }

        if (packet instanceof ClientboundSetChunkCacheRadiusPacket ||
            packet instanceof ClientboundSetChunkCacheCenterPacket) {
            return true;
        }

        if (packet instanceof ClientboundSetHealthPacket ||
            packet instanceof ClientboundSetExperiencePacket ||
            packet instanceof ClientboundPlayerAbilitiesPacket) {
            return true;
        }

        if (packet instanceof ClientboundContainerSetContentPacket ||
            packet instanceof ClientboundContainerSetSlotPacket) {
            return true;
        }

        if (packet.getClass().getSimpleName().contains("KeepAlive")) {
            return true;
        }

        String packetName = packet.getClass().getSimpleName();
        if (packetName.contains("Login") || packetName.contains("Configuration")) {
            return true;
        }

        if (packetName.contains("Disconnect")) {
            return true;
        }

        return false;
    }

    public static boolean isNonEssentialDuringTeleport(Packet<?> packet) {
        if (packet == null) {
            return false;
        }

        if (packet instanceof ClientboundLevelParticlesPacket) {
            return true;
        }

        if (packet instanceof ClientboundSoundPacket ||
            packet instanceof ClientboundSoundEntityPacket) {
            return true;
        }

        if (packet instanceof ClientboundSetSubtitleTextPacket ||
            packet instanceof ClientboundSetTitleTextPacket ||
            packet instanceof ClientboundSetActionBarTextPacket) {
            return true;
        }

        if (packet instanceof ClientboundBossEventPacket) {
            return true;
        }

        if (packet instanceof ClientboundSetScorePacket ||
            packet instanceof ClientboundSetDisplayObjectivePacket ||
            packet instanceof ClientboundSetPlayerTeamPacket) {
            return true;
        }

        if (packet instanceof ClientboundTabListPacket) {
            return true;
        }

        if (packet instanceof ClientboundMoveEntityPacket ||
            packet instanceof ClientboundRotateHeadPacket) {
            return true;
        }

        if (packet instanceof ClientboundSetEntityMotionPacket) {
            return true;
        }

        return false;
    }

    public static String getPacketTypeDescription(Packet<?> packet) {
        if (packet == null) {
            return "null";
        }

        if (isTeleportPacket(packet)) {
            return "TELEPORT";
        }

        if (isEssentialDuringTeleport(packet)) {
            return "ESSENTIAL";
        }

        if (isNonEssentialDuringTeleport(packet)) {
            return "NON_ESSENTIAL";
        }

        return "NORMAL";
    }
}
