package org.virgil.akiasync.mixin.mixins.spawn;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.virgil.akiasync.mixin.bridge.Bridge;
import org.virgil.akiasync.mixin.bridge.BridgeManager;

@Mixin(value = {ServerLevel.class, MinecraftServer.class}, priority = 900)
public class SpawnChunkRemovalMixin {
    
    @Unique
    private static volatile boolean enabled = false;
    @Unique
    private static volatile boolean initialized = false;
    
    @Unique
    private static synchronized void aki$init() {
        if (initialized) return;
        
        Bridge bridge = BridgeManager.getBridge();
        if (bridge != null) {
            enabled = bridge.isSpawnChunkRemovalEnabled();
            
            if (bridge.isDebugLoggingEnabled() && enabled) {
                bridge.debugLog("[AkiAsync-SpawnChunks] Spawn chunk removal enabled (Ksyxis-style)");
                bridge.debugLog("[AkiAsync-SpawnChunks]   - Target: Minecraft 1.21.8 / 1.21.10");
            }
        }
        
        initialized = true;
    }
    
    @ModifyVariable(
            method = "setDefaultSpawnPos(Lnet/minecraft/core/BlockPos;F)V",
            at = @At("STORE"),
            index = 5,
            require = 0
    )
    private int overrideSpawnChunkRadius(int spawnChunkRadius) {
        if (!initialized) {
            aki$init();
        }
        
        if (!enabled) {
            return spawnChunkRadius;
        }
        
        return 0;
    }
    
    @ModifyVariable(
            method = "prepareLevels",
            at = @At("STORE"),
            index = 5,
            require = 0
    )
    private int overridePrepareLevelsSpawnRadius(int spawnChunkRadius) {
        if (!initialized) {
            aki$init();
        }
        
        if (!enabled) {
            return spawnChunkRadius;
        }
        
        return 0;
    }
    
    @ModifyConstant(
            method = "prepareLevels",
            constant = @Constant(intValue = 441),
            require = 0
    )
    private int preventFreezing(int spawnChunks) {
        if (!initialized) {
            aki$init();
        }
        
        if (!enabled) {
            return spawnChunks;
        }
        
        return 0;
    }
}
