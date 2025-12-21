package org.virgil.akiasync.mixin.accessor;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Entity.class)
public interface EntityBlockPosAccessor {
    
    @Accessor("akiasync$cachedBlockPos")
    BlockPos.MutableBlockPos getCachedBlockPos();
}
