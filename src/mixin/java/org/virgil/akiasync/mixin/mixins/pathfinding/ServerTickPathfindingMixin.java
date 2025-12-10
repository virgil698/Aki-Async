package org.virgil.akiasync.mixin.mixins.pathfinding;

import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.virgil.akiasync.mixin.pathfinding.EnhancedPathfindingInitializer;

/**
 * 服务器 Tick 寻路处理 Mixin
 * 
 * 在每个服务器 tick 中处理寻路队列
 * 
 * @author AkiAsync
 */
@Mixin(MinecraftServer.class)
public class ServerTickPathfindingMixin {
    
    /**
     * 在服务器 tick 结束时处理寻路队列
     */
    @Inject(
        method = "tickServer",
        at = @At("TAIL"),
        require = 0
    )
    private void akiasync$processPathfindingQueue(CallbackInfo ci) {
        try {
            EnhancedPathfindingInitializer.tick();
        } catch (Exception e) {
            
        }
    }
}
