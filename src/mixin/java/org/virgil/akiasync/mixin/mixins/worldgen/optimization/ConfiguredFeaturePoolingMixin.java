package org.virgil.akiasync.mixin.mixins.worldgen.optimization;

import org.virgil.akiasync.mixin.util.PooledFeatureContext;
import org.virgil.akiasync.mixin.util.SimpleObjectPool;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Optional;

@Mixin(ConfiguredFeature.class)
public class ConfiguredFeaturePoolingMixin<FC extends FeatureConfiguration, F extends Feature<FC>> {

    @Shadow @Final public F feature;
    @Shadow @Final public FC config;

    @Overwrite
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        if (!level.ensureCanWrite(origin)) {
            return false;
        }

        final SimpleObjectPool<PooledFeatureContext<?>> pool = PooledFeatureContext.POOL.get();
        @SuppressWarnings("unchecked")
        final PooledFeatureContext<FC> context = (PooledFeatureContext<FC>) pool.alloc();
        try {
            context.reInit(Optional.empty(), level, generator, random, origin, this.config);
            return this.feature.place(context);
        } finally {
            pool.release(context);
        }
    }
}
