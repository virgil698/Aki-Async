package org.virgil.akiasync.mixin.mixins.worldgen.optimization;

import org.virgil.akiasync.mixin.util.ObjectCachingUtils;
import net.minecraft.world.level.levelgen.feature.OreFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.BitSet;

@Mixin(OreFeature.class)
public class OreFeaturePoolingMixin {

    @Redirect(
        method = "doPlace",
        at = @At(value = "NEW", target = "java/util/BitSet")
    )
    private BitSet redirectNewBitSet(int nbits) {
        return ObjectCachingUtils.getCachedOrNewBitSet(nbits);
    }
}
