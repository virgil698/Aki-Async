package org.virgil.akiasync.mixin.mixins.entity.breed;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.concurrent.atomic.AtomicBoolean;

@Mixin(Frog.class)
public abstract class FrogBreedConcurrentMixin extends Animal {

    @Unique
    private final AtomicBoolean aki$breedingFlag = new AtomicBoolean(false);

    protected FrogBreedConcurrentMixin(EntityType<? extends Animal> entityType, Level world) {
        super(entityType, world);
    }

    @WrapMethod(method = "spawnChildFromBreeding")
    private void aki$wrapSpawnChildFromBreeding(ServerLevel world, Animal other, Operation<Void> original) {

        if (this.getId() > other.getId()) {
            return;
        }

        FrogBreedConcurrentMixin otherMixin = (FrogBreedConcurrentMixin) other;

        if (this.aki$breedingFlag.compareAndSet(false, true) &&
            otherMixin.aki$breedingFlag.compareAndSet(false, true)) {
            try {
                original.call(world, other);
            } finally {
                this.aki$breedingFlag.set(false);
                otherMixin.aki$breedingFlag.set(false);
            }
        }
    }
}
