package org.virgil.akiasync.mixin.mixins.shapes;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.virgil.akiasync.mixin.util.collections.Object2BooleanCacheTable;

@Mixin(Block.class)
public class BlockShapeCacheMixin {
    private static final Object2BooleanCacheTable<VoxelShape> FULL_CUBE_CACHE = new Object2BooleanCacheTable<>(
            512,
            shape -> !Shapes.joinIsNotEmpty(Shapes.block(), shape, BooleanOp.NOT_SAME)
    );

    @Overwrite
    public static boolean isShapeFullBlock(VoxelShape shape) {
        return FULL_CUBE_CACHE.get(shape);
    }
}
