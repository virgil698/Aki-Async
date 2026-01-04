package org.virgil.akiasync.mixin.accessor;

import ca.spottedleaf.moonrise.patches.chunk_system.level.entity.ChunkEntitySlices;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;


@Mixin(ca.spottedleaf.moonrise.patches.chunk_system.level.entity.EntityLookup.class)
public interface EntityLookupAccessor {
    
    @Invoker("getSlicesUnsafe")
    ChunkEntitySlices[] invokeGetSlicesUnsafe(AABB boundingBox);
}
