package org.virgil.akiasync.mixin.mixins.world;

import net.minecraft.commands.CommandSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.util.thread.ReentrantBlockableEventLoop;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = MinecraftServer.class, priority = Integer.MAX_VALUE)
public abstract class MinecraftServerMixin extends ReentrantBlockableEventLoop<TickTask> implements CommandSource, AutoCloseable {

    public MinecraftServerMixin(String name) {
        super(name);
    }

    @Redirect(method = "reloadResources(Ljava/util/Collection;Lio/papermc/paper/event/server/ServerResourcesReloadedEvent$Cause;)Ljava/util/concurrent/CompletableFuture;", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;isSameThread()Z"))
    private boolean akiasync$onServerExecutionThreadPatch(MinecraftServer server) {
        return Thread.currentThread().equals(Thread.currentThread());
    }
}
