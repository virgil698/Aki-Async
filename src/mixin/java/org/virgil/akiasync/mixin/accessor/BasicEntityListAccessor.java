package org.virgil.akiasync.mixin.accessor;

import ca.spottedleaf.moonrise.patches.chunk_system.level.entity.ChunkEntitySlices;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;


@Mixin(ChunkEntitySlices.BasicEntityList.class)
public interface BasicEntityListAccessor {
    
    @Accessor("sectionY")
    int getSectionY();
    
    @Accessor("storage")
    Entity[] getStorage();
}
