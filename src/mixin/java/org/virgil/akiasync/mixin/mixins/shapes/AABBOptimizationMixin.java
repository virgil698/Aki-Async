package org.virgil.akiasync.mixin.mixins.shapes;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(AABB.class)
public class AABBOptimizationMixin {
    
    @Shadow @Final public double minX;
    @Shadow @Final public double minY;
    @Shadow @Final public double minZ;
    @Shadow @Final public double maxX;
    @Shadow @Final public double maxY;
    @Shadow @Final public double maxZ;
    
    @Unique
    private static volatile boolean enabled = true;
    @Unique
    private static volatile boolean initialized = false;
    
    /**
     * @author AkiAsync (from Nitori/Lithium)
     * @reason 避免调用 axis.choose()，使用 ordinal() 直接索引
     */
    @Overwrite
    public double min(Direction.Axis axis) {
        if (!initialized) {
            akiasync$initConfig();
        }
        
        if (enabled) {
            return switch (axis.ordinal()) {
                case 0 -> this.minX;  
                case 1 -> this.minY;  
                case 2 -> this.minZ;  
                default -> throw new IllegalArgumentException("Invalid axis: " + axis);
            };
        }
        
        return axis.choose(this.minX, this.minY, this.minZ);
    }
    
    /**
     * @author AkiAsync (from Nitori/Lithium)
     * @reason 避免调用 axis.choose()，使用 ordinal() 直接索引
     */
    @Overwrite
    public double max(Direction.Axis axis) {
        if (!initialized) {
            akiasync$initConfig();
        }
        
        if (enabled) {
            return switch (axis.ordinal()) {
                case 0 -> this.maxX;  
                case 1 -> this.maxY;  
                case 2 -> this.maxZ;  
                default -> throw new IllegalArgumentException("Invalid axis: " + axis);
            };
        }
        
        return axis.choose(this.maxX, this.maxY, this.maxZ);
    }
    
    @Unique
    private static synchronized void akiasync$initConfig() {
        if (initialized) return;
        
        try {
            org.virgil.akiasync.mixin.bridge.Bridge bridge = 
                org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
            
            if (bridge != null) {
                enabled = bridge.isShapeOptimizationEnabled();
                
                bridge.debugLog("[AkiAsync] AABBOptimizationMixin initialized: enabled=" + enabled + 
                    " (from Nitori/Lithium)");
            }
        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "AABBOptimization", "init", e);
        }
        
        initialized = true;
    }
}
