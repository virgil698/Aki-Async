package com.example.plugin.mixin.mixins;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.bukkit.craftbukkit.Main;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;
import java.util.List;

// TODO: remove this. this is only for example
@Mixin(Main.class)
public final class TemplateMixin {
    @Inject(method = "main", at = @At("HEAD"))
    private static void templateMixin(String[] args, CallbackInfo ci) {
        System.err.println("This is a template mixin. boot args: " + String.join(", ", args));
        System.err.println("PLEASE REMOVE THIS MIXIN AND REPLACE IT WITH YOUR OWN!");
    }

    @WrapMethod(method = "main")
    private static void wrapMain(String[] args, Operation<Void> original) {
        System.err.println("This is a template mixin. boot args: " + String.join(", ", args));
        System.err.println("PLEASE REMOVE THIS MIXIN AND REPLACE IT WITH YOUR OWN!");
        List<String> listArgs = Arrays.asList(args);
        System.err.println("Parse args with access widener: " + listArgs);
        original.call((Object) args);
    }
}
