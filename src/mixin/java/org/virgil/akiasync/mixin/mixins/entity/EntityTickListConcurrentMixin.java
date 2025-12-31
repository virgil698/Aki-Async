package org.virgil.akiasync.mixin.mixins.entity;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityTickList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.concurrent.ConcurrentHashMap;


@Mixin(value = EntityTickList.class, priority = 900)
public class EntityTickListConcurrentMixin {

    @Unique
    private final Object akiasync$lock = new Object();

    @WrapMethod(method = "add")
    private void akiasync$addSynchronized(Entity entity, Operation<Void> original) {
        synchronized (akiasync$lock) {
            original.call(entity);
        }
    }

    @WrapMethod(method = "remove")
    private void akiasync$removeSynchronized(Entity entity, Operation<Void> original) {
        synchronized (akiasync$lock) {
            original.call(entity);
        }
    }
}
