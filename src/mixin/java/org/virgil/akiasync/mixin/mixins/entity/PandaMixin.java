package org.virgil.akiasync.mixin.mixins.entity;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.Panda;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Panda.class)
public class PandaMixin {

    @Unique
    private static final Object akiasync$pandaLock = new Object();

    @WrapMethod(method = "pickUpItem")
    private void akiasync$pickUpItemSynchronized(ServerLevel level, ItemEntity entity, Operation<Void> original) {
        synchronized (akiasync$pandaLock) {
            if (!entity.isRemoved()) {
                original.call(level, entity);
            }
        }
    }
}
