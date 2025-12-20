package org.virgil.akiasync.mixin.mixins.shapes;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;
import org.virgil.akiasync.mixin.util.collections.Object2BooleanCacheTable;

@Mixin(Block.class)
public class BlockShapeCacheMixin {
    
    @Unique
    private static volatile boolean enabled = true;
    @Unique
    private static volatile boolean initialized = false;
    @Unique
    private static volatile int cacheSize = 512;
    @Unique
    private static Object2BooleanCacheTable<VoxelShape> FULL_CUBE_CACHE = null;

    /**
     * @author AkiAsync
     * @reason 缓存形状检测结果，性能提升 2-3x
     */
    @Overwrite
    public static boolean isShapeFullBlock(VoxelShape shape) {
        if (!initialized) {
            akiasync$initCache();
        }
        
        if (!enabled || FULL_CUBE_CACHE == null) {
            
            return !Shapes.joinIsNotEmpty(Shapes.block(), shape, BooleanOp.NOT_SAME);
        }
        
        return FULL_CUBE_CACHE.get(shape);
    }
    
    @Unique
    private static synchronized void akiasync$initCache() {
        if (initialized) return;
        
        try {
            org.virgil.akiasync.mixin.bridge.Bridge bridge = 
                org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
            
            if (bridge != null) {
                enabled = bridge.isShapeOptimizationEnabled() && 
                         bridge.isShapeBlockShapeCache();
                cacheSize = bridge.getShapeBlockShapeCacheSize();
                
                if (enabled) {
                    FULL_CUBE_CACHE = new Object2BooleanCacheTable<>(
                        cacheSize,
                        s -> !Shapes.joinIsNotEmpty(Shapes.block(), s, BooleanOp.NOT_SAME)
                    );
                }
                
                bridge.debugLog("[AkiAsync] BlockShapeCacheMixin initialized: enabled=" + enabled + 
                    ", cacheSize=" + cacheSize);
            }
        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "BlockShapeCache", "init", e);
        }
        
        initialized = true;
    }
}
