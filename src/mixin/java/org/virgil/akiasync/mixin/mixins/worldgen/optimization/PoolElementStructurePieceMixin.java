package org.virgil.akiasync.mixin.mixins.worldgen.optimization;

import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PoolElementStructurePiece.class)
public abstract class PoolElementStructurePieceMixin {

    @Inject(method = "<init>*", at = @At("RETURN"))
    private void akiasync$onConstructed(CallbackInfo ci) {
        if (!akiasync$isJigsawOptimizationEnabled()) {
            return;
        }

        try {
            org.virgil.akiasync.mixin.bridge.Bridge bridge =
                org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();

            if (bridge != null && bridge.hasJigsawOctree()) {

                PoolElementStructurePiece piece = (PoolElementStructurePiece) (Object) this;
                BoundingBox boundingBox = piece.getBoundingBox();
                if (boundingBox != null) {

                    AABB aabb = new AABB(
                        boundingBox.minX(),
                        boundingBox.minY(),
                        boundingBox.minZ(),
                        boundingBox.maxX() + 1,
                        boundingBox.maxY() + 1,
                        boundingBox.maxZ() + 1
                    );

                    bridge.insertIntoJigsawOctree(aabb);
                }
            }
        } catch (Exception e) {
            org.virgil.akiasync.mixin.util.ExceptionHandler.handleExpected(
                "PoolElementStructurePiece", "insertJigsawOctree", e);
        }
    }

    private boolean akiasync$isJigsawOptimizationEnabled() {
        try {
            org.virgil.akiasync.mixin.bridge.Bridge bridge =
                org.virgil.akiasync.mixin.bridge.BridgeManager.getBridge();
            return bridge != null && bridge.isJigsawOptimizationEnabled();
        } catch (Exception e) {
            return true;
        }
    }
}
