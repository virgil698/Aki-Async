package org.virgil.akiasync.mixin.mixins.secureseed.structure;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.virgil.akiasync.mixin.bridge.BridgeManager;
import org.virgil.akiasync.mixin.secureseed.crypto.Globals;
import org.virgil.akiasync.mixin.secureseed.crypto.random.WorldgenCryptoRandom;

@Mixin(RandomSpreadStructurePlacement.class)
public abstract class RandomSpreadStructurePlacementMixin {
    
    private static volatile Boolean cached_enabled = null;

    @Inject(
        method = "getPotentialStructureChunk",
        at = @At("HEAD"),
        cancellable = true
    )
    private void securePotentialChunk(
            long seed,
            int x,
            int z,
            CallbackInfoReturnable<ChunkPos> cir) {
        
        if (!isEnabled()) {
            return;
        }

        WorldgenCryptoRandom random = new WorldgenCryptoRandom(
            x, z,
            Globals.Salt.POTENTIONAL_FEATURE,
            seed
        );

        int offsetX = random.nextInt(24);
        int offsetZ = random.nextInt(24);

        cir.setReturnValue(new ChunkPos(x * 32 + offsetX, z * 32 + offsetZ));
    }

    private static boolean isEnabled() {
        if (cached_enabled == null) {
            var bridge = BridgeManager.getBridge();
            cached_enabled = bridge != null && 
                           bridge.isSecureSeedEnabled() && 
                           bridge.isSecureSeedProtectStructures();
        }
        return cached_enabled;
    }

    public static void resetCache() {
        cached_enabled = null;
    }
}
