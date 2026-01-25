package org.virgil.akiasync.mixin.mixins.entity.breed;

import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.animal.Fox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(Fox.FoxBreedGoal.class)
public abstract class FoxBreedConcurrentMixin extends BreedGoal {

    public FoxBreedConcurrentMixin(Fox fox, double speedModifier) {
        super(fox, speedModifier);
    }

    @Unique
    private static final ConcurrentHashMap<String, Boolean> aki$breedingPairs = new ConcurrentHashMap<>();

    @Inject(method = "start", at = @At("HEAD"))
    private void aki$resetBreedingFlag(CallbackInfo ci) {

        aki$breedingPairs.remove(aki$getPairKey());
    }

    @Inject(method = "breed", at = @At("HEAD"), cancellable = true)
    private void aki$preventDoubleBreed(CallbackInfo ci) {
        String pairKey = aki$getPairKey();

        if (aki$breedingPairs.putIfAbsent(pairKey, Boolean.TRUE) != null) {
            ci.cancel();
        }
    }

    @Unique
    private String aki$getPairKey() {
        UUID id1 = this.animal.getUUID();
        UUID id2 = null;
        if (this.partner != null) {
            id2 = this.partner.getUUID();
        }

        String s1 = id1.toString();
        String s2 = null;
        if (id2 != null) {
            s2 = id2.toString();
        }

        if (s2 != null) {
            return s1.compareTo(s2) <= 0 ? s1 + "|" + s2 : s2 + "|" + s1;
        }
        return s1;
    }
}
