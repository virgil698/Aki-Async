package org.virgil.akiasync.mixin.mixins.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "net.minecraft.world.level.material.FlowingFluid")
public class FlowingFluidOptimizationMixin {

    private static volatile boolean initialized = false;
    private static volatile boolean enabled = true;
    private static volatile boolean debugEnabled = false;

    @Redirect(
            method = "canHoldAnyFluid(Lnet/minecraft/world/level/block/state/BlockState;)Z",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/tags/TagKey;)Z"),
            require = 0
    )
    private static boolean optimizeSignCheck(BlockState blockState, TagKey<Block> tagKey) {
        if (!initialized) {
            initFluidOptimization();
        }
        
        if (!enabled) {
            return blockState.is(tagKey);
        }
        
        if (tagKey.location().equals(BlockTags.SIGNS.location())) {
            boolean result = blockState.getBlock() instanceof SignBlock;
            
            if (debugEnabled) {
                org.virgil.akiasync.mixin.bridge.Bridge bridge = 
                    org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
                if (bridge != null) {
                    bridge.debugLog("[AkiAsync-Fluid] SIGNS check optimized: block=%s, result=%s", 
                        blockState.getBlock().getDescriptionId(), result);
                }
            }
            
            return result;
        }
        
        boolean result = blockState.is(tagKey);
        
        if (debugEnabled) {
            org.virgil.akiasync.mixin.bridge.Bridge bridge = 
                org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
            if (bridge != null) {
                bridge.debugLog("[AkiAsync-Fluid] Tag check fallback: block=%s, tag=%s, result=%s", 
                    blockState.getBlock().getDescriptionId(), 
                    tagKey.location(), 
                    result);
            }
        }
        
        return result;
    }
    
    private static synchronized void initFluidOptimization() {
        if (initialized) return;
        
        org.virgil.akiasync.mixin.bridge.Bridge bridge = 
            org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        
        if (bridge != null) {
            enabled = bridge.isFluidOptimizationEnabled();
            debugEnabled = bridge.isFluidDebugEnabled();
            bridge.debugLog("[AkiAsync] FlowingFluidOptimizationMixin initialized:");
            bridge.debugLog("[AkiAsync]   - Optimization enabled: %s", enabled);
            bridge.debugLog("[AkiAsync]   - Debug enabled: %s", debugEnabled);
            bridge.debugLog("[AkiAsync]   - SIGNS tag: %s", BlockTags.SIGNS.location());
        } else {
            
            enabled = true;
            debugEnabled = false;
            System.out.println("[AkiAsync] FlowingFluidOptimizationMixin initialized without bridge (defaults: enabled=true, debug=false)");
        }
        
        initialized = true;
    }
}
