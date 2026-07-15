package com.akiasync.mixin.mixins.datapack;

import com.akiasync.mixin.datapack.FunctionCompilationCache;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.ExecutionCommandSource;
import net.minecraft.commands.execution.UnboundEntryAction;
import net.minecraft.commands.functions.CommandFunction;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(CommandFunction.class)
public interface CommandFunctionMixin {
    @WrapMethod(method = "parseCommand")
    private static <T extends ExecutionCommandSource<T>> UnboundEntryAction<T> akiAsync$deduplicateCompilation(
            CommandDispatcher<T> dispatcher,
            T source,
            StringReader command,
            Operation<UnboundEntryAction<T>> original
    ) throws CommandSyntaxException {
        return FunctionCompilationCache.getOrCompile(
                dispatcher, source, command, () -> original.call(dispatcher, source, command)
        );
    }
}
