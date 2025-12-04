package org.virgil.akiasync.mixin.mixins.crypto;

import net.minecraft.world.level.levelgen.WorldgenRandom;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.virgil.akiasync.mixin.bridge.Bridge;
import org.virgil.akiasync.mixin.bridge.BridgeManager;


@Mixin(WorldgenRandom.class)
public class SlimeChunkMixin {
    
    
    @ModifyVariable(
        method = "seedSlimeChunk",
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 0
    )
    private static long encryptSlimeChunkSeed(long worldSeed, int x, int z, long scrambler) {
        Bridge bridge = BridgeManager.getBridge();
        if (bridge == null) {
            return worldSeed;
        }
        

        if (!bridge.isSeedProtectionEnabled()) {
            return worldSeed;
        }
        

        if (!bridge.isSeedEncryptionProtectSlimes()) {
            return worldSeed;
        }
        
        try {

            if (bridge.isQuantumSeedEnabled()) {
                return aki$encryptSlimeWithQuantumSeed(bridge, worldSeed, x, z);
            }
            

            if (bridge.isSecureSeedEnabled()) {
                return aki$encryptSlimeWithSecureSeed(bridge, worldSeed, x, z);
            }
        } catch (Exception e) {
            bridge.errorLog("[SeedEncryption] Failed to encrypt slime chunk seed: %s", e.getMessage());
        }
        
        return worldSeed;
    }
    
    
    private static long aki$encryptSlimeWithQuantumSeed(Bridge bridge, long worldSeed, int x, int z) {
        String dimension = "minecraft:overworld";
        long gameTime = 0;
        
        return bridge.getEncryptedSeed(
            worldSeed,
            x,
            z,
            dimension,
            "SLIME",
            gameTime
        );
    }
    
    
    private static long aki$encryptSlimeWithSecureSeed(Bridge bridge, long worldSeed, int x, int z) {
        long[] secureSeed = bridge.getSecureSeedWorldSeed();
        if (secureSeed == null || secureSeed.length == 0) {
            return worldSeed;
        }
        

        long mixed = worldSeed;
        

        for (int i = 0; i < 4; i++) {
            mixed ^= secureSeed[i];
            mixed = Long.rotateLeft(mixed, 13 + i * 3);
        }
        

        mixed ^= ((long) x << 32) | (z & 0xFFFFFFFFL);
        mixed = Long.rotateLeft(mixed, 23);
        

        for (int i = secureSeed.length - 4; i < secureSeed.length; i++) {
            mixed ^= secureSeed[i];
            mixed = Long.rotateLeft(mixed, 19);
        }
        
        return mixed;
    }
}
