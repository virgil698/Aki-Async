package org.virgil.akiasync.mixin.mixins.redstone;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.virgil.akiasync.mixin.async.redstone.PandaWireEvaluator;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Mixin(RedStoneWireBlock.class)
public class RedstoneWireOptimizationMixin {
    
    @Unique
    private static final Map<ServerLevel, PandaWireEvaluator> aki$evaluatorCache = new ConcurrentHashMap<>();

    
    @Inject(method = "updatePowerStrength", at = @At("HEAD"), cancellable = true)
    private void aki$usePandaWire(Level level, BlockPos pos, BlockState state, 
                                  Orientation orientation, boolean updateShape, 
                                  CallbackInfo ci) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        org.virgil.akiasync.mixin.bridge.Bridge bridge =
            org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        
        if (bridge == null || !bridge.isUsePandaWireAlgorithm()) {
            return;
        }


        PandaWireEvaluator evaluator = aki$evaluatorCache.computeIfAbsent(
            serverLevel, 
            l -> new PandaWireEvaluator(l, (RedStoneWireBlock)(Object)this)
        );
        

        evaluator.evaluateWire(pos, state);

        if (bridge.isDebugLoggingEnabled()) {
            bridge.debugLog("[AkiAsync-Redstone] Enhanced PandaWire evaluation at %s", pos);
        }

        ci.cancel();
    }
    
    
    @Unique
    public static void aki$clearCache(ServerLevel level) {
        aki$evaluatorCache.remove(level);
    }
    
    
    @Unique
    public static void clearAllCaches() {
        aki$evaluatorCache.clear();
    }
    
    
    @Unique
    public static int getEvaluatorCount() {
        return aki$evaluatorCache.size();
    }
}
