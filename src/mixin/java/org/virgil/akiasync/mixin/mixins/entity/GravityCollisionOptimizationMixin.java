package org.virgil.akiasync.mixin.mixins.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class GravityCollisionOptimizationMixin {
    
    @Shadow public abstract AABB getBoundingBox();
    @Shadow public abstract double getX();
    @Shadow public abstract double getZ();
    @Shadow public abstract void setDeltaMovement(Vec3 vec3);
    
    @Unique
    private static volatile boolean enabled = true;
    @Unique
    private static volatile boolean initialized = false;
    
    @Unique
    private static volatile long totalGravityChecks = 0;
    @Unique
    private static volatile long optimizedGravityChecks = 0;
    
    @Unique
    private static synchronized void akiasync$initGravityOptimization() {
        if (initialized) {
            return;
        }
        
        try {
            org.virgil.akiasync.mixin.bridge.Bridge bridge =
                org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
            
            if (bridge != null) {
                enabled = true;
                bridge.debugLog("[GravityCollisionOptimization] Initialized: enabled=%s", enabled);
                
                initialized = true;
            }
        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "GravityCollisionOptimization", "init", e);
        }
    }
    
    @Inject(
        method = "move",
        at = @At("HEAD"),
        cancellable = true
    )
    private void optimizeGravityCollision(MoverType type, Vec3 movement, CallbackInfo ci) {
        if (!initialized) {
            akiasync$initGravityOptimization();
        }
        
        if (!enabled) {
            return;
        }
        
        if (movement.y >= 0 || movement.x != 0 || movement.z != 0) {
            return;
        }
        
        totalGravityChecks++;
        
        Entity self = (Entity) (Object) this;
        Vec3 result = akiasync$fastGravityCheck(self, movement);
        
        if (result != null) {
            self.setDeltaMovement(result);
            optimizedGravityChecks++;
            ci.cancel();
        }
    }
    
    @Unique
    private Vec3 akiasync$fastGravityCheck(Entity entity, Vec3 movement) {
        try {
            AABB box = entity.getBoundingBox();
            
            double feetY = box.minY - 0.0001;
            BlockPos feetPos = BlockPos.containing(entity.getX(), feetY, entity.getZ());
            
            BlockState state = entity.level().getBlockState(feetPos);
            
            if (state.isAir()) {
                return null;
            }
            
            VoxelShape shape = state.getCollisionShape(entity.level(), feetPos);

            if (shape.isEmpty()) {
                return null;
            }
            
            double blockTopY = shape.max(Direction.Axis.Y) + feetPos.getY();
            
            if (blockTopY >= box.minY) {
                return Vec3.ZERO;
            }
            
            double newFeetY = box.minY + movement.y;
            
            if (newFeetY <= blockTopY) {
                double adjustedY = blockTopY - box.minY;
                return new Vec3(0, adjustedY, 0);
            }
            
            return null;
            
        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "GravityCollisionOptimization", "fastGravityCheck", e);
            return null;
        }
    }
    
    @Unique
    private static String akiasync$getStats() {
        if (totalGravityChecks == 0) {
            return "GravityCollisionOptimization: No data";
        }
        
        double optimizationRate = (double) optimizedGravityChecks / totalGravityChecks * 100.0;
        
        return String.format(
            "GravityCollisionOptimization: total=%d, optimized=%d (%.1f%%)",
            totalGravityChecks, optimizedGravityChecks, optimizationRate
        );
    }
    
    @Unique
    private static void akiasync$resetStats() {
        totalGravityChecks = 0;
        optimizedGravityChecks = 0;
    }
}
