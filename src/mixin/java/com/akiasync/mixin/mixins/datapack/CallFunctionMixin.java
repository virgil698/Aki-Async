package com.akiasync.mixin.mixins.datapack;

import com.akiasync.mixin.datapack.DataPackOptimizationMetrics;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.commands.execution.ExecutionContext;
import net.minecraft.commands.execution.Frame;
import net.minecraft.commands.execution.tasks.CallFunction;
import net.minecraft.commands.execution.tasks.ContinuationTask;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(CallFunction.class)
public abstract class CallFunctionMixin {
    @WrapOperation(
            method = "execute",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/commands/execution/tasks/ContinuationTask;schedule(Lnet/minecraft/commands/execution/ExecutionContext;Lnet/minecraft/commands/execution/Frame;Ljava/util/List;Lnet/minecraft/commands/execution/tasks/ContinuationTask$TaskProvider;)V"
            )
    )
    private static <T, P> void akiAsync$scheduleSmallFunction(
            ExecutionContext<T> context,
            Frame frame,
            List<P> arguments,
            ContinuationTask.TaskProvider<T, P> taskProvider,
            Operation<Void> original
    ) {
        int size = arguments.size();
        if (size >= 3 && size <= 8) {
            for (P argument : arguments) {
                context.queueNext(taskProvider.create(frame, argument));
            }
            DataPackOptimizationMetrics.smallFunctionScheduled(size);
            return;
        }
        original.call(context, frame, arguments, taskProvider);
    }
}
