package org.virgil.akiasync.mixin.mixins.entitytracker;

import com.google.common.collect.Lists;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.virgil.akiasync.mixin.accessor.ChunkMapTrackerAccess;

import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

/**
 * ServerEntity Mixin - 确保数据包在主线程发送
 * 
 * 功能：
 * 1. 拦截 removePairing() 中的数据包发送，确保在主线程执行
 * 2. 拦截 addPairing() 中的数据包发送，确保在主线程执行
 * 3. 拦截 sendChanges() 中的数据包发送，确保在主线程执行
 * 
 * 原因：
 * - 数据包发送必须在主线程执行，否则可能导致客户端显示异常
 * - 多线程追踪器在更新玩家时可能在工作线程中调用这些方法
 * 
 * @author AkiAsync
 */
@Mixin(ServerEntity.class)
public abstract class ServerEntityMixin {
    
    @Shadow @Final private Entity entity;
    
    @Shadow
    public abstract void sendPairingData(ServerPlayer player, Consumer<net.minecraft.network.protocol.Packet<?>> consumer);
    
    @Shadow
    protected abstract void broadcastAndSend(net.minecraft.network.protocol.Packet<?> packet);
    
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
            player.connection.send(new ClientboundRemoveEntitiesPacket(new int[]{this.entity.getId()}));
        });
        ci.cancel();
    }
    
    @Inject(method = "addPairing", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/server/level/ServerEntity;sendPairingData(Lnet/minecraft/server/level/ServerPlayer;Ljava/util/function/Consumer;)V"
    ), cancellable = true)
    private void aki$ensureAddPairingOnMainThread(ServerPlayer player, CallbackInfo ci) {
        ServerGamePacketListenerImpl playerconnection = player.connection;
        Objects.requireNonNull(player.connection);
        
        ChunkMapTrackerAccess chunkMap = aki$getChunkMapAccess();
        chunkMap.aki$runOnTrackerMainThread(() -> {
            this.sendPairingData(player, playerconnection::send);
        });
        
        this.entity.startSeenByPlayer(player);
        ci.cancel();
    }
    
    @Redirect(method = "sendDirtyEntityData", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/server/level/ServerEntity;broadcastAndSend(Lnet/minecraft/network/protocol/Packet;)V",
        ordinal = 0
    ))
    private void aki$ensureEntityDataOnMainThread(ServerEntity instance, net.minecraft.network.protocol.Packet<?> packet) {
        ChunkMapTrackerAccess chunkMap = aki$getChunkMapAccess();
        chunkMap.aki$runOnTrackerMainThread(() -> {
            this.broadcastAndSend(packet);
        });
    }
    
    @Redirect(method = "sendDirtyEntityData", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/server/level/ServerEntity;broadcastAndSend(Lnet/minecraft/network/protocol/Packet;)V",
        ordinal = 1
    ))
    private void aki$ensureAttributesOnMainThread(ServerEntity instance, net.minecraft.network.protocol.Packet<?> packet) {
        ChunkMapTrackerAccess chunkMap = aki$getChunkMapAccess();
        chunkMap.aki$runOnTrackerMainThread(() -> {
            this.broadcastAndSend(packet);
        });
    }
}
