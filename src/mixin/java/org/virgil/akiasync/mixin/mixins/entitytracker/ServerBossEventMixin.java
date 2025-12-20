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

/**
 * ServerBossEvent Mixin - 线程安全改造
 * 
 * 功能：
 * 将 players 集合改为线程安全的并发集合
 * 
 * 原因：
 * - 多线程追踪器可能在异步线程中移除玩家
 * - Boss栏的玩家集合需要支持并发访问
 * 
 * 实现：
 * - 使用 Sets.newConcurrentHashSet() 替代 Sets.newHashSet()
 * - 确保所有对 players 的访问都是线程安全的
 * 
 * @author AkiAsync (based on Petal)
 */
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
