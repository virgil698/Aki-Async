package org.virgil.akiasync.mixin.async.explosion.merge;

import net.minecraft.world.entity.item.PrimedTnt;

public interface MergeableTNT {

    int aki$getMergeCount();

    void aki$setMergeCount(int count);

    void aki$mergeWith(PrimedTnt other);

    boolean aki$canMergeWith(PrimedTnt other);
}
