package org.virgil.akiasync.mixin.mixins.entity.tracker;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.virgil.akiasync.mixin.accessor.ChunkMapTrackerAccess;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Mixin(ServerEntity.class)
public abstract class ServerEntityMixin_1_21_10 {

    @Shadow @Final private Entity entity;

    @Shadow
    public abstract void sendPairingData(ServerPlayer player, Consumer<Packet<ClientGamePacketListener>> consumer);

    @Unique
    @SuppressWarnings("BC_UNCONFIRMED_CAST")
    private ChunkMapTrackerAccess aki$getChunkMapAccess() {
        ServerLevel level = (ServerLevel) this.entity.level();
        return (ChunkMapTrackerAccess) level.chunkSource.chunkMap;
    }

    @Inject(method = "removePairing", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;send(Lnet/minecraft/network/protocol/Packet;)V"
    ), cancellable = true)
    private void aki$ensureRemovePairingOnMainThread(ServerPlayer player, CallbackInfo ci) {
        ChunkMapTrackerAccess chunkMap = aki$getChunkMapAccess();
        chunkMap.aki$runOnTrackerMainThread(() -> {
            player.connection.send(new ClientboundRemoveEntitiesPacket(this.entity.getId()));
        });
        ci.cancel();
    }

    @Inject(method = "addPairing", at = @At("HEAD"), cancellable = true)
    private void aki$ensureAddPairingOnMainThread(ServerPlayer player, CallbackInfo ci) {
        if (!net.minecraft.server.MinecraftServer.getServer().isSameThread()) {
            ChunkMapTrackerAccess chunkMap = aki$getChunkMapAccess();

            chunkMap.aki$runOnTrackerMainThread(() -> {
                try {
                    List<Packet<? super ClientGamePacketListener>> list = new ArrayList<>();
                    this.sendPairingData(player, list::add);
                    player.connection.send(new ClientboundBundlePacket(list));
                    this.entity.startSeenByPlayer(player);
                } catch (Exception e) {
                    org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                        "ServerEntity", "addPairing", e);
                }
            });

            ci.cancel();
        }
    }

    @Redirect(method = "sendDirtyEntityData", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/server/level/ServerEntity$Synchronizer;sendToTrackingPlayersAndSelf(Lnet/minecraft/network/protocol/Packet;)V",
        ordinal = 0
    ), require = 0)
    private void aki$ensureEntityDataOnMainThread(Object synchronizer, net.minecraft.network.protocol.Packet<?> packet) {
        ChunkMapTrackerAccess chunkMap = aki$getChunkMapAccess();
        chunkMap.aki$runOnTrackerMainThread(() -> {
            aki$invokeSendToTrackingPlayersAndSelf(synchronizer, packet);
        });
    }

    @Redirect(method = "sendDirtyEntityData", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/server/level/ServerEntity$Synchronizer;sendToTrackingPlayersAndSelf(Lnet/minecraft/network/protocol/Packet;)V",
        ordinal = 1
    ), require = 0)
    private void aki$ensureAttributesOnMainThread(Object synchronizer, net.minecraft.network.protocol.Packet<?> packet) {
        ChunkMapTrackerAccess chunkMap = aki$getChunkMapAccess();
        chunkMap.aki$runOnTrackerMainThread(() -> {
            aki$invokeSendToTrackingPlayersAndSelf(synchronizer, packet);
        });
    }

    @Unique
    private static void aki$invokeSendToTrackingPlayersAndSelf(Object synchronizer, net.minecraft.network.protocol.Packet<?> packet) {
        try {
            java.lang.reflect.Method method = synchronizer.getClass().getMethod("sendToTrackingPlayersAndSelf", net.minecraft.network.protocol.Packet.class);
            method.invoke(synchronizer, packet);
        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "ServerEntity", "sendToTrackingPlayersAndSelf", e);
        }
    }
}
