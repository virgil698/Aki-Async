package org.virgil.akiasync.mixin.mixins.crypto.seed;

import net.minecraft.world.level.levelgen.WorldgenRandom;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.virgil.akiasync.mixin.bridge.Bridge;
import org.virgil.akiasync.mixin.bridge.BridgeManager;

@Mixin(WorldgenRandom.class)
public class DecorationSeedMixin {

    @Shadow
    public void setSeed(long seed) {}

    @Inject(
        method = "setDecorationSeed(JII)J",
        at = @At("HEAD"),
        cancellable = true
    )
    private void encryptDecorationSeed(long levelSeed, int minChunkBlockX, int minChunkBlockZ, CallbackInfoReturnable<Long> cir) {

        Bridge bridge = BridgeManager.getBridge();
        if (bridge == null) {
            return;
        }

        if (!bridge.isSeedProtectionEnabled()) {
            return;
        }

        if (!bridge.isSeedEncryptionProtectOres()) {
            return;
        }

        try {
            long encryptedSeed;

            if (bridge.isQuantumSeedEnabled()) {
                encryptedSeed = aki$encryptDecorationWithQuantumSeed(bridge, levelSeed, minChunkBlockX, minChunkBlockZ);
            }

            else if (bridge.isSecureSeedEnabled()) {
                encryptedSeed = aki$encryptDecorationWithSecureSeed(bridge, levelSeed, minChunkBlockX, minChunkBlockZ);
            }
            else {
                return;
            }

            setSeed(encryptedSeed);
            WorldgenRandom self = (WorldgenRandom)(Object)this;
            long l = self.nextLong() | 1L;
            long l1 = self.nextLong() | 1L;
            long l2 = minChunkBlockX * l + minChunkBlockZ * l1 ^ encryptedSeed;
            setSeed(l2);
            cir.setReturnValue(l2);
        } catch (Exception e) {
            bridge.errorLog("[SeedEncryption] Failed to encrypt decoration seed: %s", e.getMessage());
        }
    }

    private long aki$encryptDecorationWithQuantumSeed(Bridge bridge, long originalSeed, int x, int z) {

        String dimension = "minecraft:overworld";
        long gameTime = 0;

        return bridge.getEncryptedSeed(
            originalSeed,
            x >> 4,
            z >> 4,
            dimension,
            "DECORATION",
            gameTime
        );
    }

    private long aki$encryptDecorationWithSecureSeed(Bridge bridge, long originalSeed, int x, int z) {

        long[] worldSeed = bridge.getSecureSeedWorldSeed();
        if (worldSeed == null || worldSeed.length == 0) {
            return originalSeed;
        }

        long mixed = originalSeed;

        for (int i = 0; i < Math.min(worldSeed.length, 8); i++) {
            mixed ^= worldSeed[i];
            mixed = Long.rotateLeft(mixed, 7 + i);
        }

        mixed ^= ((long) x << 32) | (z & 0xFFFFFFFFL);
        mixed = Long.rotateLeft(mixed, 17);

        mixed ^= worldSeed[worldSeed.length - 1];

        return mixed;
    }
}
