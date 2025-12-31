package org.virgil.akiasync.mixin.mixins.chunk;

import net.minecraft.world.level.chunk.PalettedContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;


@Mixin(PalettedContainer.class)
public class PalettedContainerLockRemovalMixin {

    @Unique
    private static volatile boolean initialized = false;
    @Unique
    private static volatile boolean enabled = true;

    
    @Overwrite
    public void acquire() {
        if (!initialized) {
            akiasync$initConfig();
        }
        
        if (!enabled) {
            
            
        }
        
    }

    
    @Overwrite
    public void release() {
        if (!enabled) {
            
        }
        
    }

    @Unique
    private static synchronized void akiasync$initConfig() {
        if (initialized) {
            return;
        }

        try {
            org.virgil.akiasync.mixin.bridge.Bridge bridge =
                org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();

            if (bridge != null) {
                enabled = bridge.isPalettedContainerLockRemovalEnabled();
                bridge.debugLog("[PalettedContainerLockRemoval] Initialized: enabled=%s", enabled);
            }
        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "PalettedContainerLockRemoval", "initConfig", e);
        }

        initialized = true;
    }
}
