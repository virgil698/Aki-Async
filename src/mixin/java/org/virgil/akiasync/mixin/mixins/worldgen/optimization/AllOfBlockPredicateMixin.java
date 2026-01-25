package org.virgil.akiasync.mixin.mixins.worldgen.optimization;

import org.virgil.akiasync.mixin.util.CombinedBlockPredicateExtension;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.blockpredicates.AllOfPredicate;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(AllOfPredicate.class)
public abstract class AllOfBlockPredicateMixin implements CombinedBlockPredicateExtension {

    @Overwrite
    public boolean test(WorldGenLevel level, BlockPos pos) {
        for (BlockPredicate predicate : this.akiasync$getPredicatesArray()) {
            if (!predicate.test(level, pos)) {
                return false;
            }
        }
        return true;
    }
}
