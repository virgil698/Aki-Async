package org.virgil.akiasync.mixin.mixins.secureseed;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.world.RandomSequences;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.ServerLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.virgil.akiasync.mixin.secureseed.crypto.Globals;

import java.util.List;
import java.util.concurrent.Executor;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {

    @Inject(
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/MinecraftServer;forceSynchronousWrites()Z"
        )
    )
    private void secureSeed_setupGlobals(
            MinecraftServer minecraftServer,
            Executor executor,
            LevelStorageSource.LevelStorageAccess levelStorageAccess,
            ServerLevelData serverLevelData,
            ResourceKey resourceKey,
            LevelStem levelStem,
            ChunkProgressListener chunkProgressListener,
            boolean bl,
            long l,
            List list,
            boolean bl2,
            RandomSequences randomSequences,
            CallbackInfo ci) {
        Globals.setupGlobals((ServerLevel) (Object) this);
    }
}
