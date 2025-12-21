package org.virgil.akiasync.mixin.mixins.pathfinding;

import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.virgil.akiasync.mixin.pathfinding.EnhancedPathfindingInitializer;
import org.virgil.akiasync.mixin.util.GlobalCacheCleanup;

@Mixin(MinecraftServer.class)
public class ServerTickPathfindingMixin {
    
    @Inject(
        method = "tickServer",
        at = @At("TAIL"),
        require = 0
    )
    private void akiasync$processPathfindingQueue(CallbackInfo ci) {
        try {
            EnhancedPathfindingInitializer.tick();
        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "ServerTickPathfinding", "tick", e);
        }
        
        try {
            long currentTick = MinecraftServer.currentTick;
            GlobalCacheCleanup.performCleanup(currentTick);
            GlobalCacheCleanup.logMemoryStats(currentTick);
        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "GlobalCacheCleanup", "tick", e);
        }
    }
}
