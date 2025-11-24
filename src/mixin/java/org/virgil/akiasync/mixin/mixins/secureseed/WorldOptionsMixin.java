package org.virgil.akiasync.mixin.mixins.secureseed;

import com.google.gson.Gson;
import net.minecraft.world.level.levelgen.WorldOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.virgil.akiasync.mixin.secureseed.crypto.Globals;
import org.virgil.akiasync.mixin.secureseed.duck.IWorldOptionsFeatureSeed;

import java.util.OptionalLong;

@Mixin(WorldOptions.class)
public abstract class WorldOptionsMixin implements IWorldOptionsFeatureSeed {
    @Unique
    private static final Gson SECURE_SEED_GSON = new Gson();

    @Unique
    private long[] secureSeed$featureSeed = Globals.createRandomWorldSeed();

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(long seed, boolean generateStructures, boolean generateBonusChest, 
                       java.util.Optional<String> legacyCustomOptions, CallbackInfo ci) {
        this.secureSeed$featureSeed = Globals.createRandomWorldSeed();
    }

    @Inject(method = "defaultWithRandomSeed", at = @At("RETURN"))
    private static void setupDefaultSeed(CallbackInfoReturnable<WorldOptions> cir) {
        var worldOptions = cir.getReturnValue();
        ((IWorldOptionsFeatureSeed) worldOptions).secureSeed$setFeatureSeed(Globals.createRandomWorldSeed());
    }

    @Inject(method = "withBonusChest", at = @At("RETURN"))
    private void setupBonusChest(boolean bl, CallbackInfoReturnable<WorldOptions> cir) {
        var worldOptions2 = cir.getReturnValue();
        ((IWorldOptionsFeatureSeed) worldOptions2).secureSeed$setFeatureSeed(this.secureSeed$featureSeed);
    }

    @Inject(method = "withStructures", at = @At("RETURN"))
    private void setupStructures(boolean bl, CallbackInfoReturnable<WorldOptions> cir) {
        var worldOptions2 = cir.getReturnValue();
        ((IWorldOptionsFeatureSeed) worldOptions2).secureSeed$setFeatureSeed(this.secureSeed$featureSeed);
    }

    @Inject(method = "withSeed", at = @At("RETURN"))
    private void setupSeed(OptionalLong optionalLong, CallbackInfoReturnable<WorldOptions> cir) {
        var worldOptions2 = cir.getReturnValue();
        ((IWorldOptionsFeatureSeed) worldOptions2).secureSeed$setFeatureSeed(Globals.createRandomWorldSeed());
    }

    @Unique
    @Override
    public long[] secureSeed$featureSeed() {
        return this.secureSeed$featureSeed;
    }

    @Unique
    @Override
    public void secureSeed$setFeatureSeed(long[] seed) {
        if (seed != null && seed.length == Globals.WORLD_SEED_LONGS) {
            this.secureSeed$featureSeed = seed;
        }
    }

    @Unique
    @Override
    public String secureSeed$featureSeedSerialize() {
        return SECURE_SEED_GSON.toJson(this.secureSeed$featureSeed);
    }
}
