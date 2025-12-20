package org.virgil.akiasync.mixin.mixins.entitytracker;

import com.google.common.collect.Sets;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(ServerBossEvent.class)
public abstract class ServerBossEventMixin {
    
    @Shadow @Final @Mutable
    private Set<ServerPlayer> players;
    
    @Inject(method = "<init>", at = @At("RETURN"))
    private void aki$makePlayersThreadSafe(CallbackInfo ci) {
        
        Set<ServerPlayer> oldPlayers = this.players;
        this.players = Sets.newConcurrentHashSet();
        if (oldPlayers != null && !oldPlayers.isEmpty()) {
            this.players.addAll(oldPlayers);
        }
    }
}
