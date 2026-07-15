package com.akiasync.mixin.mixins.datapack;

import com.akiasync.mixin.datapack.DataPackOptimizationMetrics;
import com.akiasync.mixin.datapack.DataPackOptimizationMetrics.ReloadToken;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.commands.Commands;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.Registry;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.world.flag.FeatureFlagSet;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mixin(ReloadableServerResources.class)
public abstract class ReloadableServerResourcesMixin {
    @WrapMethod(method = "loadResources")
    private static CompletableFuture<ReloadableServerResources> akiAsync$measureReload(
            ResourceManager resourceManager,
            LayeredRegistryAccess<RegistryLayer> registryAccess,
            List<Registry.PendingTags<?>> postponedTags,
            FeatureFlagSet enabledFeatures,
            Commands.CommandSelection commandSelection,
            PermissionSet permissions,
            Executor backgroundExecutor,
            Executor gameExecutor,
            Operation<CompletableFuture<ReloadableServerResources>> original
    ) {
        ReloadToken token = DataPackOptimizationMetrics.beginReload();
        try {
            CompletableFuture<ReloadableServerResources> reload = original.call(
                    resourceManager,
                    registryAccess,
                    postponedTags,
                    enabledFeatures,
                    commandSelection,
                    permissions,
                    backgroundExecutor,
                    gameExecutor
            );
            return reload.whenComplete((resources, failure) -> DataPackOptimizationMetrics.completeReload(
                    token,
                    failure,
                    resources == null ? 0 : resources.getFunctionLibrary().getFunctions().size()
            ));
        } catch (RuntimeException | Error failure) {
            DataPackOptimizationMetrics.completeReload(token, failure, 0);
            throw failure;
        }
    }
}
