package org.virgil.akiasync.mixin.mixins.entity.parallel;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(targets = "net.minecraft.server.level.ServerLevel$EntityCallbacks")
public class ServerLevelEntityCallbacksMixin {

    @Unique
    private static final Object aki$lock = new Object();

    @WrapMethod(method = "onTickingStart(Lnet/minecraft/world/entity/Entity;)V")
    private void aki$syncOnTickingStart(Entity entity, Operation<Void> original) {
        synchronized (aki$lock) {
            original.call(entity);
        }
    }

    @WrapMethod(method = "onTickingEnd(Lnet/minecraft/world/entity/Entity;)V")
    private void aki$syncOnTickingEnd(Entity entity, Operation<Void> original) {
        synchronized (aki$lock) {
            original.call(entity);
        }
    }
}
