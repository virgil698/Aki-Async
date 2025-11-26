package org.virgil.akiasync.network;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;

public class PacketClassifier {

    public static PacketPriority classify(Packet<?> packet) {
        if (packet == null) {
            return PacketPriority.NORMAL;
        }

        if (packet instanceof ClientboundContainerSetContentPacket ||
            packet instanceof ClientboundContainerSetSlotPacket ||
            packet instanceof ClientboundSetEntityDataPacket ||
            packet instanceof ClientboundSetEquipmentPacket ||
            packet instanceof ClientboundSetHealthPacket ||
            packet instanceof ClientboundSetExperiencePacket ||
            packet instanceof ClientboundPlayerAbilitiesPacket ||
            packet instanceof ClientboundPlayerPositionPacket ||
            packet instanceof ClientboundTeleportEntityPacket ||
            packet instanceof ClientboundMoveEntityPacket ||
            packet instanceof ClientboundRotateHeadPacket) {
            return PacketPriority.CRITICAL;
        }

        if (packet instanceof ClientboundAddEntityPacket ||
            packet instanceof ClientboundRemoveEntitiesPacket ||
            packet instanceof ClientboundSetEntityMotionPacket ||
            packet instanceof ClientboundBlockUpdatePacket ||
            packet instanceof ClientboundSectionBlocksUpdatePacket ||
            packet instanceof ClientboundBlockEntityDataPacket ||
            packet instanceof ClientboundExplodePacket ||
            packet instanceof ClientboundDamageEventPacket ||
            packet instanceof ClientboundHurtAnimationPacket) {
            return PacketPriority.HIGH;
        }

        if (packet instanceof ClientboundLevelChunkWithLightPacket ||
            packet instanceof ClientboundLightUpdatePacket ||
            packet instanceof ClientboundForgetLevelChunkPacket ||
            packet instanceof ClientboundMapItemDataPacket ||
            packet instanceof ClientboundSetChunkCacheRadiusPacket ||
            packet instanceof ClientboundSetChunkCacheCenterPacket) {
            return PacketPriority.NORMAL;
        }

        if (packet instanceof ClientboundLevelParticlesPacket ||
            packet instanceof ClientboundSoundPacket ||
            packet instanceof ClientboundSoundEntityPacket ||
            packet instanceof ClientboundSetSubtitleTextPacket ||
            packet instanceof ClientboundSetTitleTextPacket ||
            packet instanceof ClientboundSetActionBarTextPacket ||
            packet instanceof ClientboundBossEventPacket ||
            packet instanceof ClientboundSetScorePacket ||
            packet instanceof ClientboundSetDisplayObjectivePacket ||
            packet instanceof ClientboundSetPlayerTeamPacket ||
            packet instanceof ClientboundTabListPacket) {
            return PacketPriority.LOW;
        }

        return PacketPriority.NORMAL;
    }

    public static boolean isChunkPacket(Packet<?> packet) {
        return packet instanceof ClientboundLevelChunkWithLightPacket ||
               packet instanceof ClientboundForgetLevelChunkPacket ||
               packet instanceof ClientboundLightUpdatePacket;
    }

    public static boolean isEntityPacket(Packet<?> packet) {
        return packet instanceof ClientboundAddEntityPacket ||
               packet instanceof ClientboundRemoveEntitiesPacket ||
               packet instanceof ClientboundSetEntityDataPacket ||
               packet instanceof ClientboundSetEntityMotionPacket ||
               packet instanceof ClientboundTeleportEntityPacket ||
               packet instanceof ClientboundMoveEntityPacket ||
               packet instanceof ClientboundRotateHeadPacket;
    }

    public static boolean isPlayerActionPacket(Packet<?> packet) {
        return packet instanceof ClientboundContainerSetContentPacket ||
               packet instanceof ClientboundContainerSetSlotPacket ||
               packet instanceof ClientboundSetHealthPacket ||
               packet instanceof ClientboundSetExperiencePacket ||
               packet instanceof ClientboundPlayerAbilitiesPacket ||
               packet instanceof ClientboundPlayerPositionPacket;
    }
}
