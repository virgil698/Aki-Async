package org.virgil.akiasync.mixin.mixins.secureseed;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.feature.GeodeFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.virgil.akiasync.mixin.bridge.BridgeManager;
import org.virgil.akiasync.mixin.secureseed.crypto.Globals;
import org.virgil.akiasync.mixin.secureseed.crypto.random.WorldgenCryptoRandom;

@Mixin(GeodeFeature.class)
public abstract class GeodeFeatureMixin {
    
    private static volatile Boolean cached_enabled = null;

    @ModifyVariable(
        method = "place",
        at = @At("HEAD"),
        argsOnly = true
    )
    private RandomSource replaceGeodeRandom(RandomSource random) {
        if (!isEnabled()) {
            return random;
        }

        return new WorldgenCryptoRandom(
            0, 0,
            Globals.Salt.GEODE_FEATURE,
            System.nanoTime()
        );
    }

    private static boolean isEnabled() {
        if (cached_enabled == null) {
            var bridge = BridgeManager.getBridge();
            cached_enabled = bridge != null && 
                           bridge.isSecureSeedEnabled() && 
                           bridge.isSecureSeedProtectOres();
        }
        return cached_enabled;
    }

    public static void resetCache() {
        cached_enabled = null;
    }
}
