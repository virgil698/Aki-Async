package org.virgil.akiasync.mixin.mixins.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.pools.DimensionPadding;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasLookup;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.virgil.akiasync.mixin.util.BridgeConfigCache;

import java.util.Optional;

@Mixin(JigsawPlacement.class)
public class JigsawPlacementMixin {

    @Inject(method = "addPieces", at = @At("HEAD"), require = 0, remap = false)
    private static void akiasync$onAddPiecesStart(CallbackInfoReturnable<Optional<Structure.GenerationStub>> cir) {
        
    }

    @Inject(method = "addPieces", at = @At("RETURN"), require = 0, remap = false)
    private static void akiasync$onAddPiecesEnd(CallbackInfoReturnable<Optional<Structure.GenerationStub>> cir) {
        if (!akiasync$isJigsawOptimizationEnabled()) {
            return;
        }

        try {
            org.virgil.akiasync.mixin.bridge.Bridge bridge = 
                org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
            
            if (bridge != null && bridge.hasJigsawOctree()) {
                
                String stats = bridge.getJigsawOctreeStats();
                if (stats != null) {
                    akiasync$debugLog("Jigsaw structure generation complete. Octree stats: " + stats);
                }
                
                bridge.clearJigsawOctree();
            }
        } catch (Exception e) {
            
        }
    }

    private static boolean akiasync$isJigsawOptimizationEnabled() {
        try {
            org.virgil.akiasync.mixin.bridge.Bridge bridge = 
                org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
            return bridge != null && bridge.isJigsawOptimizationEnabled();
        } catch (Exception e) {
            return true; 
        }
    }

    private static void akiasync$debugLog(String message) {
        BridgeConfigCache.debugLog("[AkiAsync-Jigsaw] " + message);
    }
}
