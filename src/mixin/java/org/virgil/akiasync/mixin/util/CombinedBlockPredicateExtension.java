package org.virgil.akiasync.mixin.util;

import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;

public interface CombinedBlockPredicateExtension {

    BlockPredicate[] akiasync$getPredicatesArray();
}
