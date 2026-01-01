package org.virgil.akiasync.mixin.mixins.shapes;

import it.unimi.dsi.fastutil.doubles.DoubleList;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.CubePointRange;
import net.minecraft.world.phys.shapes.CubeVoxelShape;
import net.minecraft.world.phys.shapes.DiscreteVoxelShape;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CubeVoxelShape.class)
public abstract class CubeVoxelShapeMixin extends VoxelShape {
    
    @Unique
    private static volatile boolean enabled = true;
    @Unique
    private static volatile boolean initialized = false;
    @Unique
    private static final Direction.Axis[] AXIS = Direction.Axis.values();

    @Unique
    private DoubleList[] list;
    
    protected CubeVoxelShapeMixin(DiscreteVoxelShape shape) {
        super(shape);
    }

    @Inject(method = "<init>(Lnet/minecraft/world/phys/shapes/DiscreteVoxelShape;)V", at = @At("RETURN"))
    private void onConstructed(DiscreteVoxelShape voxels, CallbackInfo ci) {
        if (!initialized) {
            akiasync$initConfig();
        }
        
        if (enabled) {
            this.list = new DoubleList[AXIS.length];

            for (Direction.Axis axis : AXIS) {
                this.list[axis.ordinal()] = new CubePointRange(voxels.getSize(axis));
            }
        }
    }

    @Inject(method = "getCoords", at = @At("HEAD"), cancellable = true)
    private void cacheGetCoords(Direction.Axis axis, CallbackInfoReturnable<DoubleList> cir) {
        if (!initialized) {
            akiasync$initConfig();
        }
        
        if (enabled && this.list != null && this.list[axis.ordinal()] != null) {
            cir.setReturnValue(this.list[axis.ordinal()]);
        }
    }
    
    @Unique
    private static synchronized void akiasync$initConfig() {
        if (initialized) return;
        
        try {
            org.virgil.akiasync.mixin.bridge.Bridge bridge = 
                org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
            
            if (bridge != null) {
                enabled = bridge.isShapeOptimizationEnabled() && 
                         bridge.isShapePrecomputeArrays();
                
                bridge.debugLog("[AkiAsync] CubeVoxelShapeMixin initialized: enabled=" + enabled);
            
                initialized = true;
            }
        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "CubeVoxelShape", "init", e);
        }
    }
}
