package org.virgil.akiasync.mixin.mixins.secureseed;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.virgil.akiasync.mixin.bridge.Bridge;
import org.virgil.akiasync.mixin.bridge.BridgeManager;
import org.virgil.akiasync.mixin.secureseed.crypto.Globals;
import org.virgil.akiasync.mixin.secureseed.crypto.random.WorldgenCryptoRandom;

@Mixin(ChunkGenerator.class)
public abstract class ChunkGeneratorMixin {

  @Unique
  private static volatile Boolean cachedEnabled = null;

  @ModifyVariable(
      method = "applyBiomeDecoration",
      at = @At(
          value = "STORE",
          ordinal = 0,
          target = "Lnet/minecraft/world/level/levelgen/WorldgenRandom;"
              + "<init>(Lnet/minecraft/util/RandomSource;)V"
      )
  )
  private WorldgenRandom aki$replaceRandomDecoration(
      WorldgenRandom originalRandom,
      BlockPos blockPos) {
    
    if (!aki$isEnabled()) {
      return originalRandom;
    }

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
          target = "Lnet/minecraft/world/level/levelgen/WorldgenRandom;"
              + "<init>(Lnet/minecraft/util/RandomSource;)V"
      )
  )
  private WorldgenRandom aki$replaceRandomStructures(
      WorldgenRandom originalRandom,
      ChunkPos chunkPos) {
    
    if (!aki$isEnabled()) {
      return originalRandom;
    }

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
          target = "Lnet/minecraft/world/level/levelgen/WorldgenRandom;"
              + "setLargeFeatureSeed(JII)V"
      )
  )
  private void aki$swallowSetLargeFeatureSeed(
      WorldgenRandom instance,
      long seed,
      int chunkX,
      int chunkZ) {
    
    if (!aki$isEnabled()) {
      instance.setLargeFeatureSeed(seed, chunkX, chunkZ);
    }
    
  }

  @Unique
  private static boolean aki$isEnabled() {
    if (cachedEnabled == null) {
      Bridge bridge = BridgeManager.getBridge();
      cachedEnabled = bridge != null && bridge.isSecureSeedEnabled();
    }
    return cachedEnabled;
  }

  public static void resetCache() {
    cachedEnabled = null;
  }
}
