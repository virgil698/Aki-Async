package org.virgil.akiasync.mixin.mixins.blockentity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

@SuppressWarnings("unused")
@Mixin(value = HopperBlockEntity.class, priority = 1200)
public class HopperChainAsyncMixin {
    
    @Inject(method = "tryMoveItems", at = @At("HEAD"), cancellable = true)
    private static void aki$asyncHopperTick(Level level, BlockPos pos, BlockState state, 
                                            HopperBlockEntity hopper, java.util.function.BooleanSupplier isEmpty,
                                            CallbackInfoReturnable<Boolean> cir) {
        if (!(level instanceof ServerLevel sl)) return;
        
        org.virgil.akiasync.mixin.bridge.Bridge bridge = 
            org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        if (bridge == null || !bridge.isAsyncHopperChainEnabled()) return;
        
        org.virgil.akiasync.mixin.async.hopper.HopperChainExecutor.submit(sl, pos, () -> {
            try {
                java.lang.reflect.Method method = HopperBlockEntity.class.getDeclaredMethod(
                    "tryMoveItems", Level.class, BlockPos.class, BlockState.class, 
                    HopperBlockEntity.class, java.util.function.BooleanSupplier.class);
                method.setAccessible(true);
                method.invoke(null, sl, pos, state, hopper, isEmpty);
            } catch (Exception e) {
            }
        });
        
        cir.setReturnValue(false);
    }
}

