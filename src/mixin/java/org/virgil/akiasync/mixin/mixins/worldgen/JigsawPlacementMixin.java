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

    @Inject(method = "addPieces", at = @At("HEAD"))
    private static void akiasync$onAddPiecesStart(
            Structure.GenerationContext context,
            Holder<StructureTemplatePool> startPool,
            Optional<ResourceLocation> startJigsawName,
            int maxDepth,
            BlockPos pos,
            boolean useExpansionHack,
            Optional<Heightmap.Types> projectStartToHeightmap,
            int maxDistanceFromCenter,
            PoolAliasLookup aliasLookup,
            DimensionPadding dimensionPadding,
            LiquidSettings liquidSettings,
            CallbackInfoReturnable<Optional<Structure.GenerationStub>> cir) {

        if (!akiasync$isJigsawOptimizationEnabled()) {
            return;
        }

        try {
            org.virgil.akiasync.mixin.bridge.Bridge bridge = 
                org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
            
            if (bridge != null && !bridge.hasJigsawOctree()) {
                
                AABB bounds = new AABB(
                    pos.getX() - maxDistanceFromCenter,
                    context.chunkGenerator().getMinY(),
                    pos.getZ() - maxDistanceFromCenter,
                    pos.getX() + maxDistanceFromCenter,
                    context.chunkGenerator().getGenDepth(),
                    pos.getZ() + maxDistanceFromCenter
                );
                
                bridge.initializeJigsawOctree(bounds);
                
                akiasync$debugLog("Initialized octree for Jigsaw structure at " + pos + 
                                " with bounds " + bounds);
            }
        } catch (Exception e) {
            
        }
    }

    @Inject(method = "addPieces", at = @At("RETURN"))
    private static void akiasync$onAddPiecesEnd(
            Structure.GenerationContext context,
            Holder<StructureTemplatePool> startPool,
            Optional<ResourceLocation> startJigsawName,
            int maxDepth,
            BlockPos pos,
            boolean useExpansionHack,
            Optional<Heightmap.Types> projectStartToHeightmap,
            int maxDistanceFromCenter,
            PoolAliasLookup aliasLookup,
            DimensionPadding dimensionPadding,
            LiquidSettings liquidSettings,
            CallbackInfoReturnable<Optional<Structure.GenerationStub>> cir) {

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
