package org.virgil.akiasync.mixin.mixins.secureseed;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.virgil.akiasync.mixin.bridge.BridgeManager;
import org.virgil.akiasync.mixin.secureseed.crypto.Globals;
import org.virgil.akiasync.mixin.secureseed.crypto.random.WorldgenCryptoRandom;

@Mixin(ChunkGenerator.class)
public abstract class ChunkGeneratorMixin {
    
    private static volatile Boolean cached_enabled = null;

    @ModifyVariable(
            method = "applyBiomeDecoration",
            at = @At(
                value = "STORE",
                ordinal = 0,
                target = "Lnet/minecraft/world/level/levelgen/WorldgenRandom;<init>(Lnet/minecraft/util/RandomSource;)V"
            )
    )
    private WorldgenRandom replaceRandomDecoration(WorldgenRandom value, BlockPos blockPos) {
        if (!isEnabled()) return value;
        
        return new WorldgenCryptoRandom(
            blockPos.getX(),
            blockPos.getZ(),
            Globals.Salt.UNDEFINED,
            0
        );
    }

    @ModifyVariable(
            method = "method_41041",
            at = @At(
                value = "STORE",
                ordinal = 0,
                target = "Lnet/minecraft/world/level/levelgen/WorldgenRandom;<init>(Lnet/minecraft/util/RandomSource;)V"
            )
    )
    private WorldgenRandom replaceRandomStructures(WorldgenRandom value, ChunkPos chunkPos) {
        if (!isEnabled()) return value;
        
        return new WorldgenCryptoRandom(
            chunkPos.x,
            chunkPos.z,
            Globals.Salt.GENERATE_FEATURE,
            0
        );
    }

    @Redirect(
        method = "method_41041",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/levelgen/WorldgenRandom;setLargeFeatureSeed(JII)V"
        )
    )
    public void swallowSetLargeFeatureSeed(WorldgenRandom instance, long l, int i, int j) {
        if (!isEnabled()) {
            instance.setLargeFeatureSeed(l, i, j);
        }
    }

    private static boolean isEnabled() {
        if (cached_enabled == null) {
            var bridge = BridgeManager.getBridge();
            cached_enabled = bridge != null && bridge.isSecureSeedEnabled();
        }
        return cached_enabled;
    }

    public static void resetCache() {
        cached_enabled = null;
    }
}
