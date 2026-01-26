package org.virgil.akiasync.mixin.accessor;

import net.minecraft.world.level.levelgen.NoiseChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(NoiseChunk.class)
public interface NoiseChunkAccessor {

    @Accessor("cellStartBlockX")
    int getCellStartBlockX();

    @Accessor("cellStartBlockZ")
    int getCellStartBlockZ();

    @Accessor("cellStartBlockY")
    int getCellStartBlockY();

    @Accessor("inCellX")
    int getInCellX();

    @Accessor("inCellY")
    int getInCellY();

    @Accessor("inCellZ")
    int getInCellZ();

    @Accessor("cellWidth")
    int getCellWidth();

    @Accessor("cellHeight")
    int getCellHeight();

    @Accessor("inCellX")
    void setInCellX(int value);

    @Accessor("inCellY")
    void setInCellY(int value);

    @Accessor("inCellZ")
    void setInCellZ(int value);

    @Accessor("arrayIndex")
    void setArrayIndex(int value);
}
