package org.virgil.akiasync.mixin.mixins.network;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@SuppressWarnings("unused")
@Mixin(ServerCommonPacketListenerImpl.class)
public class EntityRotationPacketFilterMixin {
    
    @Unique
    private static volatile boolean enabled = true;
    @Unique
    private static volatile boolean initialized = false;
    @Unique
    private static volatile boolean filterPureRotation = true;
    
    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void filterZeroRotationPackets(Packet<?> packet, CallbackInfo ci) {
        if (!initialized) {
            akiasync$init();
        }
        
        if (!enabled) {
            return;
        }
        
        try {
            
            if (filterPureRotation && packet instanceof ClientboundMoveEntityPacket.Rot rotPacket) {
                if (akiasync$isZeroRotation(rotPacket)) {
                    ci.cancel();
                    return;
                }
            }
            
            
        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "EntityRotationPacketFilter", "filterZeroRotationPackets", e);
        }
    }
    
    @Unique
    private boolean akiasync$isZeroRotation(ClientboundMoveEntityPacket.Rot packet) {
        try {
            
            float yRot = packet.getYRot();
            float xRot = packet.getXRot();
            
            
            return yRot == 0.0f && xRot == 0.0f;
        } catch (Exception e) {
            return false;
        }
    }
    
    @Unique
    private static void akiasync$init() {
        if (initialized) {
            return;
        }
        
        try {
            org.virgil.akiasync.mixin.bridge.Bridge bridge =
                org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
            
            if (bridge != null) {
                enabled = bridge.isEntityRotationPacketFilterEnabled();
                filterPureRotation = bridge.isFilterPureRotationEnabled();
                
                bridge.debugLog("[EntityRotationPacketFilter] Initialized: enabled=%s, filterPureRotation=%s",
                    enabled, filterPureRotation);
            
                    initialized = true;
                }
        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "EntityRotationPacketFilter", "init", e);
        }
    }
}
