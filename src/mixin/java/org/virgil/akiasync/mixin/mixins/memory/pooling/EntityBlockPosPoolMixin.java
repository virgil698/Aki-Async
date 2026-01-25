package org.virgil.akiasync.mixin.mixins.memory.pooling;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Entity.class)
public class EntityBlockPosPoolMixin {

    @Unique
    public final BlockPos.MutableBlockPos akiasync$cachedBlockPos = new BlockPos.MutableBlockPos();
}
