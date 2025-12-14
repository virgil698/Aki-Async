package org.virgil.akiasync.mixin.mixins.entitytracker;

import java.util.Set;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.virgil.akiasync.mixin.bridge.Bridge;
import org.virgil.akiasync.mixin.bridge.BridgeManager;
import org.virgil.akiasync.mixin.util.BridgeConfigCache;

import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

@SuppressWarnings("unused")
@Mixin(ChunkMap.TrackedEntity.class)
public abstract class EntityTrackerMixin {

    private static volatile boolean cached_enabled;
    private static volatile boolean initialized = false;
    
    @Shadow @Final ServerEntity serverEntity;
    @Shadow @Final Entity entity;
    @Shadow @Final int range;
    @Shadow @Final Set<ServerPlayer> seenBy;
    @Inject(method = "updatePlayer", at = @At("HEAD"), cancellable = true)
    private void preUpdatePlayer(ServerPlayer player, CallbackInfo ci) {
        if (!initialized) { akiasync$initEntityTracker(); }

        
        if (!cached_enabled) {
            return;
        }

        
        if (entity instanceof ServerPlayer) {
            return;
        }

        
        if (akiasync$isVirtualEntity(entity)) {
            return;
        }
        
        if (!seenBy.contains(player.connection)) {
            return;
        }
        
        
        Bridge bridgeForThrottle = BridgeManager.getBridge();
        if (bridgeForThrottle != null && bridgeForThrottle.isEntityPacketThrottleEnabled()) {
            if (!bridgeForThrottle.shouldSendEntityUpdate(player, entity)) {
                
                ci.cancel();
                return;
            }
        }
        
        
    }
    private boolean akiasync$isVirtualEntity(Entity entity) {
        Bridge bridge = BridgeManager.getBridge();
        if (bridge != null) {
            return bridge.isVirtualEntity(entity);
        }
        return false;
    }

    private static synchronized void akiasync$initEntityTracker() {
        if (initialized) return;

        Bridge bridge = BridgeManager.getBridge();
        if (bridge != null) {
            cached_enabled = bridge.isEntityTrackerEnabled();
            BridgeConfigCache.debugLog("[AkiAsync] EntityTrackerMixin initialized: " +
                "enabled=" + cached_enabled + " (throttle-only mode, vanilla tracking preserved)");
        } else {
            cached_enabled = false;
        }
        
        initialized = true;
    }
}
