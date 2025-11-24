package org.virgil.akiasync.mixin.mixins.secureseed;

import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.virgil.akiasync.mixin.bridge.BridgeManager;
import org.virgil.akiasync.mixin.secureseed.crypto.random.WorldgenCryptoRandom;

@Mixin(Slime.class)
public abstract class SlimeMixin {
    
    private static volatile Boolean cached_enabled = null;

    @Redirect(
        method = "checkSlimeSpawnRules",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;getRandom()Lnet/minecraft/util/RandomSource;"
        )
    )
    private static RandomSource replaceSlimeRandom(Level level, EntityType entityType, Level level2) {
        if (!isEnabled()) {
            return level.getRandom();
        }
        
        int chunkX = ((int) Math.floor(level.getSharedSpawnPos().getX())) >> 4;
        int chunkZ = ((int) Math.floor(level.getSharedSpawnPos().getZ())) >> 4;
        
        return WorldgenCryptoRandom.seedSlimeChunk(chunkX, chunkZ);
    }

    private static boolean isEnabled() {
        if (cached_enabled == null) {
            var bridge = BridgeManager.getBridge();
            cached_enabled = bridge != null && 
                           bridge.isSecureSeedEnabled() && 
                           bridge.isSecureSeedProtectSlimes();
        }
        return cached_enabled;
    }

    public static void resetCache() {
        cached_enabled = null;
    }
}
