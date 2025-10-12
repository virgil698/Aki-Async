package com.example.plugin.mixin.mixins;

import org.bukkit.craftbukkit.Main;
import org.leavesmc.plugin.mixin.condition.annotations.ServerBuild;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// TODO: remove this. this is only for example
@Mixin(Main.class)
@ServerBuild(minecraft = "1.21.5", build = ">=114514")
public final class TemplateConditionalMixin {
    @Inject(method = "main", at = @At("HEAD"))
    private static void templateMixin(String[] args, CallbackInfo ci) {
        System.err.println("This is a template mixin. boot args: " + String.join(", ", args));
        System.err.println("THIS SHOULD NEVER CALLED!!!");
    }
}
