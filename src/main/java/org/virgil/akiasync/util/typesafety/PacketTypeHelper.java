package org.virgil.akiasync.util.typesafety;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import org.virgil.akiasync.network.PacketPriority;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

public final class PacketTypeHelper {

    private PacketTypeHelper() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static boolean isPacketType(
            @Nullable Packet<?> packet, 
            @Nonnull Class<? extends Packet<?>> packetClass) {
        
        if (packet == null) {
            return false;
        }
        
        return packetClass.isInstance(packet);
    }

    @Nonnull
    public static <T extends Packet<?>> Optional<T> castPacket(
            @Nullable Packet<?> packet, 
            @Nonnull Class<T> packetClass) {
        
        return TypeSafeUtils.safeCast(packet, packetClass);
    }

    @Nonnull
    public static PacketPriority classifyPacket(@Nullable Packet<?> packet) {
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

    public static boolean isChunkPacket(@Nullable Packet<?> packet) {
        if (packet == null) {
            return false;
        }
        
        return packet instanceof ClientboundLevelChunkWithLightPacket ||
               packet instanceof ClientboundForgetLevelChunkPacket ||
               packet instanceof ClientboundLightUpdatePacket;
    }

    public static boolean isEntityPacket(@Nullable Packet<?> packet) {
        if (packet == null) {
            return false;
        }
        
        return packet instanceof ClientboundAddEntityPacket ||
               packet instanceof ClientboundRemoveEntitiesPacket ||
               packet instanceof ClientboundSetEntityDataPacket ||
               packet instanceof ClientboundSetEntityMotionPacket ||
               packet instanceof ClientboundTeleportEntityPacket ||
               packet instanceof ClientboundMoveEntityPacket ||
               packet instanceof ClientboundRotateHeadPacket;
    }

    public static boolean isPlayerActionPacket(@Nullable Packet<?> packet) {
        if (packet == null) {
            return false;
        }
        
        return packet instanceof ClientboundContainerSetContentPacket ||
               packet instanceof ClientboundContainerSetSlotPacket ||
               packet instanceof ClientboundSetHealthPacket ||
               packet instanceof ClientboundSetExperiencePacket ||
               packet instanceof ClientboundPlayerAbilitiesPacket ||
               packet instanceof ClientboundPlayerPositionPacket;
    }
}
