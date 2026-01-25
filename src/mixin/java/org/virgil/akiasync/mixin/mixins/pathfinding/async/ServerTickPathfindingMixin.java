package org.virgil.akiasync.mixin.mixins.pathfinding.async;

import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.virgil.akiasync.mixin.pathfinding.EnhancedPathfindingInitializer;
import org.virgil.akiasync.mixin.util.GlobalCacheCleanup;
import org.virgil.akiasync.mixin.util.FoliaUtils;

import java.util.function.BooleanSupplier;

@Mixin(MinecraftServer.class)
public class ServerTickPathfindingMixin {

    @Inject(
        method = "tickChildren(Ljava/util/function/BooleanSupplier;)V",
        at = @At("TAIL"),
        require = 0
    )
    private void akiasync$processPathfindingQueue(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        try {
            EnhancedPathfindingInitializer.tick();
        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "ServerTickPathfinding", "tick", e);
        }

        try {
            long currentTick = FoliaUtils.getCurrentTick();
            GlobalCacheCleanup.performCleanup(currentTick);
            GlobalCacheCleanup.logMemoryStats(currentTick);
        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "GlobalCacheCleanup", "tick", e);
        }
    }
}
