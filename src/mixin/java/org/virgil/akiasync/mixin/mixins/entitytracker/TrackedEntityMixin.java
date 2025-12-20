package org.virgil.akiasync.mixin.mixins.entitytracker;

import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerPlayerConnection;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.virgil.akiasync.mixin.bridge.Bridge;
import org.virgil.akiasync.mixin.bridge.BridgeManager;

import java.util.Collections;
import java.util.Set;

/**
 * TrackedEntity Mixin - 线程安全改造
 * 
 * 功能：
 * 1. 将 seenBy 集合改为线程安全的同步集合
 * 2. 移除 AsyncCatcher 检查，允许异步更新和移除
 * 
 * 线程安全措施：
 * - 使用 Collections.synchronizedSet() 包装 seenBy 集合
 * - 所有对 seenBy 的访问都是线程安全的
 * 
 * 实现方式：
 * - 使用 @Redirect 拦截 AsyncCatcher.catchOp() 调用
 * - 只有在多线程追踪器启用时才跳过检查
 * - 保持与原版的兼容性
 * 
 * @author AkiAsync
 */
@Mixin(ChunkMap.TrackedEntity.class)
public abstract class TrackedEntityMixin {
    
    @Shadow @Final @Mutable
    public Set<ServerPlayerConnection> seenBy;
    
    @Inject(method = "<init>", at = @At("RETURN"))
    private void aki$makeSeenByThreadSafe(CallbackInfo ci) {
        
        this.seenBy = Collections.synchronizedSet(new ReferenceOpenHashSet<>());
    }
    
    @Redirect(
        method = "removePlayer",
        at = @At(
            value = "INVOKE",
            target = "Lorg/spigotmc/AsyncCatcher;catchOp(Ljava/lang/String;)V",
            remap = false
        ),
        require = 0  
    )
    private void aki$skipAsyncCatcherInRemovePlayer(String reason) {
        
        Bridge bridge = BridgeManager.getBridge();
        if (bridge != null && bridge.isMultithreadedEntityTrackerEnabled()) {
            
            return;
        }
        
        org.spigotmc.AsyncCatcher.catchOp(reason);
    }
    
    @Redirect(
        method = "updatePlayer",
        at = @At(
            value = "INVOKE",
            target = "Lorg/spigotmc/AsyncCatcher;catchOp(Ljava/lang/String;)V",
            remap = false
        ),
        require = 0  
    )
    private void aki$skipAsyncCatcherInUpdatePlayer(String reason) {
        
        Bridge bridge = BridgeManager.getBridge();
        if (bridge != null && bridge.isMultithreadedEntityTrackerEnabled()) {
            
            return;
        }
        
        org.spigotmc.AsyncCatcher.catchOp(reason);
    }
}
