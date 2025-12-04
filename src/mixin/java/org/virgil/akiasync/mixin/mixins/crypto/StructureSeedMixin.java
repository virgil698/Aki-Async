package org.virgil.akiasync.mixin.mixins.crypto;

import net.minecraft.world.level.levelgen.WorldgenRandom;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.virgil.akiasync.mixin.bridge.Bridge;
import org.virgil.akiasync.mixin.bridge.BridgeManager;


@Mixin(WorldgenRandom.class)
public class StructureSeedMixin {
    
    @Shadow
    public void setSeed(long seed) {}
    
    
    @Inject(
        method = "setLargeFeatureSeed(JII)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void encryptStructureSeed(long baseSeed, int chunkX, int chunkZ, CallbackInfo ci) {
        Bridge bridge = BridgeManager.getBridge();
        if (bridge == null) {
            return;
        }
        

        if (!bridge.isSeedProtectionEnabled()) {
            return;
        }
        

        if (!bridge.isSeedEncryptionProtectStructures()) {
            return;
        }
        
        try {
            long encryptedSeed;
            

            if (bridge.isQuantumSeedEnabled()) {
                encryptedSeed = aki$encryptStructureWithQuantumSeed(bridge, baseSeed, chunkX, chunkZ);
            }

            else if (bridge.isSecureSeedEnabled()) {
                encryptedSeed = aki$encryptStructureWithSecureSeed(bridge, baseSeed, chunkX, chunkZ);
            }
            else {
                return;
            }
            

            setSeed(encryptedSeed);
            WorldgenRandom self = (WorldgenRandom)(Object)this;
            long randomLong = self.nextLong();
            long randomLong1 = self.nextLong();
            long l = chunkX * randomLong ^ chunkZ * randomLong1 ^ encryptedSeed;
            setSeed(l);
            ci.cancel();
        } catch (Exception e) {
            bridge.errorLog("[SeedEncryption] Failed to encrypt structure seed: %s", e.getMessage());
        }
    }
    
    
    private long aki$encryptStructureWithQuantumSeed(Bridge bridge, long originalSeed, int x, int z) {
        String dimension = "minecraft:overworld";
        long gameTime = 0;
        
        return bridge.getEncryptedSeed(
            originalSeed,
            x >> 4,
            z >> 4,
            dimension,
            "STRUCTURE",
            gameTime
        );
    }
    
    
    private long aki$encryptStructureWithSecureSeed(Bridge bridge, long originalSeed, int x, int z) {
        long[] worldSeed = bridge.getSecureSeedWorldSeed();
        if (worldSeed == null || worldSeed.length == 0) {
            return originalSeed;
        }
        

        long mixed = originalSeed;
        

        for (int i = 0; i < Math.min(worldSeed.length, 12); i++) {
            mixed ^= worldSeed[i];
            mixed = Long.rotateLeft(mixed, 11 + i);
            mixed *= 0x9E3779B97F4A7C15L;
        }
        

        mixed ^= ((long) x << 32) | (z & 0xFFFFFFFFL);
        mixed = Long.rotateLeft(mixed, 23);
        

        mixed ^= worldSeed[worldSeed.length - 1];
        mixed = Long.rotateLeft(mixed, 31);
        
        return mixed;
    }
}
