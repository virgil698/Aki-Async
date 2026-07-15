package com.akiasync.mixin.mixins.datapack;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.commands.Commands;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Commands.class)
public abstract class CommandsMixin {
    @WrapOperation(
            method = "executeCommandInContext",
            at = @At(value = "INVOKE", target = "Ljava/lang/ThreadLocal;set(Ljava/lang/Object;)V")
    )
    private static void akiAsync$removeFinishedContext(
            ThreadLocal<Object> threadLocal,
            Object value,
            Operation<Void> original
    ) {
        if (value == null) {
            threadLocal.remove();
        } else {
            original.call(threadLocal, value);
        }
    }
}
