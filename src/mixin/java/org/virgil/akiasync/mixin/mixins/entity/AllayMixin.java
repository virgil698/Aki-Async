package org.virgil.akiasync.mixin.mixins.entity;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Allay.class)
public abstract class AllayMixin {

    @Unique
    private static final Object akiasync$allayLock = new Object();

    @WrapMethod(method = "pickUpItem")
    private void akiasync$pickUpItemSynchronized(ServerLevel level, ItemEntity entity, Operation<Void> original) {
        synchronized (akiasync$allayLock) {
            if (!entity.isRemoved()) {
                original.call(level, entity);
            }
        }
    }
}
