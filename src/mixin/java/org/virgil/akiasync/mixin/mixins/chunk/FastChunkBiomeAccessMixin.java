package org.virgil.akiasync.mixin.mixins.chunk;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;


@Mixin(LevelReader.class)
public interface FastChunkBiomeAccessMixin {

    @Redirect(
        method = "getBiome",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/LevelReader;getChunk(IILnet/minecraft/world/level/chunk/status/ChunkStatus;Z)Lnet/minecraft/world/level/chunk/ChunkAccess;"
        ),
        require = 0
    )
    default ChunkAccess akiasync$redirectBiomeChunk(LevelReader instance, int x, int z, ChunkStatus status, boolean create) {
        if (!create && instance instanceof ServerLevel serverLevel) {
            return akiasync$tryGetLoadedChunk(serverLevel, x, z, status, create);
        }
        return instance.getChunk(x, z, status, create);
    }

    @Unique
    private static ChunkAccess akiasync$tryGetLoadedChunk(ServerLevel level, int x, int z, ChunkStatus status, boolean create) {
        try {
            
            LevelChunk chunk = level.getChunkSource().getChunkNow(x, z);
            if (chunk != null) {
                return chunk;
            }
        } catch (Exception e) {
            
        }
        
        return level.getChunk(x, z, status, create);
    }
}
