package org.virgil.akiasync.mixin.util;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

import java.util.Optional;

public class PooledFeatureContext<FC extends FeatureConfiguration> extends FeaturePlaceContext<FC> {

    public static final ThreadLocal<SimpleObjectPool<PooledFeatureContext<?>>> POOL =
        ThreadLocal.withInitial(() -> new SimpleObjectPool<>(
            unused -> new PooledFeatureContext<>(),
            PooledFeatureContext::reInit,
            2048
        ));

    private Optional<ConfiguredFeature<?, ?>> feature;
    private WorldGenLevel level;
    private ChunkGenerator generator;
    private RandomSource random;
    private BlockPos origin;
    private FC config;

    @SuppressWarnings("ConstantConditions")
    public PooledFeatureContext() {
        super(null, null, null, null, null, null);
    }

    public void reInit(Optional<ConfiguredFeature<?, ?>> feature, WorldGenLevel level,
                       ChunkGenerator generator, RandomSource random, BlockPos origin, FC config) {
        this.feature = feature;
        this.level = level;
        this.generator = generator;
        this.random = random;
        this.origin = origin;
        this.config = config;
    }

    public void reInit() {
        this.feature = null;
        this.level = null;
        this.generator = null;
        this.random = null;
        this.origin = null;
        this.config = null;
    }

    @Override
    public WorldGenLevel level() {
        return this.level;
    }

    @Override
    public ChunkGenerator chunkGenerator() {
        return this.generator;
    }

    @Override
    public RandomSource random() {
        return this.random;
    }

    @Override
    public BlockPos origin() {
        return this.origin;
    }

    @Override
    public FC config() {
        return this.config;
    }

    @Override
    public Optional<ConfiguredFeature<?, ?>> topFeature() {
        return this.feature;
    }
}
