package org.virgil.akiasync.mixin.mixins.worldgen.optimization;

import org.virgil.akiasync.mixin.util.CombinedBlockPredicateExtension;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.blockpredicates.CombiningPredicate;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.List;

@Mixin(CombiningPredicate.class)
public class CombinedBlockPredicateMixin implements CombinedBlockPredicateExtension {

    @Shadow @Final protected List<BlockPredicate> predicates;

    @Unique
    private BlockPredicate[] akiasync$predicatesArray;

    @Override
    public BlockPredicate[] akiasync$getPredicatesArray() {
        BlockPredicate[] predicateArray = this.akiasync$predicatesArray;
        if (predicateArray == null) {
            this.akiasync$predicatesArray = predicateArray = this.predicates.toArray(BlockPredicate[]::new);
        }
        return predicateArray;
    }
}
