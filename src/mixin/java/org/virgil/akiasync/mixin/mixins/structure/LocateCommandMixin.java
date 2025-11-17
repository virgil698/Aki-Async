package org.virgil.akiasync.mixin.mixins.structure;

import java.util.concurrent.CompletableFuture;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.ResourceOrTagKeyArgument;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.commands.LocateCommand;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.structure.Structure;

@SuppressWarnings("unused")
@Mixin(LocateCommand.class)
public class LocateCommandMixin {
    
    @Unique private static volatile boolean cached_enabled;
    @Unique private static volatile int searchRadius;
    @Unique private static volatile boolean skipKnownStructures;
    @Unique private static volatile boolean initialized = false;
    
    @Inject(
        method = "locateStructure",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/chunk/ChunkGenerator;findNearestMapStructure(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/HolderSet;Lnet/minecraft/core/BlockPos;IZ)Lcom/mojang/datafixers/util/Pair;"
        ),
        cancellable = true,
        locals = LocalCapture.CAPTURE_FAILSOFT
    )
    private static void interceptLocateStructure(
        CommandSourceStack sourceStack,
        ResourceOrTagKeyArgument.Result<Structure> structureResult,
        CallbackInfoReturnable<Integer> cir,
        Registry<Structure> registry,
        HolderSet<Structure> holderSet
    ) {
        if (!initialized) { aki$initLocateCommand(); }
        if (!cached_enabled) return;
        
        CommandSource source = sourceStack.source;
        if (source instanceof ServerPlayer || source instanceof MinecraftServer) {
            org.virgil.akiasync.mixin.async.StructureLocatorBridge.locateStructureAsync(sourceStack, structureResult, holderSet);
            cir.setReturnValue(0);
        }
    }
    
    @Unique
    private static synchronized void aki$initLocateCommand() {
        if (initialized) return;
        org.virgil.akiasync.mixin.bridge.Bridge bridge = 
            org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
        if (bridge != null) {
            cached_enabled = bridge.isStructureLocationAsyncEnabled() && bridge.isLocateCommandEnabled();
            searchRadius = bridge.getLocateCommandSearchRadius();
            skipKnownStructures = bridge.isLocateCommandSkipKnownStructures();
        } else {
            cached_enabled = false;
            searchRadius = 0;
            skipKnownStructures = false;
        }
        initialized = true;
        if (bridge != null) {
            bridge.debugLog("[AkiAsync] LocateCommandMixin initialized: enabled=" + cached_enabled + ", radius=" + searchRadius + ", skipKnown=" + skipKnownStructures);
        }
    }
}
