package org.virgil.akiasync.mixin.mixins.secureseed;

import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.virgil.akiasync.mixin.bridge.Bridge;
import org.virgil.akiasync.mixin.bridge.BridgeManager;
import org.virgil.akiasync.mixin.secureseed.crypto.random.WorldgenCryptoRandom;

@Mixin(Slime.class)
public abstract class SlimeMixin {

  @Unique
  private static volatile Boolean cachedEnabled = null;

  @Unique
  private static final int CHUNK_SHIFT = 4; 

  @Redirect(
      method = "checkSlimeSpawnRules",
      at = @At(
          value = "INVOKE",
          target = "Lnet/minecraft/world/level/Level;getRandom()"
              + "Lnet/minecraft/util/RandomSource;"
      )
  )
  private static RandomSource aki$replaceSlimeRandom(
      Level level,
      EntityType<?> entityType,
      Level level2) {
    
    if (!aki$isEnabled()) {
      return level.getRandom();
    }

    int chunkX = ((int) Math.floor(level.getSharedSpawnPos().getX())) >> CHUNK_SHIFT;
    int chunkZ = ((int) Math.floor(level.getSharedSpawnPos().getZ())) >> CHUNK_SHIFT;

    return WorldgenCryptoRandom.seedSlimeChunk(chunkX, chunkZ);
  }

  @Unique
  private static boolean aki$isEnabled() {
    if (cachedEnabled == null) {
      Bridge bridge = BridgeManager.getBridge();
      cachedEnabled = bridge != null
          && bridge.isSecureSeedEnabled()
          && bridge.isSecureSeedProtectSlimes();
    }
    return cachedEnabled;
  }

  public static void resetCache() {
    cachedEnabled = null;
  }
}
